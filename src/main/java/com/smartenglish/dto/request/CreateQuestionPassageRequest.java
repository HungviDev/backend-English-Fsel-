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
public class CreateQuestionPassageRequest {

    @NotBlank(message = "Skill type cannot be blank (READING hoặc LISTENING)")
    private String skillType;

    private String title;

    private String passageText;

    private String audioUrl;

    @Builder.Default
    private Integer orderIndex = 0;
}

