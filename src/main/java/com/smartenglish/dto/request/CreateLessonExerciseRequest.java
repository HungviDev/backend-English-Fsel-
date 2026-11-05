package com.smartenglish.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLessonExerciseRequest {

    @NotBlank(message = "Question text cannot be blank")
    private String questionText;

    @NotBlank(message = "Option A cannot be blank")
    private String optionA;

    @NotBlank(message = "Option B cannot be blank")
    private String optionB;

    private String optionC;
    private String optionD;

    @NotBlank(message = "Correct option cannot be blank (A, B, C, D)")
    private String correctOption;

    private String explanation;
}

