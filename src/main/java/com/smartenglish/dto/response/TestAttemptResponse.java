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
public class TestAttemptResponse {
    private Long attemptId;
    private Long testId;
    private String testTitle;
    private Integer durationMinutes;
    private LocalDateTime startedAt;
    private String status;
}

