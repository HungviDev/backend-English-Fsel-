package com.smartenglish.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "question_passages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionPassage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "passageId")
    private Long passageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "testId", nullable = false)
    private PlacementTest test;

    @Column(name = "skillType", nullable = false, length = 50)
    private String skillType; // LISTENING, READING

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "passageText", columnDefinition = "NVARCHAR(MAX)")
    private String passageText;

    @Column(name = "audioUrl", length = 500)
    private String audioUrl;

    @Builder.Default
    @Column(name = "orderIndex", nullable = false)
    private Integer orderIndex = 0;
}

