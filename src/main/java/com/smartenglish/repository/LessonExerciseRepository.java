package com.smartenglish.repository;

import com.smartenglish.entity.LessonExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonExerciseRepository extends JpaRepository<LessonExercise, Long> {
    List<LessonExercise> findByLesson_LessonId(Long lessonId);
}

