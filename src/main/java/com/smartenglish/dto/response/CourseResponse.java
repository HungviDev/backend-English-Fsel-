package com.smartenglish.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {

    private Long courseId;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String targetLevel;
    private Integer orderIndex;
    private Boolean isPublished;

    // Creator details
    private Long createdById;
    private String createdByName;
    private String createdByEmail;

    // Statistics
    private Integer totalLessons;

    private LocalDateTime createdAt;
}

