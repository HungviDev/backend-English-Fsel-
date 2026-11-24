package com.smartenglish.dto.request;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTestAttemptRequest {

    private Double listeningScore;

    private Double readingScore;

    private Double totalScore;

    private String evaluatedLevel; // A1, A2, B1, B2, C1, C2

    private String weaknessAnalysisJson;

    private String aiOverallFeedback;
}

