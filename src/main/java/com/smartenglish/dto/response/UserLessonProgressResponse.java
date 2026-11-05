package com.smartenglish.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLessonProgressResponse {
    private Long progressId;
    private Long userId;
    private Long lessonId;
    private String lessonTitle;
    private String status;
    private Double practiceScore;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime completedAt;
}

