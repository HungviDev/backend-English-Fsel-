package com.smartenglish.dto.request;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserTestAnswerRequest {

    private String selectedOption; // A, B, C, D

    private Boolean isCorrect;

    @Builder.Default
    private Boolean recalculateScores = true; // Tự động tính lại điểm sau khi sửa câu trả lời
}

