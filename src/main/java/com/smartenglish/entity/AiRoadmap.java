package com.smartenglish.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_roadmaps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRoadmap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roadmapId")
    private Long roadmapId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sourceAttemptId")
    private UserTestAttempt sourceAttempt;

    @Column(name = "roadmapTitle", nullable = false, length = 255)
    private String roadmapTitle;

    @Column(name = "targetWeaknesses", length = 500)
    private String targetWeaknesses;

    @Column(name = "aiModelVersion", length = 100)
    private String aiModelVersion;

    @Column(name = "startDate")
    private LocalDate startDate;

    @Column(name = "endDate")
    private LocalDate endDate;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 50)
    private String status = "ACTIVE"; // ACTIVE, COMPLETED, ARCHIVED

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "roadmap", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNumber ASC")
    @JsonManagedReference
    private List<AiRoadmapDay> days = new ArrayList<>();

    public void addDay(AiRoadmapDay day) {
        days.add(day);
        day.setRoadmap(this);
    }
}

