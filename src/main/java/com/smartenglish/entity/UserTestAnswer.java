package com.smartenglish.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_test_answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTestAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answerId")
    private Long answerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attemptId", nullable = false)
    private UserTestAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionId", nullable = false)
    private PlacementQuestion question;

    @Column(name = "selectedOption", length = 10)
    private String selectedOption;

    @Column(name = "isCorrect")
    private Boolean isCorrect;
}

