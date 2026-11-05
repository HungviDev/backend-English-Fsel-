package com.smartenglish.repository;

import com.smartenglish.entity.AiRoadmap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiRoadmapRepository extends JpaRepository<AiRoadmap, Long> {
    List<AiRoadmap> findByUser_UserIdOrderByCreatedAtDesc(Long userId);
    Optional<AiRoadmap> findFirstByUser_UserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
}

