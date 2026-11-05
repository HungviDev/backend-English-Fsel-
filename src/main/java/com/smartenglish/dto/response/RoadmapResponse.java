package com.smartenglish.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapResponse {
    private Long roadmapId;
    private Long userId;
    private Long sourceAttemptId;
    private String roadmapTitle;
    private String targetWeaknesses;
    private String aiModelVersion;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private LocalDateTime createdAt;
    private List<RoadmapDayResponse> days;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoadmapDayResponse {
        private Long roadmapDayId;
        private Integer dayNumber;
        private String dailyTopic;
        private String focusSkill;
        private String targetSubSkill;
        private String aiDailyAdvice;
        private Boolean isUnlocked;
        private Boolean isCompleted;
        private LocalDateTime completedAt;
        private List<RoadmapTaskResponse> tasks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoadmapTaskResponse {
        private Long taskId;
        private String taskTitle;
        private String taskType;
        private Long referenceLessonId;
        private String aiGeneratedContentJson;
        private Integer estimatedMinutes;
        private Integer orderIndex;
        private Double userScore;
        private Boolean isCompleted;
    }
}

