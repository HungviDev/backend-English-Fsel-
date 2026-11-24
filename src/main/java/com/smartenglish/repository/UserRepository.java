package com.smartenglish.repository;

import com.smartenglish.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole_RoleNameOrderByCreatedAtDesc(String roleName);

    @Query(value = "SELECT u FROM User u WHERE u.role.roleName = 'ROLE_STUDENT' " +
           "AND (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:placementCompleted IS NULL OR u.isPlacementCompleted = :placementCompleted) " +
           "AND (:isActive IS NULL OR u.isActive = :isActive)",
           countQuery = "SELECT COUNT(u) FROM User u WHERE u.role.roleName = 'ROLE_STUDENT' " +
           "AND (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:placementCompleted IS NULL OR u.isPlacementCompleted = :placementCompleted) " +
           "AND (:isActive IS NULL OR u.isActive = :isActive)")
    Page<User> searchStudentsPaged(
            @Param("search") String search,
            @Param("placementCompleted") Boolean placementCompleted,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );

    @Query("SELECT u FROM User u WHERE u.role.roleName = 'ROLE_STUDENT' " +
           "AND (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:placementCompleted IS NULL OR u.isPlacementCompleted = :placementCompleted) " +
           "AND (:isActive IS NULL OR u.isActive = :isActive) " +
           "ORDER BY u.createdAt DESC")
    List<User> searchStudents(
            @Param("search") String search,
            @Param("placementCompleted") Boolean placementCompleted,
            @Param("isActive") Boolean isActive
    );
}
