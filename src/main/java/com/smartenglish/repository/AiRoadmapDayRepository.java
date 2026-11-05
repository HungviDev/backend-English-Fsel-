package com.smartenglish.repository;

import com.smartenglish.entity.AiRoadmapDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiRoadmapDayRepository extends JpaRepository<AiRoadmapDay, Long> {
    List<AiRoadmapDay> findByRoadmap_RoadmapIdOrderByDayNumberAsc(Long roadmapId);
}

