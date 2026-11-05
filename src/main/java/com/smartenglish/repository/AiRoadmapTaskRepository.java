package com.smartenglish.repository;

import com.smartenglish.entity.AiRoadmapTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiRoadmapTaskRepository extends JpaRepository<AiRoadmapTask, Long> {
    List<AiRoadmapTask> findByRoadmapDay_RoadmapDayIdOrderByOrderIndexAsc(Long roadmapDayId);
}

