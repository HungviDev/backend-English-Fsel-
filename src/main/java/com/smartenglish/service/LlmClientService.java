package com.smartenglish.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartenglish.config.AiProperties;
import com.smartenglish.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmClientService {

    private final AiProperties aiProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    /**
     * Gửi yêu cầu hoàn thành chat sang LLM hoặc trả về Mock Data nếu mockEnabled = true
     */
    public String generateChatCompletion(String systemPrompt, String userPrompt) {
        if (aiProperties.isMockEnabled()) {
            log.info("[LLM MOCK MODE ACTIVE] Bỏ qua gọi API thật. Trả về cấu trúc 7 ngày mẫu hoàn chỉnh.");
            return getMockRoadmapJsonResponse();
        }

        log.info("[LLM REAL API] Gửi yêu cầu tới: {} (Model: {})", aiProperties.getApiUrl(), aiProperties.getModel());

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", aiProperties.getModel());
            requestBody.put("temperature", 0.7);

            List<Map<String, String>> messages = List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            );
            requestBody.put("messages", messages);

            // Ép khuôn JSON Object nếu provider hỗ trợ (OpenAI standard)
            requestBody.put("response_format", Map.of("type", "json_object"));

            String responseBody = restClient.post()
                    .uri(aiProperties.getApiUrl())
                    .header("Authorization", "Bearer " + aiProperties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                String content = choices.get(0).path("message").path("content").asText();
                log.debug("LLM Response Content: {}", content);
                return content;
            } else {
                throw new AppException(HttpStatus.BAD_GATEWAY, "LLM API response did not contain expected choices array");
            }
        } catch (Exception ex) {
            log.warn("[LLM API WARNING] Gọi LLM API thất bại ({}). Tự động fallback sang Mock Data chuẩn 7 ngày để đảm bảo quy trình nộp bài của học viên không bị gián đoạn!", ex.getMessage());
            return getMockRoadmapJsonResponse();
        }
    }

    /**
     * Dữ liệu Mock chuẩn xác 7 ngày học theo đúng schema và SQL CHECK constraints:
     * - focusSkill: LISTENING | READING | MIXED_REVIEW
     * - taskType: LESSON | PRACTICE
     */
    private String getMockRoadmapJsonResponse() {
        return """
        {
          "roadmapTitle": "Lộ trình 7 ngày bứt phá kỹ năng Nghe & Từ vựng (B1-B2)",
          "aiOverallFeedback": "Dựa trên bài đánh giá đầu vào, bạn có nền tảng ngữ pháp cơ bản khá vững (chiếm 70% câu đúng), tuy nhiên gặp khó khăn đáng kể ở kỹ năng Nghe chi tiết (LISTENING_DETAIL) khi tốc độ nói nhanh và vốn từ vựng học thuật (READING_VOCAB). Lộ trình 7 ngày này được thiết kế để bù đắp điểm yếu của bạn bằng cách kết hợp bài giảng chuyên sâu của giáo viên và bài tập luyện phản xạ do AI cá nhân hóa.",
          "targetWeaknesses": "LISTENING_DETAIL, READING_VOCAB",
          "days": [
            {
              "dayNumber": 1,
              "dailyTopic": "Chiến lược bắt từ khóa (Keywords) trong hội thoại ngắn",
              "focusSkill": "LISTENING",
              "targetSubSkill": "LISTENING_DETAIL",
              "aiDailyAdvice": "Đừng cố gắng dịch từng từ sang tiếng Việt. Hãy chú ý đến các từ được nhấn âm (Stressed words: danh từ, động từ chính).",
              "tasks": [
                {
                  "taskTitle": "Bài giảng trọng tâm: Phương pháp bắt từ khóa nghe chi tiết",
                  "taskType": "LESSON",
                  "referenceLessonId": 1,
                  "estimatedMinutes": 15,
                  "orderIndex": 1,
                  "aiGeneratedContentJson": "{\\"notes\\": \\"Xem kỹ phần phân tích bẫy phát âm đuôi -ed và nối âm trong bài giảng.\\"}"
                },
                {
                  "taskTitle": "Luyện tập phản xạ AI: Nhận diện từ khóa với 5 câu thoại tốc độ chuẩn",
                  "taskType": "PRACTICE",
                  "referenceLessonId": null,
                  "estimatedMinutes": 20,
                  "orderIndex": 2,
                  "aiGeneratedContentJson": "{\\"exerciseType\\": \\"DICTATION\\", \\"questions\\": [{\\"q\\": \\"She scheduled the conference for next Wednesday.\\", \\"keywords\\": [\\"scheduled\\", \\"conference\\", \\"Wednesday\\"]}]}"
                }
              ]
            },
            {
              "dayNumber": 2,
              "dailyTopic": "Mở rộng vốn từ vựng Chủ đề Môi trường & Khoa học",
              "focusSkill": "READING",
              "targetSubSkill": "READING_VOCAB",
              "aiDailyAdvice": "Học từ vựng theo cụm Collocations thay vì học từ đơn lẻ để ghi nhớ tự nhiên hơn.",
              "tasks": [
                {
                  "taskTitle": "Bài giảng: 20 Academic Collocations chủ đề Environment",
                  "taskType": "LESSON",
                  "referenceLessonId": 2,
                  "estimatedMinutes": 15,
                  "orderIndex": 1,
                  "aiGeneratedContentJson": "{\\"targetWords\\": [\\"carbon footprint\\", \\"biodiversity\\", \\"sustainable practice\\"]}"
                },
                {
                  "taskTitle": "Flashcard & Trắc nghiệm từ vựng thông minh",
                  "taskType": "PRACTICE",
                  "referenceLessonId": null,
                  "estimatedMinutes": 15,
                  "orderIndex": 2,
                  "aiGeneratedContentJson": "{\\"totalCards\\": 10, \\"quizMode\\": \\"MULTIPLE_CHOICE\\"}"
                }
              ]
            },
            {
              "dayNumber": 3,
              "dailyTopic": "Nhận diện bẫy thông tin đối lập trong bài nghe",
              "focusSkill": "LISTENING",
              "targetSubSkill": "LISTENING_DETAIL",
              "aiDailyAdvice": "Cảnh giác cao độ với các liên từ chuyển ý: 'However', 'Actually', 'On the other hand'. Thông tin đúng thường nằm sau các từ này.",
              "tasks": [
                {
                  "taskTitle": "Phân tích bẫy thông tin qua đoạn hội thoại thực tế",
                  "taskType": "LESSON",
                  "referenceLessonId": 1,
                  "estimatedMinutes": 15,
                  "orderIndex": 1,
                  "aiGeneratedContentJson": "{\\"tip\\": \\"Người nói hay đưa ra phương án A trước, sau đó đổi ý chọn phương án B.\\"}"
                },
                {
                  "taskTitle": "AI Listening Drill: Bắt lỗi thay đổi quyết định",
                  "taskType": "PRACTICE",
                  "referenceLessonId": null,
                  "estimatedMinutes": 20,
                  "orderIndex": 2,
                  "aiGeneratedContentJson": "{\\"dialogueCount\\": 3, \\"focus\\": \\"distractor-filtering\\"}"
                }
              ]
            },
            {
              "dayNumber": 4,
              "dailyTopic": "Kỹ thuật đoán nghĩa từ qua ngữ cảnh văn bản (Context Clues)",
              "focusSkill": "READING",
              "targetSubSkill": "READING_VOCAB",
              "aiDailyAdvice": "Khi gặp từ mới, hãy nhìn các câu trước và sau để tìm từ đồng nghĩa, từ trái nghĩa hoặc định nghĩa giải thích kèm theo.",
              "tasks": [
                {
                  "taskTitle": "Bài đọc ngắn ứng dụng Context Clues",
                  "taskType": "LESSON",
                  "referenceLessonId": 2,
                  "estimatedMinutes": 25,
                  "orderIndex": 1,
                  "aiGeneratedContentJson": "{\\"passageLength\\": 250, \\"unknownWordsTarget\\": 5}"
                }
              ]
            },
            {
              "dayNumber": 5,
              "dailyTopic": "Nghe số liệu, ngày tháng và tên riêng chính xác",
              "focusSkill": "LISTENING",
              "targetSubSkill": "LISTENING_DETAIL",
              "aiDailyAdvice": "Phân biệt rõ âm đuôi -teen (trọng âm rơi vào teen) và -ty (trọng âm rơi vào âm tiết đầu).",
              "tasks": [
                {
                  "taskTitle": "Luyện nghe số liệu và ghi chú thông tin biểu mẫu (Form Completion)",
                  "taskType": "PRACTICE",
                  "referenceLessonId": null,
                  "estimatedMinutes": 20,
                  "orderIndex": 1,
                  "aiGeneratedContentJson": "{\\"exercise\\": \\"FORM_FILLING\\", \\"audioSimulation\\": true}"
                }
              ]
            },
            {
              "dayNumber": 6,
              "dailyTopic": "Ôn tập tổng hợp & Củng cố liên kết Từ vựng - Nghe hiểu",
              "focusSkill": "MIXED_REVIEW",
              "targetSubSkill": "READING_VOCAB",
              "aiDailyAdvice": "Hôm nay hãy thử phát âm lại các từ vựng đã học trong 5 ngày qua để củng cố cả trí nhớ thị giác lẫn âm thanh.",
              "tasks": [
                {
                  "taskTitle": "Thử thách ghép từ và hoàn thành bài đọc học thuật",
                  "taskType": "PRACTICE",
                  "referenceLessonId": null,
                  "estimatedMinutes": 25,
                  "orderIndex": 1,
                  "aiGeneratedContentJson": "{\\"targetAccuracy\\": 80, \\"wordCount\\": 30}"
                }
              ]
            },
            {
              "dayNumber": 7,
              "dailyTopic": "Mini-Test Kiểm tra đánh giá tiến bộ sau 7 ngày",
              "focusSkill": "MIXED_REVIEW",
              "targetSubSkill": "LISTENING_DETAIL",
              "aiDailyAdvice": "Tự tin làm bài trong điều kiện không dùng từ điển để đánh giá chuẩn xác sự tiến bộ nhé!",
              "tasks": [
                {
                  "taskTitle": "Mini Check-point Test (10 câu Nghe + 10 câu Từ vựng)",
                  "taskType": "PRACTICE",
                  "referenceLessonId": null,
                  "estimatedMinutes": 30,
                  "orderIndex": 1,
                  "aiGeneratedContentJson": "{\\"totalQuestions\\": 20, \\"durationMinutes\\": 30}"
                }
              ]
            }
          ]
        }
        """;
    }
}
