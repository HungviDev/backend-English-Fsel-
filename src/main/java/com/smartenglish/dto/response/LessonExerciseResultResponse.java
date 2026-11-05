package com.smartenglish.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonExerciseResultResponse {
    private Long lessonId;
    private int totalQuestions;
    private int correctCount;
    private Double practiceScore; // Percentage 0 - 100
    private String status; // COMPLETED
    private List<ExerciseFeedbackItem> feedback;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExerciseFeedbackItem {
        private Long exerciseId;
        private String questionText;
        private String selectedOption;
        private String correctOption;
        private Boolean isCorrect;
        private String explanation;
    }
}

