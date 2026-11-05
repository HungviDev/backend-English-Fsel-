package com.smartenglish.repository;

import com.smartenglish.entity.UserTestAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTestAnswerRepository extends JpaRepository<UserTestAnswer, Long> {
    List<UserTestAnswer> findByAttempt_AttemptId(Long attemptId);
    Optional<UserTestAnswer> findByAttempt_AttemptIdAndQuestion_QuestionId(Long attemptId, Long questionId);
}

