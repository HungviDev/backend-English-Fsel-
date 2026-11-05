package com.smartenglish.repository;

import com.smartenglish.entity.QuestionPassage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionPassageRepository extends JpaRepository<QuestionPassage, Long> {
    List<QuestionPassage> findByTest_TestIdOrderByOrderIndexAsc(Long testId);
}

