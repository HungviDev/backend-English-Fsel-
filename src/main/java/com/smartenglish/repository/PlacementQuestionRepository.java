package com.smartenglish.repository;

import com.smartenglish.entity.PlacementQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlacementQuestionRepository extends JpaRepository<PlacementQuestion, Long> {
    List<PlacementQuestion> findByPassage_PassageId(Long passageId);
    List<PlacementQuestion> findByPassage_Test_TestId(Long testId);
}

