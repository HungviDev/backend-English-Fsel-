package com.smartenglish.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentSummaryResponse {

    private Long userId;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String currentLevel;
    private Boolean isPlacementCompleted;
    private Boolean isActive;
    private LocalDateTime createdAt;

    // Thông tin thi cử
    private Integer totalAttempts;
    private Double latestTotalScore;
    private String latestEvaluatedLevel;
    private LocalDateTime latestCompletedAt;
}

