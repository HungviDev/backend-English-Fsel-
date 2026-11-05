package com.smartenglish.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionPassageStudentDto {
    private Long passageId;
    private String skillType; // LISTENING hoặc READING
    private String title;
    private String passageText;
    private String audioUrl;
    private Integer orderIndex;
    private List<PlacementQuestionStudentDto> questions;
}

