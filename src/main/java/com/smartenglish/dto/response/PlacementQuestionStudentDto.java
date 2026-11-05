package com.smartenglish.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlacementQuestionStudentDto {
    private Long questionId;
    private String subSkillTag;
    private String difficultyLevel;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    // Bỏ correctOption và explanation để bảo mật đề thi khi học viên đang làm bài
}

