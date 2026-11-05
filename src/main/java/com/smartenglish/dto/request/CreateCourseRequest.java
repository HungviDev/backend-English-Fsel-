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
public class CreateCourseRequest {

    @NotBlank(message = "Title cannot be blank")
    private String title;

    private String description;
    private String thumbnailUrl;

    @NotBlank(message = "Target level cannot be blank")
    private String targetLevel;

    @Builder.Default
    private Integer orderIndex = 0;

    @Builder.Default
    private Boolean isPublished = true;
}

