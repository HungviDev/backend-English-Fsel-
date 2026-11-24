package com.smartenglish.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lessonId")
    private Long lessonId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courseId", nullable = false)
    private Course course;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "skillType", nullable = false, length = 50)
    private String skillType; // LISTENING, READING, VOCABULARY, GRAMMAR

    @Column(name = "subSkillTag", nullable = false, length = 100)
    private String subSkillTag; // e.g. LISTENING_DETAIL, READING_VOCAB, GRAMMAR_TENSE

    @Column(name = "contentHtml", columnDefinition = "NVARCHAR(MAX)")
    private String contentHtml;

    @Column(name = "audioUrl", length = 500)
    private String audioUrl;

    @Column(name = "videoUrl", length = 500)
    private String videoUrl;

    @Builder.Default
    @Column(name = "estimatedMinutes", nullable = false)
    private Integer estimatedMinutes = 15;

    @Builder.Default
    @Column(name = "orderIndex", nullable = false)
    private Integer orderIndex = 0;

    @Builder.Default
    @Column(name = "isPublished", nullable = false)
    private Boolean isPublished = true;
}

