package com.smartenglish.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ai_roadmap_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRoadmapTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "taskId")
    private Long taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roadmapDayId", nullable = false)
    @JsonBackReference
    private AiRoadmapDay roadmapDay;

    @Column(name = "taskTitle", nullable = false, length = 255)
    private String taskTitle;

    @Column(name = "taskType", nullable = false, length = 50)
    private String taskType; // THEORY_LESSON, AI_PRACTICE, VOCAB_QUIZ, LISTENING_DRILL

    @Column(name = "referenceLessonId")
    private Long referenceLessonId;

    @Column(name = "aiGeneratedContentJson", columnDefinition = "NVARCHAR(MAX)")
    private String aiGeneratedContentJson;

    @Builder.Default
    @Column(name = "estimatedMinutes", nullable = false)
    private Integer estimatedMinutes = 15;

    @Builder.Default
    @Column(name = "orderIndex", nullable = false)
    private Integer orderIndex = 0;

    @Column(name = "userScore")
    private Double userScore;

    @Builder.Default
    @Column(name = "isCompleted", nullable = false)
    private Boolean isCompleted = false;
}

