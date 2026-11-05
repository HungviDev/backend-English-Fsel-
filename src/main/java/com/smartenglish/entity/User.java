package com.smartenglish.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userId")
    private Long userId;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "passwordHash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "fullName", nullable = false, length = 150)
    private String fullName;

    @Column(name = "avatarUrl", length = 500)
    private String avatarUrl;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "roleId", nullable = false)
    private Role role;

    @Column(name = "currentLevel", length = 20)
    private String currentLevel;

    @Builder.Default
    @Column(name = "isPlacementCompleted", nullable = false)
    private Boolean isPlacementCompleted = false;

    @Builder.Default
    @Column(name = "isActive", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

