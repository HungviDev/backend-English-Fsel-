package com.smartenglish.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDetailResponse {

    private Long userId;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String currentLevel;
    private Boolean isPlacementCompleted;
    private Boolean isActive;
    private LocalDateTime createdAt;

    // Lộ trình học tập đang hoạt động
    private Long activeRoadmapId;
    private String activeRoadmapTitle;

    // Lịch sử các lần thi đầu vào
    private List<TestAttemptSummaryDto> attempts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestAttemptSummaryDto {
        private Long attemptId;
        private Long testId;
        private String testTitle;
        private Double listeningScore;
        private Double readingScore;
        private Double totalScore;
        private String evaluatedLevel;
        private String aiOverallFeedback;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
    }
}

