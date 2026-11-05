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
public class GenerateRoadmapRequest {

    private Long userId;

    private Long attemptId;

    @NotBlank(message = "Evaluated level cannot be blank (e.g. A1, A2, B1, B2, C1, C2)")
    private String evaluatedLevel;

    @NotBlank(message = "Weakness analysis JSON or tags cannot be blank")
    private String weaknessAnalysisJson;
}

