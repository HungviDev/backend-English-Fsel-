package com.smartenglish.repository;

import com.smartenglish.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourse_CourseIdAndIsPublishedTrueOrderByOrderIndexAsc(Long courseId);

    @Query("SELECT l FROM Lesson l WHERE l.isPublished = true AND l.subSkillTag IN :subSkillTags AND (l.course.targetLevel = :targetLevel OR :targetLevel IS NULL)")
    List<Lesson> findRelevantLessonsForRag(@Param("subSkillTags") Collection<String> subSkillTags,
                                           @Param("targetLevel") String targetLevel);

    List<Lesson> findBySubSkillTagInAndIsPublishedTrue(Collection<String> subSkillTags);
}

