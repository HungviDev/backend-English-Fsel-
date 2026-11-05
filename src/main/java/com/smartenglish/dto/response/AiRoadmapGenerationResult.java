package com.smartenglish.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiRoadmapGenerationResult {
    private String roadmapTitle;
    private String aiOverallFeedback;
    private String targetWeaknesses;
    private List<AiDayResult> days;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiDayResult {
        private Integer dayNumber;
        private String dailyTopic;
        private String focusSkill;
        private String targetSubSkill;
        private String aiDailyAdvice;
        private List<AiTaskResult> tasks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiTaskResult {
        private String taskTitle;
        private String taskType;
        private Long referenceLessonId;
        private Integer estimatedMinutes;
        private Integer orderIndex;
        private Object aiGeneratedContentJson; // Can be String or Map/Node; we serialize to String
    }
}

