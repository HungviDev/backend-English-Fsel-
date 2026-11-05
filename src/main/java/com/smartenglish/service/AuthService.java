package com.smartenglish.service;

import com.smartenglish.config.JwtTokenProvider;
import com.smartenglish.dto.request.LoginRequest;
import com.smartenglish.dto.request.RegisterRequest;
import com.smartenglish.dto.response.AuthResponse;
import com.smartenglish.entity.Role;
import com.smartenglish.entity.User;
import com.smartenglish.exception.AppException;
import com.smartenglish.repository.RoleRepository;
import com.smartenglish.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Email is already registered: " + request.getEmail());
        }

        Role studentRole = roleRepository.findByRoleName("ROLE_STUDENT")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .roleName("ROLE_STUDENT")
                        .description("Default Student Role")
                        .build()));

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(studentRole)
                .isPlacementCompleted(false)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(savedUser);

        log.info("Registered new user with email: {}", savedUser.getEmail());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(savedUser.getUserId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(studentRole.getRoleName())
                .isPlacementCompleted(savedUser.getIsPlacementCompleted())
                .currentLevel(savedUser.getCurrentLevel())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase().trim(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AppException(HttpStatus.FORBIDDEN, "Account has been deactivated");
        }

        String token = jwtTokenProvider.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole() != null ? user.getRole().getRoleName() : "ROLE_STUDENT")
                .isPlacementCompleted(user.getIsPlacementCompleted())
                .currentLevel(user.getCurrentLevel())
                .build();
    }
}

