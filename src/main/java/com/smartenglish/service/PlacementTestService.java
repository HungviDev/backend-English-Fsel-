package com.smartenglish.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartenglish.dto.request.*;
import com.smartenglish.dto.response.*;
import com.smartenglish.entity.*;
import com.smartenglish.exception.AppException;
import com.smartenglish.exception.ResourceNotFoundException;
import com.smartenglish.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlacementTestService {

    private final PlacementTestRepository placementTestRepository;
    private final QuestionPassageRepository passageRepository;
    private final PlacementQuestionRepository questionRepository;
    private final UserTestAttemptRepository attemptRepository;
    private final UserTestAnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final RagRoadmapService ragRoadmapService;
    private final ObjectMapper objectMapper;

    // ==========================================
    // GIAI ĐOẠN 1: DÀNH CHO ADMIN
    // ==========================================

    @Transactional
    public PlacementTest createPlacementTest(CreatePlacementTestRequest request, Long adminId) {
        User admin = adminId != null ? userRepository.findById(adminId).orElse(null) : null;

        PlacementTest test = PlacementTest.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .durationMinutes(request.getDurationMinutes())
                .totalQuestions(request.getTotalQuestions())
                .isActive(request.getIsActive())
                .createdBy(admin)
                .build();

        return placementTestRepository.save(test);
    }

    @Transactional(readOnly = true)
    public List<PlacementTest> getAllTests() {
        return placementTestRepository.findAll();
    }

    @Transactional(readOnly = true)
    public PageResponse<PlacementTestResponse> getAllTestsPaged(
            String search,
            Boolean isActive,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String safeSortBy = (sortBy != null && !sortBy.trim().isEmpty()) ? sortBy.trim() : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(direction, safeSortBy));

        Page<PlacementTest> testPage = placementTestRepository.searchTestsPaged(cleanSearch, isActive, pageable);
        List<PlacementTestResponse> dtos = testPage.getContent().stream()
                .map(this::mapToTestResponse)
                .collect(Collectors.toList());

        return PageResponse.of(testPage, dtos);
    }

    @Transactional(readOnly = true)
    public PlacementTest getTestDetail(Long testId) {
        return placementTestRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("PlacementTest", "testId", testId));
    }

    @Transactional(readOnly = true)
    public PlacementTestResponse getTestDetailResponse(Long testId) {
        PlacementTest test = getTestDetail(testId);
        return mapToTestResponse(test);
    }

    public PlacementTestResponse mapToTestResponse(PlacementTest test) {
        List<QuestionPassage> passages = passageRepository.findByTest_TestIdOrderByOrderIndexAsc(test.getTestId());
        int questionCount = 0;
        for (QuestionPassage p : passages) {
            questionCount += questionRepository.findByPassage_PassageId(p.getPassageId()).size();
        }

        Long adminId = null;
        String adminName = null;
        String adminEmail = null;
        if (test.getCreatedBy() != null) {
            try {
                adminId = test.getCreatedBy().getUserId();
                adminName = test.getCreatedBy().getFullName();
                adminEmail = test.getCreatedBy().getEmail();
            } catch (Exception ignored) {
            }
        }

        return PlacementTestResponse.builder()
                .testId(test.getTestId())
                .title(test.getTitle())
                .description(test.getDescription())
                .durationMinutes(test.getDurationMinutes())
                .totalQuestions(test.getTotalQuestions())
                .isActive(test.getIsActive())
                .createdById(adminId)
                .createdByName(adminName)
                .createdByEmail(adminEmail)
                .passageCount(passages.size())
                .questionCount(questionCount)
                .createdAt(test.getCreatedAt())
                .build();
    }

    @Transactional
    public QuestionPassage addPassage(Long testId, CreateQuestionPassageRequest request) {
        PlacementTest test = getTestDetail(testId);

        QuestionPassage passage = QuestionPassage.builder()
                .test(test)
                .skillType(request.getSkillType().toUpperCase())
                .title(request.getTitle())
                .passageText(request.getPassageText())
                .audioUrl(request.getAudioUrl())
                .orderIndex(request.getOrderIndex())
                .build();

        return passageRepository.save(passage);
    }

    @Transactional
    public PlacementQuestion addQuestionToPassage(Long passageId, CreatePlacementQuestionRequest request) {
        QuestionPassage passage = passageRepository.findById(passageId)
                .orElseThrow(() -> new ResourceNotFoundException("QuestionPassage", "passageId", passageId));

        PlacementQuestion question = PlacementQuestion.builder()
                .passage(passage)
                .subSkillTag(request.getSubSkillTag().toUpperCase())
                .difficultyLevel(request.getDifficultyLevel() != null ? request.getDifficultyLevel().toUpperCase() : "B1")
                .questionText(request.getQuestionText())
                .optionA(request.getOptionA())
                .optionB(request.getOptionB())
                .optionC(request.getOptionC())
                .optionD(request.getOptionD())
                .correctOption(request.getCorrectOption().toUpperCase().trim())
                .explanation(request.getExplanation())
                .build();

        return questionRepository.save(question);
    }

    @Transactional(readOnly = true)
    public List<QuestionPassage> getPassagesByTest(Long testId) {
        return passageRepository.findByTest_TestIdOrderByOrderIndexAsc(testId);
    }

    @Transactional(readOnly = true)
    public List<PlacementQuestion> getQuestionsByPassage(Long passageId) {
        return questionRepository.findByPassage_PassageId(passageId);
    }

    // ==========================================
    // GIAI ĐOẠN 2 & 3: HỌC VIÊN LÀM BÀI TEST
    // ==========================================

    @Transactional(readOnly = true)
    public boolean checkPlacementCompleted(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
        return Boolean.TRUE.equals(user.getIsPlacementCompleted());
    }

    @Transactional(readOnly = true)
    public PlacementTestStudentDto getActivePlacementTestForStudent() {
        PlacementTest test = placementTestRepository.findFirstByIsActiveTrue()
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Hiện tại không có đề thi đầu vào nào đang kích hoạt"));

        List<QuestionPassage> passages = passageRepository.findByTest_TestIdOrderByOrderIndexAsc(test.getTestId());

        List<QuestionPassageStudentDto> passageDtos = passages.stream().map(p -> {
            List<PlacementQuestion> questions = questionRepository.findByPassage_PassageId(p.getPassageId());
            List<PlacementQuestionStudentDto> questionDtos = questions.stream().map(q ->
                    PlacementQuestionStudentDto.builder()
                            .questionId(q.getQuestionId())
                            .subSkillTag(q.getSubSkillTag())
                            .difficultyLevel(q.getDifficultyLevel())
                            .questionText(q.getQuestionText())
                            .optionA(q.getOptionA())
                            .optionB(q.getOptionB())
                            .optionC(q.getOptionC())
                            .optionD(q.getOptionD())
                            .build()
            ).collect(Collectors.toList());

            return QuestionPassageStudentDto.builder()
                    .passageId(p.getPassageId())
                    .skillType(p.getSkillType())
                    .title(p.getTitle())
                    .passageText(p.getPassageText())
                    .audioUrl(p.getAudioUrl())
                    .orderIndex(p.getOrderIndex())
                    .questions(questionDtos)
                    .build();
        }).collect(Collectors.toList());

        return PlacementTestStudentDto.builder()
                .testId(test.getTestId())
                .title(test.getTitle())
                .description(test.getDescription())
                .durationMinutes(test.getDurationMinutes())
                .totalQuestions(test.getTotalQuestions())
                .passages(passageDtos)
                .build();
    }

    @Transactional
    public TestAttemptResponse startAttempt(Long testId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        PlacementTest test = placementTestRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("PlacementTest", "testId", testId));

        UserTestAttempt attempt = UserTestAttempt.builder()
                .user(user)
                .test(test)
                .listeningScore(0.0)
                .readingScore(0.0)
                .totalScore(0.0)
                .evaluatedLevel("PENDING")
                .weaknessAnalysisJson("[]")
                .aiOverallFeedback("")
                .startedAt(LocalDateTime.now())
                .build();

        UserTestAttempt saved = attemptRepository.save(attempt);

        return TestAttemptResponse.builder()
                .attemptId(saved.getAttemptId())
                .testId(test.getTestId())
                .testTitle(test.getTitle())
                .durationMinutes(test.getDurationMinutes())
                .startedAt(saved.getStartedAt())
                .status("IN_PROGRESS")
                .build();
    }

    @Transactional
    public void recordAnswer(Long attemptId, SubmitAnswerRequest request, Long userId) {
        UserTestAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("UserTestAttempt", "attemptId", attemptId));

        if (!attempt.getUser().getUserId().equals(userId)) {
            throw new AppException(HttpStatus.FORBIDDEN,
                    String.format("Bạn không có quyền thao tác trên lượt thi này (Lượt thi #%d thuộc về userId %d, bạn đang đăng nhập userId %d)",
                            attemptId, attempt.getUser().getUserId(), userId));
        }
        if (attempt.getCompletedAt() != null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Lượt thi này đã nộp bài, không thể sửa câu trả lời");
        }

        PlacementQuestion question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("PlacementQuestion", "questionId", request.getQuestionId()));

        String selectedOpt = normalizeOption(request.getSelectedOption());
        String correctOpt = normalizeOption(question.getCorrectOption());
        boolean isCorrect = selectedOpt.equalsIgnoreCase(correctOpt);

        UserTestAnswer answer = answerRepository.findByAttempt_AttemptIdAndQuestion_QuestionId(attemptId, question.getQuestionId())
                .orElseGet(() -> UserTestAnswer.builder()
                        .attempt(attempt)
                        .question(question)
                        .build());

        answer.setSelectedOption(selectedOpt);
        answer.setIsCorrect(isCorrect);

        answerRepository.save(answer);
        log.info("Recorded answer for Attempt #{} - Question #{}: selected='{}', isCorrect={}",
                attemptId, question.getQuestionId(), selectedOpt, isCorrect);
    }

    private String normalizeOption(String option) {
        if (option == null) return "";
        String clean = option.trim().toUpperCase();
        if (clean.startsWith("OPTION")) {
            clean = clean.replace("OPTION", "").replace("_", "").trim();
        }
        return clean;
    }

    @Transactional
    public void batchRecordAnswers(Long attemptId, BatchSubmitAnswersRequest request, Long userId) {
        if (request.getAnswers() != null) {
            for (SubmitAnswerRequest answerReq : request.getAnswers()) {
                recordAnswer(attemptId, answerReq, userId);
            }
        }
    }

    // ==========================================
    // GIAI ĐOẠN 4: NỘP BÀI, THỐNG KÊ & KÍCH HOẠT RAG AI
    // ==========================================

    @Transactional
    public TestSubmissionResultResponse submitAttempt(Long attemptId, Long userId) {
        UserTestAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("UserTestAttempt", "attemptId", attemptId));

        if (!attempt.getUser().getUserId().equals(userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền nộp bài cho lượt thi này");
        }
        if (attempt.getCompletedAt() != null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Lượt thi này đã nộp bài trước đó");
        }

        List<UserTestAnswer> answers = answerRepository.findByAttempt_AttemptId(attemptId);
        if (answers.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Bạn chưa trả lời câu hỏi nào, không thể nộp bài");
        }

        int listeningTotal = 0;
        int listeningCorrect = 0;
        int readingTotal = 0;
        int readingCorrect = 0;

        // Thống kê theo subSkillTag: tổng số câu và số câu sai
        Map<String, int[]> subSkillStats = new HashMap<>(); // [total, wrong]

        for (UserTestAnswer ans : answers) {
            PlacementQuestion q = ans.getQuestion();
            String skill = (q.getPassage() != null && q.getPassage().getSkillType() != null)
                    ? q.getPassage().getSkillType().toUpperCase()
                    : "READING";

            boolean correct = Boolean.TRUE.equals(ans.getIsCorrect());

            if ("LISTENING".equalsIgnoreCase(skill)) {
                listeningTotal++;
                if (correct) listeningCorrect++;
            } else {
                readingTotal++;
                if (correct) readingCorrect++;
            }

            String subSkill = q.getSubSkillTag() != null ? q.getSubSkillTag().toUpperCase() : "GENERAL";
            subSkillStats.putIfAbsent(subSkill, new int[]{0, 0});
            subSkillStats.get(subSkill)[0]++; // total
            if (!correct) {
                subSkillStats.get(subSkill)[1]++; // wrong
            }
        }

        double listeningScore = listeningTotal > 0 ? ((double) listeningCorrect / listeningTotal) * 100.0 : 0.0;
        double readingScore = readingTotal > 0 ? ((double) readingCorrect / readingTotal) * 100.0 : 0.0;
        int totalQuestions = listeningTotal + readingTotal;
        int totalCorrect = listeningCorrect + readingCorrect;
        double totalScore = totalQuestions > 0 ? ((double) totalCorrect / totalQuestions) * 100.0 : 0.0;

        // Xếp loại trình độ CEFR
        String evaluatedLevel;
        if (totalScore >= 90.0) evaluatedLevel = "C1";
        else if (totalScore >= 75.0) evaluatedLevel = "B2";
        else if (totalScore >= 60.0) evaluatedLevel = "B1";
        else if (totalScore >= 45.0) evaluatedLevel = "A2";
        else evaluatedLevel = "A1";

        // Tạo danh sách điểm yếu (các subSkillTag có tỷ lệ sai >= 40% hoặc sai từ 1 câu trở lên)
        List<String> weakSubSkills = new ArrayList<>();
        List<Map<String, Object>> weaknessAnalysisList = new ArrayList<>();

        for (Map.Entry<String, int[]> entry : subSkillStats.entrySet()) {
            String tag = entry.getKey();
            int total = entry.getValue()[0];
            int wrong = entry.getValue()[1];
            double errorRate = total > 0 ? ((double) wrong / total) * 100.0 : 0.0;

            Map<String, Object> stat = new HashMap<>();
            stat.put("subSkillTag", tag);
            stat.put("totalQuestions", total);
            stat.put("wrongCount", wrong);
            stat.put("errorRate", Math.round(errorRate * 10.0) / 10.0);
            weaknessAnalysisList.add(stat);

            if (wrong > 0 && errorRate >= 30.0) {
                weakSubSkills.add(tag);
            }
        }

        // Nếu người dùng làm đúng hết hoặc ít sai, đưa các subSkill đã thi để củng cố
        if (weakSubSkills.isEmpty()) {
            weakSubSkills.addAll(subSkillStats.keySet());
        }

        String weaknessAnalysisJson;
        try {
            weaknessAnalysisJson = objectMapper.writeValueAsString(weakSubSkills);
        } catch (Exception ex) {
            weaknessAnalysisJson = "[\"LISTENING_DETAIL\", \"READING_VOCAB\"]";
        }

        // Cập nhật Attempt
        attempt.setListeningScore(Math.round(listeningScore * 10.0) / 10.0);
        attempt.setReadingScore(Math.round(readingScore * 10.0) / 10.0);
        attempt.setTotalScore(Math.round(totalScore * 10.0) / 10.0);
        attempt.setEvaluatedLevel(evaluatedLevel);
        attempt.setWeaknessAnalysisJson(weaknessAnalysisJson);
        attempt.setCompletedAt(LocalDateTime.now());
        attemptRepository.save(attempt);

        log.info("Attempt {} submitted. Evaluated Level: {}, Weaknesses: {}", attemptId, evaluatedLevel, weaknessAnalysisJson);

        // KÍCH HOẠT RAG + LLM SINH LỘ TRÌNH 7 NGÀY
        GenerateRoadmapRequest roadmapReq = GenerateRoadmapRequest.builder()
                .userId(userId)
                .attemptId(attemptId)
                .evaluatedLevel(evaluatedLevel)
                .weaknessAnalysisJson(weaknessAnalysisJson)
                .build();

        RoadmapResponse generatedRoadmap = ragRoadmapService.generateAndSaveRoadmap(roadmapReq, userId);

        attempt.setAiOverallFeedback(generatedRoadmap.getRoadmapTitle());
        attemptRepository.save(attempt);

        return TestSubmissionResultResponse.builder()
                .attemptId(attemptId)
                .userId(userId)
                .listeningScore(attempt.getListeningScore())
                .readingScore(attempt.getReadingScore())
                .totalScore(attempt.getTotalScore())
                .evaluatedLevel(evaluatedLevel)
                .weaknessAnalysisJson(weaknessAnalysisJson)
                .aiOverallFeedback(generatedRoadmap.getRoadmapTitle())
                .completedAt(attempt.getCompletedAt())
                .isPlacementCompleted(true)
                .generatedRoadmap(generatedRoadmap)
                .build();
    }
}

