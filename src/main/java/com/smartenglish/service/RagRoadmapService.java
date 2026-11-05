package com.smartenglish.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartenglish.config.AiProperties;
import com.smartenglish.dto.request.GenerateRoadmapRequest;
import com.smartenglish.dto.response.AiRoadmapGenerationResult;
import com.smartenglish.dto.response.RoadmapResponse;
import com.smartenglish.dto.response.TaskCompletionResponse;
import com.smartenglish.entity.*;
import com.smartenglish.exception.AppException;
import com.smartenglish.exception.ResourceNotFoundException;
import com.smartenglish.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagRoadmapService {

    private final UserRepository userRepository;
    private final UserTestAttemptRepository attemptRepository;
    private final LessonRepository lessonRepository;
    private final AiRoadmapRepository roadmapRepository;
    private final AiRoadmapDayRepository roadmapDayRepository;
    private final AiRoadmapTaskRepository roadmapTaskRepository;
    private final LlmClientService llmClientService;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    /**
     * Quy trình RAG + LLM hoàn chỉnh:
     * 1. Retrieval: Tìm kiếm bài học của giáo viên theo điểm yếu subSkillTag &
     * targetLevel.
     * 2. Prompting & Generation: Tạo System Prompt kèm Context từ DB, gọi LLM sinh
     * lộ trình 7 ngày.
     * 3. Persistence: Parse JSON bằng Jackson và lưu vào 3 bảng ai_roadmaps,
     * ai_roadmap_days, ai_roadmap_tasks.
     * 4. Cập nhật users.isPlacementCompleted = true.
     */
    @Transactional
    public RoadmapResponse generateAndSaveRoadmap(GenerateRoadmapRequest request, Long currentAuthUserId) {
        Long targetUserId = request.getUserId() != null ? request.getUserId() : currentAuthUserId;
        if (targetUserId == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "User ID must be provided or obtained from JWT session");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", targetUserId));

        UserTestAttempt attempt = null;
        if (request.getAttemptId() != null) {
            attempt = attemptRepository.findById(request.getAttemptId()).orElse(null);
        } else {
            attempt = attemptRepository.findFirstByUser_UserIdOrderByStartedAtDesc(targetUserId).orElse(null);
        }

        String evaluatedLevel = StringUtils.hasText(request.getEvaluatedLevel())
                ? request.getEvaluatedLevel().toUpperCase()
                : (attempt != null && attempt.getEvaluatedLevel() != null
                        && !"PENDING".equalsIgnoreCase(attempt.getEvaluatedLevel())
                                ? attempt.getEvaluatedLevel()
                                : "B1");

        String weaknessInput = StringUtils.hasText(request.getWeaknessAnalysisJson())
                ? request.getWeaknessAnalysisJson()
                : (attempt != null ? attempt.getWeaknessAnalysisJson() : "[\"LISTENING_DETAIL\", \"READING_VOCAB\"]");

        // --- BƯỚC 1: RETRIEVAL (RAG) ---
        Set<String> subSkillTags = extractSubSkillTags(weaknessInput);
        log.info("RAG Step 1 - Extracted SubSkills for User {}: {}", targetUserId, subSkillTags);

        List<Lesson> relevantLessons = new ArrayList<>();
        if (!subSkillTags.isEmpty()) {
            relevantLessons = lessonRepository.findRelevantLessonsForRag(subSkillTags, evaluatedLevel);
            if (relevantLessons.isEmpty()) {
                relevantLessons = lessonRepository.findBySubSkillTagInAndIsPublishedTrue(subSkillTags);
            }
        }
        if (relevantLessons.isEmpty()) {
            // Lấy một số bài giảng công khai mặc định nếu chưa khớp chính xác
            relevantLessons = lessonRepository.findAll().stream().limit(5).toList();
        }

        String lessonsContext = buildLessonCatalogPrompt(relevantLessons);

        // --- BƯỚC 2: PROMPTING & GENERATION ---
        String systemPrompt = buildSystemPrompt(lessonsContext);
        String userPrompt = buildUserPrompt(user.getFullName(), evaluatedLevel, weaknessInput);

        log.info("RAG Step 2 - Invoking LLM for User {}", targetUserId);
        String llmOutputJson = llmClientService.generateChatCompletion(systemPrompt, userPrompt);

        // --- BƯỚC 3: PARSING JSON (JACKSON) ---
        AiRoadmapGenerationResult parsedResult = parseLlmResponse(llmOutputJson);

        // --- BƯỚC 4: PERSISTENCE (LƯU DB TRONG TRANSACTION) ---
        AiRoadmap roadmap = AiRoadmap.builder()
                .user(user)
                .sourceAttempt(attempt)
                .roadmapTitle(parsedResult.getRoadmapTitle())
                .targetWeaknesses(parsedResult.getTargetWeaknesses())
                .aiModelVersion(aiProperties.getModel())
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(7))
                .status("ACTIVE")
                .build();

        AiRoadmap savedRoadmap = roadmapRepository.save(roadmap);

        List<AiRoadmapDay> savedDays = new ArrayList<>();

        if (parsedResult.getDays() != null) {
            for (AiRoadmapGenerationResult.AiDayResult dayResult : parsedResult.getDays()) {
                int dayNum = dayResult.getDayNumber() != null ? dayResult.getDayNumber() : 1;
                String normalizedFocus = normalizeFocusSkill(dayResult.getFocusSkill(), dayNum);

                AiRoadmapDay day = AiRoadmapDay.builder()
                        .roadmap(savedRoadmap)
                        .dayNumber(dayNum)
                        .dailyTopic(dayResult.getDailyTopic() != null ? dayResult.getDailyTopic() : "Học tập tổng hợp")
                        .focusSkill(normalizedFocus)
                        .targetSubSkill(dayResult.getTargetSubSkill())
                        .aiDailyAdvice(dayResult.getAiDailyAdvice())
                        .isUnlocked(dayNum == 1)
                        .isCompleted(false)
                        .build();

                AiRoadmapDay savedDay = roadmapDayRepository.save(day);

                if (dayResult.getTasks() != null) {
                    for (AiRoadmapGenerationResult.AiTaskResult taskResult : dayResult.getTasks()) {
                        String contentJsonString = convertContentToJsonString(taskResult.getAiGeneratedContentJson());
                        String normalizedTask = normalizeTaskType(taskResult.getTaskType(),
                                taskResult.getReferenceLessonId());

                        AiRoadmapTask task = AiRoadmapTask.builder()
                                .roadmapDay(savedDay)
                                .taskTitle(taskResult.getTaskTitle() != null ? taskResult.getTaskTitle()
                                        : "Bài tập hàng ngày")
                                .taskType(normalizedTask)
                                .referenceLessonId(taskResult.getReferenceLessonId())
                                .aiGeneratedContentJson(contentJsonString)
                                .estimatedMinutes(
                                        taskResult.getEstimatedMinutes() != null ? taskResult.getEstimatedMinutes()
                                                : 15)
                                .orderIndex(taskResult.getOrderIndex() != null ? taskResult.getOrderIndex() : 1)
                                .isCompleted(false)
                                .build();

                        roadmapTaskRepository.save(task);
                        savedDay.getTasks().add(task);
                    }
                }
                savedDays.add(savedDay);
            }
        }

        savedRoadmap.setDays(savedDays);

        // Cập nhật trạng thái người dùng
        user.setIsPlacementCompleted(true);
        user.setCurrentLevel(evaluatedLevel);
        userRepository.save(user);

        log.info("RAG Step 4 - Successfully generated and persisted 7-day roadmap ID {} for User {}",
                savedRoadmap.getRoadmapId(), user.getUserId());

        return mapToRoadmapResponse(savedRoadmap);
    }

    @Transactional(readOnly = true)
    public RoadmapResponse getCurrentActiveRoadmap(Long userId) {
        AiRoadmap roadmap = roadmapRepository.findFirstByUser_UserIdAndStatusOrderByCreatedAtDesc(userId, "ACTIVE")
                .orElseThrow(() -> new ResourceNotFoundException("Active Roadmap", "userId", userId));

        return mapToRoadmapResponse(roadmap);
    }

    @Transactional(readOnly = true)
    public RoadmapResponse getRoadmapById(Long roadmapId) {
        AiRoadmap roadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new ResourceNotFoundException("Roadmap", "roadmapId", roadmapId));

        return mapToRoadmapResponse(roadmap);
    }

    @Transactional
    public TaskCompletionResponse completeTask(Long taskId, Double userScore, Long userId) {
        AiRoadmapTask task = roadmapTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("AiRoadmapTask", "taskId", taskId));

        AiRoadmapDay day = task.getRoadmapDay();
        AiRoadmap roadmap = day.getRoadmap();

        if (roadmap.getUser() != null && !roadmap.getUser().getUserId().equals(userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền cập nhật nhiệm vụ này");
        }

        task.setIsCompleted(true);
        task.setUserScore(userScore != null ? userScore : 100.0);
        roadmapTaskRepository.save(task);

        // Kiểm tra xem tất cả các tasks trong ngày đã hoàn thành chưa
        List<AiRoadmapTask> tasksInDay = roadmapTaskRepository
                .findByRoadmapDay_RoadmapDayIdOrderByOrderIndexAsc(day.getRoadmapDayId());
        boolean allTasksCompleted = tasksInDay.stream().allMatch(t -> Boolean.TRUE.equals(t.getIsCompleted()));

        boolean nextDayUnlocked = false;
        if (allTasksCompleted) {
            day.setIsCompleted(true);
            day.setCompletedAt(java.time.LocalDateTime.now());
            roadmapDayRepository.save(day);

            // Tự động mở khóa ngày tiếp theo
            int nextDayNumber = day.getDayNumber() + 1;
            List<AiRoadmapDay> allDays = roadmapDayRepository
                    .findByRoadmap_RoadmapIdOrderByDayNumberAsc(roadmap.getRoadmapId());
            for (AiRoadmapDay d : allDays) {
                if (d.getDayNumber() == nextDayNumber && !Boolean.TRUE.equals(d.getIsUnlocked())) {
                    d.setIsUnlocked(true);
                    roadmapDayRepository.save(d);
                    nextDayUnlocked = true;
                    break;
                }
            }
        }

        String msg = allTasksCompleted
                ? String.format("Chúc mừng! Bạn đã hoàn thành toàn bộ nhiệm vụ của Ngày %d! %s",
                        day.getDayNumber(), nextDayUnlocked ? "Ngày tiếp theo đã được mở khóa." : "")
                : "Hoàn thành nhiệm vụ thành công!";

        return TaskCompletionResponse.builder()
                .taskId(taskId)
                .isCompleted(true)
                .userScore(task.getUserScore())
                .dayNumber(day.getDayNumber())
                .dayIsCompleted(day.getIsCompleted())
                .nextDayUnlocked(nextDayUnlocked)
                .message(msg)
                .build();
    }

    private Set<String> extractSubSkillTags(String jsonOrTags) {
        Set<String> tags = new HashSet<>();
        try {
            if (jsonOrTags.trim().startsWith("[")) {
                List<String> list = objectMapper.readValue(jsonOrTags, new TypeReference<List<String>>() {
                });
                tags.addAll(list);
            } else if (jsonOrTags.trim().startsWith("{")) {
                JsonNode root = objectMapper.readTree(jsonOrTags);
                if (root.has("weaknesses") && root.get("weaknesses").isArray()) {
                    for (JsonNode node : root.get("weaknesses")) {
                        tags.add(node.asText());
                    }
                }
            } else {
                for (String part : jsonOrTags.split("[,;]")) {
                    if (StringUtils.hasText(part)) {
                        tags.add(part.trim().toUpperCase());
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Could not parse weaknessAnalysisJson as JSON array, splitting by delimiter: {}", ex.getMessage());
            for (String part : jsonOrTags.split("[,;]")) {
                if (StringUtils.hasText(part)) {
                    tags.add(part.trim().toUpperCase());
                }
            }
        }
        return tags;
    }

    private String buildLessonCatalogPrompt(List<Lesson> lessons) {
        if (lessons == null || lessons.isEmpty()) {
            return "No pre-existing admin lessons available. You can recommend AI-generated tasks directly.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("DANH MỤC BÀI GIẢNG CỦA ADMIN (Có thể tham chiếu làm referenceLessonId trong Task):\n");
        for (Lesson l : lessons) {
            sb.append(String.format(
                    "- ID %d: \"%s\" | Kỹ năng: %s | SubSkill: %s | Thời lượng: %d phút | Khóa học: %s (Level %s)\n",
                    l.getLessonId(),
                    l.getTitle(),
                    l.getSkillType(),
                    l.getSubSkillTag(),
                    l.getEstimatedMinutes(),
                    l.getCourse() != null ? l.getCourse().getTitle() : "N/A",
                    l.getCourse() != null ? l.getCourse().getTargetLevel() : "N/A"));
        }
        return sb.toString();
    }

    private String buildSystemPrompt(String lessonCatalog) {
        return """
                Bạn là Chuyên gia Khảo thí và Thiết kế Lộ trình Học tiếng Anh Cá nhân hóa AI (Senior Pedagogical Curriculum Architect).
                Nhiệm vụ của bạn là phân tích điểm yếu học viên và sinh ra một Lộ trình 7 ngày học tập chi tiết (Day 1 đến Day 7).

                QUY ĐỊNH RÀNG BUỘC CỐT LÕI (BẮT BUỘC TUÂN THỦ 100%):
                1. Trường "focusSkill":
                   - CHỈ ĐƯỢC PHÉP nhận đúng 1 trong 3 chuỗi ký tự viết hoa sau: "LISTENING", "READING", hoặc "MIXED_REVIEW".
                   - TUYỆT ĐỐI KHÔNG dùng dấu gạch đứng (|), không viết thường, và không tự bịa các giá trị khác ngoài 3 giá trị trên. Nếu ngày học không thuần túy chỉ có Listening hoặc Reading, hãy dùng "MIXED_REVIEW".
                2. Trường "taskType":
                   - CHỈ ĐƯỢC PHÉP nhận 1 trong 2 giá trị viết hoa: "LESSON" hoặc "PRACTICE".
                   - Chọn "LESSON": Nếu là bài giảng có sẵn của giáo viên (kèm "referenceLessonId" tương ứng).
                   - Chọn "PRACTICE": Nếu là bài tập trắc nghiệm / phản xạ / flashcard do AI sinh tự động ("referenceLessonId" phải để là null).

                YÊU CẦU ĐẦU RA JSON BẮT BUỘC (Không kèm markdown code fence như ```json, chỉ trả về chuỗi JSON thuần hợp lệ):
                {
                  "roadmapTitle": "Tên lộ trình súc tích, hấp dẫn",
                  "aiOverallFeedback": "Nhận xét chuyên sâu điểm mạnh, điểm yếu dựa trên bài test",
                  "targetWeaknesses": "Các subSkillTag cần khắc phục",
                  "days": [
                    {
                      "dayNumber": 1,
                      "dailyTopic": "Chủ đề học trong ngày",
                      "focusSkill": "LISTENING",
                      "targetSubSkill": "LISTENING_DETAIL",
                      "aiDailyAdvice": "Lời khuyên sư phạm chiến thuật",
                      "tasks": [
                        {
                          "taskTitle": "Tên nhiệm vụ cụ thể",
                          "taskType": "LESSON",
                          "referenceLessonId": 1,
                          "estimatedMinutes": 15,
                          "orderIndex": 1,
                          "aiGeneratedContentJson": {}
                        }
                      ]
                    }
                  ]
                }

                DƯỚI ĐÂY LÀ KHO DỮ LIỆU BÀI GIẢNG HIỆN CÓ CỦA HỆ THỐNG:
                """
                + lessonCatalog;
    }

    private String buildUserPrompt(String studentName, String evaluatedLevel, String weaknessAnalysisJson) {
        return String.format(
                """
                        HỌC VIÊN: %s
                        TRÌNH ĐỘ ĐÁNH GIÁ (CEFR): %s
                        CHI TIẾT ĐIỂM YẾU HỌC VIÊN:
                        %s

                        Hãy tạo lộ trình 7 ngày giúp học viên vượt qua các điểm yếu trên, kết hợp hợp lý giữa bài giảng của giáo viên và bài tập luyện phản xạ do AI đề xuất!
                        """,
                studentName, evaluatedLevel, weaknessAnalysisJson);
    }

    private AiRoadmapGenerationResult parseLlmResponse(String rawJson) {
        try {
            String clean = rawJson.trim();
            if (clean.startsWith("```json")) {
                clean = clean.substring(7);
            } else if (clean.startsWith("```")) {
                clean = clean.substring(3);
            }
            if (clean.endsWith("```")) {
                clean = clean.substring(0, clean.length() - 3);
            }
            clean = clean.trim();

            return objectMapper.readValue(clean, AiRoadmapGenerationResult.class);
        } catch (Exception ex) {
            log.error("Failed to parse LLM Response JSON: {}", rawJson, ex);
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to parse AI Roadmap JSON: " + ex.getMessage());
        }
    }

    private String convertContentToJsonString(Object contentObj) {
        if (contentObj == null) {
            return "{}";
        }
        if (contentObj instanceof String str) {
            return str;
        }
        try {
            return objectMapper.writeValueAsString(contentObj);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private RoadmapResponse mapToRoadmapResponse(AiRoadmap roadmap) {
        List<RoadmapResponse.RoadmapDayResponse> dayResponses = new ArrayList<>();

        if (roadmap.getDays() != null) {
            for (AiRoadmapDay day : roadmap.getDays()) {
                List<RoadmapResponse.RoadmapTaskResponse> taskResponses = new ArrayList<>();
                if (day.getTasks() != null) {
                    for (AiRoadmapTask task : day.getTasks()) {
                        taskResponses.add(RoadmapResponse.RoadmapTaskResponse.builder()
                                .taskId(task.getTaskId())
                                .taskTitle(task.getTaskTitle())
                                .taskType(task.getTaskType())
                                .referenceLessonId(task.getReferenceLessonId())
                                .aiGeneratedContentJson(task.getAiGeneratedContentJson())
                                .estimatedMinutes(task.getEstimatedMinutes())
                                .orderIndex(task.getOrderIndex())
                                .userScore(task.getUserScore())
                                .isCompleted(task.getIsCompleted())
                                .build());
                    }
                }

                dayResponses.add(RoadmapResponse.RoadmapDayResponse.builder()
                        .roadmapDayId(day.getRoadmapDayId())
                        .dayNumber(day.getDayNumber())
                        .dailyTopic(day.getDailyTopic())
                        .focusSkill(day.getFocusSkill())
                        .targetSubSkill(day.getTargetSubSkill())
                        .aiDailyAdvice(day.getAiDailyAdvice())
                        .isUnlocked(day.getIsUnlocked())
                        .isCompleted(day.getIsCompleted())
                        .completedAt(day.getCompletedAt())
                        .tasks(taskResponses)
                        .build());
            }
        }

        return RoadmapResponse.builder()
                .roadmapId(roadmap.getRoadmapId())
                .userId(roadmap.getUser() != null ? roadmap.getUser().getUserId() : null)
                .sourceAttemptId(roadmap.getSourceAttempt() != null ? roadmap.getSourceAttempt().getAttemptId() : null)
                .roadmapTitle(roadmap.getRoadmapTitle())
                .targetWeaknesses(roadmap.getTargetWeaknesses())
                .aiModelVersion(roadmap.getAiModelVersion())
                .startDate(roadmap.getStartDate())
                .endDate(roadmap.getEndDate())
                .status(roadmap.getStatus())
                .createdAt(roadmap.getCreatedAt())
                .days(dayResponses)
                .build();
    }

    /**
     * Chuẩn hóa focusSkill đảm bảo luôn thỏa mãn ràng buộc SQL Server:
     * CK__ai_roadma__focus__05D8E0BE: ([focusSkill]='MIXED_REVIEW' OR
     * [focusSkill]='READING' OR [focusSkill]='LISTENING')
     */
    private String normalizeFocusSkill(String rawFocus, int dayNumber) {
        if (rawFocus == null || rawFocus.isBlank()) {
            return (dayNumber % 2 == 1) ? "LISTENING" : "READING";
        }
        String upper = rawFocus.trim().toUpperCase();
        if (upper.contains("LISTEN")) {
            return "LISTENING";
        }
        if (upper.contains("READ")) {
            return "READING";
        }
        // Các trường hợp VOCABULARY, GRAMMAR, EVALUATION, MIXED, REVIEW...
        return "MIXED_REVIEW";
    }

    /**
     * Chuẩn hóa taskType đảm bảo luôn thỏa mãn ràng buộc SQL Server:
     * Task tham chiếu bài học -> "LESSON", bài tập phản xạ/thực hành AI ->
     * "PRACTICE"
     */
    private String normalizeTaskType(String rawTaskType, Long referenceLessonId) {
        if (referenceLessonId != null) {
            return "LESSON";
        }
        if (rawTaskType == null || rawTaskType.isBlank()) {
            return "PRACTICE";
        }
        String upper = rawTaskType.trim().toUpperCase();
        if (upper.contains("LESSON") || upper.contains("THEORY")) {
            return "LESSON";
        }
        return "PRACTICE";
    }
}
