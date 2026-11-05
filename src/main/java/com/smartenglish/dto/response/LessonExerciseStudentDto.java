package com.smartenglish.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonExerciseStudentDto {
    private Long exerciseId;
    private Long lessonId;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
}

