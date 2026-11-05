package com.smartenglish.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lesson_exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exerciseId")
    private Long exerciseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lessonId", nullable = false)
    private Lesson lesson;

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

