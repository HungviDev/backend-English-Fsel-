package com.smartenglish.repository;

import com.smartenglish.entity.UserTestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTestAttemptRepository extends JpaRepository<UserTestAttempt, Long> {
    List<UserTestAttempt> findByUser_UserIdOrderByStartedAtDesc(Long userId);
    Optional<UserTestAttempt> findFirstByUser_UserIdOrderByStartedAtDesc(Long userId);
}

