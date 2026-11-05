package com.smartenglish.repository;

import com.smartenglish.entity.PlacementTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlacementTestRepository extends JpaRepository<PlacementTest, Long> {
    Optional<PlacementTest> findFirstByIsActiveTrue();
}

