package com.smartenglish.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "placement_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PlacementQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "questionId")
    private Long questionId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passageId")
    private QuestionPassage passage;

    @Column(name = "subSkillTag", nullable = false, length = 100)
    private String subSkillTag; // e.g. LISTENING_DETAIL, READING_VOCAB

    @Column(name = "difficultyLevel", length = 20)
    private String difficultyLevel; // A1, A2, B1, B2, C1, C2

    @Column(name = "questionText", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String questionText;

    @Column(name = "optionA", length = 500)
    private String optionA;

    @Column(name = "optionB", length = 500)
    private String optionB;

    @Column(name = "optionC", length = 500)
    private String optionC;

    @Column(name = "optionD", length = 500)
    private String optionD;

    @Column(name = "correctOption", nullable = false, length = 10)
    private String correctOption; // A, B, C, D

    @Column(name = "explanation", columnDefinition = "NVARCHAR(MAX)")
    private String explanation;
}
