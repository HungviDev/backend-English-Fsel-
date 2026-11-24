package com.smartenglish.repository;

import com.smartenglish.entity.PlacementTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlacementTestRepository extends JpaRepository<PlacementTest, Long> {

    Optional<PlacementTest> findFirstByIsActiveTrue();

    @Query(value = "SELECT t FROM PlacementTest t WHERE " +
           "(:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:isActive IS NULL OR t.isActive = :isActive)",
           countQuery = "SELECT COUNT(t) FROM PlacementTest t WHERE " +
           "(:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:isActive IS NULL OR t.isActive = :isActive)")
    Page<PlacementTest> searchTestsPaged(
            @Param("search") String search,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );
}
