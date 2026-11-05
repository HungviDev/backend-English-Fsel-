package com.smartenglish.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_roadmap_days")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRoadmapDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roadmapDayId")
    private Long roadmapDayId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roadmapId", nullable = false)
    @JsonBackReference
    private AiRoadmap roadmap;

    @Column(name = "dayNumber", nullable = false)
    private Integer dayNumber; // 1 to 7

    @Column(name = "dailyTopic", nullable = false, length = 255)
    private String dailyTopic;

    @Column(name = "focusSkill", nullable = false, length = 50)
    private String focusSkill; // LISTENING, READING, VOCABULARY, GRAMMAR

    @Column(name = "targetSubSkill", length = 100)
    private String targetSubSkill;

    @Column(name = "aiDailyAdvice", columnDefinition = "NVARCHAR(MAX)")
    private String aiDailyAdvice;

    @Builder.Default
    @Column(name = "isUnlocked", nullable = false)
    private Boolean isUnlocked = false;

    @Builder.Default
    @Column(name = "isCompleted", nullable = false)
    private Boolean isCompleted = false;

    @Column(name = "completedAt")
    private LocalDateTime completedAt;

    @Builder.Default
    @OneToMany(mappedBy = "roadmapDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @JsonManagedReference
    private List<AiRoadmapTask> tasks = new ArrayList<>();

    public void addTask(AiRoadmapTask task) {
        tasks.add(task);
        task.setRoadmapDay(this);
    }
}

