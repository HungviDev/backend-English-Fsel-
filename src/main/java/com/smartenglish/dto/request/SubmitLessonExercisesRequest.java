package com.smartenglish.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitLessonExercisesRequest {

    @NotEmpty(message = "Answers list cannot be empty")
    @Valid
    private List<ExerciseAnswerItem> answers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExerciseAnswerItem {
        @NotNull(message = "Exercise ID is required")
        private Long exerciseId;

        @NotNull(message = "Selected option is required (A, B, C, D)")
        private String selectedOption;
    }
}

