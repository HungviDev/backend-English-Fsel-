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
public class TestSubmissionResultResponse {
    private Long attemptId;
    private Long userId;
    private Double listeningScore;
    private Double readingScore;
    private Double totalScore;
    private String evaluatedLevel;
    private String weaknessAnalysisJson;
    private String aiOverallFeedback;
    private LocalDateTime completedAt;
    private Boolean isPlacementCompleted;
    private RoadmapResponse generatedRoadmap;
}

