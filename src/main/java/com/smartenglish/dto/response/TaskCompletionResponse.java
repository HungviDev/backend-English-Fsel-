package com.smartenglish.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCompletionResponse {
    private Long taskId;
    private Boolean isCompleted;
    private Double userScore;
    private Integer dayNumber;
    private Boolean dayIsCompleted;
    private Boolean nextDayUnlocked;
    private String message;
}

