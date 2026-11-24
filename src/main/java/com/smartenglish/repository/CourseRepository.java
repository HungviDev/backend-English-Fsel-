package com.smartenglish.repository;

import com.smartenglish.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByIsPublishedTrueOrderByOrderIndexAsc();

    List<Course> findByTargetLevel(String targetLevel);

    @Query(value = "SELECT c FROM Course c WHERE " +
           "(:search IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:targetLevel IS NULL OR c.targetLevel = :targetLevel) " +
           "AND (:isPublished IS NULL OR c.isPublished = :isPublished)",
           countQuery = "SELECT COUNT(c) FROM Course c WHERE " +
           "(:search IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:targetLevel IS NULL OR c.targetLevel = :targetLevel) " +
           "AND (:isPublished IS NULL OR c.isPublished = :isPublished)")
    Page<Course> searchCoursesPaged(
            @Param("search") String search,
            @Param("targetLevel") String targetLevel,
            @Param("isPublished") Boolean isPublished,
            Pageable pageable
    );
}
