package com.smartenglish.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentTestDetailResponse {

    private Long attemptId;
    private Long userId;
    private String studentName;
    private String studentEmail;
    private Long testId;
    private String testTitle;

    private Double listeningScore;
    private Double readingScore;
    private Double totalScore;
    private String evaluatedLevel;
    private String weaknessAnalysisJson;
    private String aiOverallFeedback;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private List<UserAnswerDetailDto> answers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserAnswerDetailDto {
        private Long answerId;
        private Long questionId;
        private String skillType; // LISTENING hoặc READING
        private String subSkillTag;
        private String difficultyLevel;
        private String questionText;
        private String optionA;
        private String optionB;
        private String optionC;
        private String optionD;
        private String selectedOption;
        private String correctOption;
        private Boolean isCorrect;
        private String explanation;
    }
}

