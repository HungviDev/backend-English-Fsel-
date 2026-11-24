package com.smartenglish.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlacementTestResponse {

    private Long testId;
    private String title;
    private String description;
    private Integer durationMinutes;
    private Integer totalQuestions;
    private Boolean isActive;

    private Long createdById;
    private String createdByName;
    private String createdByEmail;

    private Integer passageCount;
    private Integer questionCount;

    private LocalDateTime createdAt;
}

