package com.smartenglish.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_test_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTestAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attemptId")
    private Long attemptId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "testId", nullable = false)
    private PlacementTest test;

    @Builder.Default
    @Column(name = "listeningScore")
    private Double listeningScore = 0.0;

    @Builder.Default
    @Column(name = "readingScore")
    private Double readingScore = 0.0;

    @Builder.Default
    @Column(name = "totalScore")
    private Double totalScore = 0.0;

    @Builder.Default
    @Column(name = "evaluatedLevel", length = 20)
    private String evaluatedLevel = "PENDING"; // PENDING khi đang thi, A1-C2 khi đã nộp bài

    @Builder.Default
    @Column(name = "weaknessAnalysisJson", columnDefinition = "NVARCHAR(MAX)")
    private String weaknessAnalysisJson = "[]";

    @Builder.Default
    @Column(name = "aiOverallFeedback", columnDefinition = "NVARCHAR(MAX)")
    private String aiOverallFeedback = "";

    @CreationTimestamp
    @Column(name = "startedAt", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "completedAt")
    private LocalDateTime completedAt;
}

