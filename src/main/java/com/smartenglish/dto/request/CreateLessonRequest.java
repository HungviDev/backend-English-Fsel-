package com.smartenglish.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLessonRequest {

    @NotBlank(message = "Lesson title cannot be blank")
    private String title;

    @NotBlank(message = "Skill type cannot be blank (LISTENING, READING, VOCABULARY, GRAMMAR)")
    private String skillType;

    @NotBlank(message = "Sub-skill tag cannot be blank (e.g. LISTENING_DETAIL, READING_VOCAB)")
    private String subSkillTag;

    private String contentHtml;
    private String audioUrl;
    private String videoUrl;

    @Builder.Default
    private Integer estimatedMinutes = 15;

    @Builder.Default
    private Integer orderIndex = 0;

    @Builder.Default
    private Boolean isPublished = true;
}

