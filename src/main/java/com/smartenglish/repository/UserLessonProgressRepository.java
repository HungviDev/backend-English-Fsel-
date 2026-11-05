package com.smartenglish.repository;

import com.smartenglish.entity.UserLessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLessonProgressRepository extends JpaRepository<UserLessonProgress, Long> {
    Optional<UserLessonProgress> findByUser_UserIdAndLesson_LessonId(Long userId, Long lessonId);
    List<UserLessonProgress> findByUser_UserId(Long userId);
}

