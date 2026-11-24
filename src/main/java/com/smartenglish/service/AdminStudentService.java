package com.smartenglish.service;

import com.smartenglish.dto.request.*;
import com.smartenglish.dto.response.*;
import com.smartenglish.entity.*;
import com.smartenglish.exception.AppException;
import com.smartenglish.exception.ResourceNotFoundException;
import com.smartenglish.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStudentService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserTestAttemptRepository attemptRepository;
    private final UserTestAnswerRepository answerRepository;
    private final AiRoadmapRepository roadmapRepository;
    private final UserLessonProgressRepository progressRepository;
    private final PasswordEncoder passwordEncoder;

    // ==========================================
    // 1. QUẢN LÝ DANH SÁCH & HỒ SƠ HỌC SINH
    // ==========================================

    @Transactional(readOnly = true)
    public List<StudentSummaryResponse> getAllStudents(String search, Boolean placementCompleted, Boolean isActive) {
        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        List<User> students = userRepository.searchStudents(cleanSearch, placementCompleted, isActive);
        return students.stream().map(this::mapToSummaryResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentSummaryResponse> getStudentsPaged(
            String search,
            Boolean placementCompleted,
            Boolean isActive,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String safeSortBy = (sortBy != null && !sortBy.trim().isEmpty()) ? sortBy.trim() : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(direction, safeSortBy));

        Page<User> userPage = userRepository.searchStudentsPaged(cleanSearch, placementCompleted, isActive, pageable);
        List<StudentSummaryResponse> dtoList = userPage.getContent().stream()
                .map(this::mapToSummaryResponse)
                .collect(Collectors.toList());

        return PageResponse.of(userPage, dtoList);
    }

    @Transactional(readOnly = true)
    public StudentDetailResponse getStudentDetail(Long userId) {
        User user = findStudentById(userId);
        // Lấy lịch sử các lần thi đầu vào
        List<UserTestAttempt> attempts = attemptRepository.findByUser_UserIdOrderByStartedAtDesc(userId);
        List<StudentDetailResponse.TestAttemptSummaryDto> attemptDtos = attempts.stream()
                .map(a -> StudentDetailResponse.TestAttemptSummaryDto.builder()
                        .attemptId(a.getAttemptId())
                        .testId(a.getTest() != null ? a.getTest().getTestId() : null)
                        .testTitle(a.getTest() != null ? a.getTest().getTitle() : "Bài kiểm tra đầu vào")
                        .listeningScore(a.getListeningScore())
                        .readingScore(a.getReadingScore())
                        .totalScore(a.getTotalScore())
                        .evaluatedLevel(a.getEvaluatedLevel())
                        .aiOverallFeedback(a.getAiOverallFeedback())
                        .startedAt(a.getStartedAt())
                        .completedAt(a.getCompletedAt())
                        .build())
                .collect(Collectors.toList());

        // Lấy lộ trình 7 ngày đang active (nếu có)
        Optional<AiRoadmap> activeRoadmap = roadmapRepository
                .findFirstByUser_UserIdAndStatusOrderByCreatedAtDesc(userId, "ACTIVE");

        return StudentDetailResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .currentLevel(user.getCurrentLevel())
                .isPlacementCompleted(user.getIsPlacementCompleted())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .activeRoadmapId(activeRoadmap.map(AiRoadmap::getRoadmapId).orElse(null))
                .activeRoadmapTitle(activeRoadmap.map(AiRoadmap::getRoadmapTitle).orElse(null))
                .attempts(attemptDtos)
                .build();
    }

    @Transactional
    public StudentSummaryResponse createStudent(CreateStudentRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Email '" + request.getEmail() + "' đã được sử dụng");
        }

        Role studentRole = roleRepository.findByRoleName("ROLE_STUDENT")
                .orElseThrow(() -> new AppException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Không tìm thấy vai trò ROLE_STUDENT"));

        User student = User.builder()
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .avatarUrl(request.getAvatarUrl())
                .role(studentRole)
                .currentLevel(request.getCurrentLevel() != null ? request.getCurrentLevel().toUpperCase() : "A1")
                .isPlacementCompleted(Boolean.TRUE.equals(request.getIsPlacementCompleted()))
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        User saved = userRepository.save(student);
        log.info("Admin created new student: userId={}, email={}", saved.getUserId(), saved.getEmail());
        return mapToSummaryResponse(saved);
    }

    @Transactional
    public StudentSummaryResponse updateStudent(Long userId, UpdateStudentRequest request) {
        User user = findStudentById(userId);

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getCurrentLevel() != null) {
            user.setCurrentLevel(request.getCurrentLevel().toUpperCase());
        }
        if (request.getIsPlacementCompleted() != null) {
            user.setIsPlacementCompleted(request.getIsPlacementCompleted());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword().trim()));
            log.info("Admin reset password for student userId={}", userId);
        }

        User updated = userRepository.save(user);
        return mapToSummaryResponse(updated);
    }

    @Transactional
    public void updateStudentStatus(Long userId, Boolean isActive) {
        User user = findStudentById(userId);
        user.setIsActive(Boolean.TRUE.equals(isActive));
        userRepository.save(user);
        log.info("Admin updated student isActive status: userId={}, isActive={}", userId, isActive);
    }

    @Transactional
    public void resetStudentPlacement(Long userId) {
        User user = findStudentById(userId);
        user.setIsPlacementCompleted(false);
        userRepository.save(user);
        log.info("Admin reset placement status for student: userId={}", userId);
    }

    @Transactional
    public void deleteStudent(Long userId, boolean permanent) {
        User user = findStudentById(userId);

        if (!permanent) {
            // Xóa mềm: Khóa tài khoản
            user.setIsActive(false);
            userRepository.save(user);
            log.info("Admin soft-deleted (deactivated) student userId={}", userId);
            return;
        }

        // Xóa cứng: Dọn dẹp các bản ghi liên kết trước
        List<UserTestAttempt> attempts = attemptRepository.findByUser_UserIdOrderByStartedAtDesc(userId);
        for (UserTestAttempt attempt : attempts) {
            List<UserTestAnswer> answers = answerRepository.findByAttempt_AttemptId(attempt.getAttemptId());
            answerRepository.deleteAll(answers);
        }
        attemptRepository.deleteAll(attempts);

        List<UserLessonProgress> progresses = progressRepository.findByUser_UserId(userId);
        progressRepository.deleteAll(progresses);

        List<AiRoadmap> roadmaps = roadmapRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
        roadmapRepository.deleteAll(roadmaps);

        userRepository.delete(user);
        log.warn("Admin permanently deleted student userId={} and all related records", userId);
    }

    // ==========================================
    // 2. QUẢN LÝ & CHỈNH SỬA CHI TIẾT BÀI THI
    // ==========================================

    @Transactional(readOnly = true)
    public List<StudentDetailResponse.TestAttemptSummaryDto> getStudentAttempts(Long userId) {
        findStudentById(userId); // kiểm tra tồn tại
        List<UserTestAttempt> attempts = attemptRepository.findByUser_UserIdOrderByStartedAtDesc(userId);

        return attempts.stream().map(a -> StudentDetailResponse.TestAttemptSummaryDto.builder()
                .attemptId(a.getAttemptId())
                .testId(a.getTest() != null ? a.getTest().getTestId() : null)
                .testTitle(a.getTest() != null ? a.getTest().getTitle() : "Đề thi đầu vào")
                .listeningScore(a.getListeningScore())
                .readingScore(a.getReadingScore())
                .totalScore(a.getTotalScore())
                .evaluatedLevel(a.getEvaluatedLevel())
                .aiOverallFeedback(a.getAiOverallFeedback())
                .startedAt(a.getStartedAt())
                .completedAt(a.getCompletedAt())
                .build()).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudentTestDetailResponse getTestAttemptDetail(Long attemptId) {
        UserTestAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("UserTestAttempt", "attemptId", attemptId));

        List<UserTestAnswer> answers = answerRepository.findByAttempt_AttemptId(attemptId);

        List<StudentTestDetailResponse.UserAnswerDetailDto> answerDtos = answers.stream().map(ans -> {
            PlacementQuestion q = ans.getQuestion();
            String skillType = (q.getPassage() != null && q.getPassage().getSkillType() != null)
                    ? q.getPassage().getSkillType().toUpperCase()
                    : "READING";

            return StudentTestDetailResponse.UserAnswerDetailDto.builder()
                    .answerId(ans.getAnswerId())
                    .questionId(q.getQuestionId())
                    .skillType(skillType)
                    .subSkillTag(q.getSubSkillTag())
                    .difficultyLevel(q.getDifficultyLevel())
                    .questionText(q.getQuestionText())
                    .optionA(q.getOptionA())
                    .optionB(q.getOptionB())
                    .optionC(q.getOptionC())
                    .optionD(q.getOptionD())
                    .selectedOption(ans.getSelectedOption())
                    .correctOption(q.getCorrectOption())
                    .isCorrect(ans.getIsCorrect())
                    .explanation(q.getExplanation())
                    .build();
        }).collect(Collectors.toList());

        return StudentTestDetailResponse.builder()
                .attemptId(attempt.getAttemptId())
                .userId(attempt.getUser().getUserId())
                .studentName(attempt.getUser().getFullName())
                .studentEmail(attempt.getUser().getEmail())
                .testId(attempt.getTest() != null ? attempt.getTest().getTestId() : null)
                .testTitle(attempt.getTest() != null ? attempt.getTest().getTitle() : "Đề thi phân loại")
                .listeningScore(attempt.getListeningScore())
                .readingScore(attempt.getReadingScore())
                .totalScore(attempt.getTotalScore())
                .evaluatedLevel(attempt.getEvaluatedLevel())
                .weaknessAnalysisJson(attempt.getWeaknessAnalysisJson())
                .aiOverallFeedback(attempt.getAiOverallFeedback())
                .startedAt(attempt.getStartedAt())
                .completedAt(attempt.getCompletedAt())
                .answers(answerDtos)
                .build();
    }

    @Transactional
    public StudentTestDetailResponse updateTestAttempt(Long attemptId, UpdateTestAttemptRequest request) {
        UserTestAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("UserTestAttempt", "attemptId", attemptId));

        if (request.getListeningScore() != null) {
            attempt.setListeningScore(Math.round(request.getListeningScore() * 10.0) / 10.0);
        }
        if (request.getReadingScore() != null) {
            attempt.setReadingScore(Math.round(request.getReadingScore() * 10.0) / 10.0);
        }
        if (request.getTotalScore() != null) {
            attempt.setTotalScore(Math.round(request.getTotalScore() * 10.0) / 10.0);
        }
        if (request.getEvaluatedLevel() != null && !request.getEvaluatedLevel().trim().isEmpty()) {
            String level = request.getEvaluatedLevel().trim().toUpperCase();
            attempt.setEvaluatedLevel(level);

            // Đồng bộ sang currentLevel của học sinh nếu đây là điểm mới nhất
            User student = attempt.getUser();
            student.setCurrentLevel(level);
            userRepository.save(student);
        }
        if (request.getAiOverallFeedback() != null) {
            attempt.setAiOverallFeedback(request.getAiOverallFeedback());
        }
        if (request.getWeaknessAnalysisJson() != null) {
            attempt.setWeaknessAnalysisJson(request.getWeaknessAnalysisJson());
        }

        attemptRepository.save(attempt);
        log.info("Admin updated test attempt details: attemptId={}", attemptId);

        return getTestAttemptDetail(attemptId);
    }

    @Transactional
    public StudentTestDetailResponse updateTestAnswer(Long attemptId, Long answerId,
            UpdateUserTestAnswerRequest request) {
        UserTestAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("UserTestAttempt", "attemptId", attemptId));

        UserTestAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("UserTestAnswer", "answerId", answerId));

        if (!answer.getAttempt().getAttemptId().equals(attemptId)) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Câu trả lời answerId=" + answerId + " không thuộc bài thi attemptId=" + attemptId);
        }

        if (request.getSelectedOption() != null) {
            String opt = normalizeOption(request.getSelectedOption());
            answer.setSelectedOption(opt);
            // Tự động kiểm tra với correctOption của câu hỏi
            if (request.getIsCorrect() == null) {
                String correctOpt = normalizeOption(answer.getQuestion().getCorrectOption());
                answer.setIsCorrect(opt.equalsIgnoreCase(correctOpt));
            }
        }
        if (request.getIsCorrect() != null) {
            answer.setIsCorrect(request.getIsCorrect());
        }

        answerRepository.save(answer);

        // Tự động tính toán lại điểm số nếu được yêu cầu
        if (Boolean.TRUE.equals(request.getRecalculateScores())) {
            recalculateAttemptScores(attempt);
        }

        log.info("Admin updated answerId={} in attemptId={}: selectedOption={}, isCorrect={}",
                answerId, attemptId, answer.getSelectedOption(), answer.getIsCorrect());

        return getTestAttemptDetail(attemptId);
    }

    private void recalculateAttemptScores(UserTestAttempt attempt) {
        List<UserTestAnswer> allAnswers = answerRepository.findByAttempt_AttemptId(attempt.getAttemptId());
        if (allAnswers.isEmpty())
            return;

        int listeningTotal = 0, listeningCorrect = 0;
        int readingTotal = 0, readingCorrect = 0;

        for (UserTestAnswer a : allAnswers) {
            PlacementQuestion q = a.getQuestion();
            String skill = (q.getPassage() != null && q.getPassage().getSkillType() != null)
                    ? q.getPassage().getSkillType().toUpperCase()
                    : "READING";

            boolean correct = Boolean.TRUE.equals(a.getIsCorrect());
            if ("LISTENING".equalsIgnoreCase(skill)) {
                listeningTotal++;
                if (correct)
                    listeningCorrect++;
            } else {
                readingTotal++;
                if (correct)
                    readingCorrect++;
            }
        }

        double listeningScore = listeningTotal > 0 ? ((double) listeningCorrect / listeningTotal) * 100.0 : 0.0;
        double readingScore = readingTotal > 0 ? ((double) readingCorrect / readingTotal) * 100.0 : 0.0;
        int totalQuestions = listeningTotal + readingTotal;
        int totalCorrect = listeningCorrect + readingCorrect;
        double totalScore = totalQuestions > 0 ? ((double) totalCorrect / totalQuestions) * 100.0 : 0.0;

        attempt.setListeningScore(Math.round(listeningScore * 10.0) / 10.0);
        attempt.setReadingScore(Math.round(readingScore * 10.0) / 10.0);
        attempt.setTotalScore(Math.round(totalScore * 10.0) / 10.0);

        String level;
        if (totalScore >= 90.0)
            level = "C1";
        else if (totalScore >= 75.0)
            level = "B2";
        else if (totalScore >= 60.0)
            level = "B1";
        else if (totalScore >= 45.0)
            level = "A2";
        else
            level = "A1";

        attempt.setEvaluatedLevel(level);
        attemptRepository.save(attempt);

        // Cập nhật currentLevel của user
        User user = attempt.getUser();
        user.setCurrentLevel(level);
        userRepository.save(user);

        log.info("Recalculated scores for attemptId={}: L={}, R={}, Total={}, Level={}",
                attempt.getAttemptId(), attempt.getListeningScore(), attempt.getReadingScore(), attempt.getTotalScore(),
                level);
    }

    private String normalizeOption(String option) {
        if (option == null)
            return "";
        String clean = option.trim().toUpperCase();
        if (clean.startsWith("OPTION")) {
            clean = clean.replace("OPTION", "").replace("_", "").trim();
        }
        return clean;
    }

    private User findStudentById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        if (user.getRole() != null && !"ROLE_STUDENT".equals(user.getRole().getRoleName())) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Tài khoản userId=" + userId + " không phải là học sinh (ROLE_STUDENT)");
        }
        return user;
    }

    private StudentSummaryResponse mapToSummaryResponse(User u) {
        Optional<UserTestAttempt> latestAttempt = attemptRepository
                .findFirstByUser_UserIdOrderByStartedAtDesc(u.getUserId());

        return StudentSummaryResponse.builder()
                .userId(u.getUserId())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .avatarUrl(u.getAvatarUrl())
                .currentLevel(u.getCurrentLevel())
                .isPlacementCompleted(u.getIsPlacementCompleted())
                .isActive(u.getIsActive())
                .createdAt(u.getCreatedAt())
                .totalAttempts(attemptRepository.findByUser_UserIdOrderByStartedAtDesc(u.getUserId()).size())
                .latestTotalScore(latestAttempt.map(UserTestAttempt::getTotalScore).orElse(null))
                .latestEvaluatedLevel(latestAttempt.map(UserTestAttempt::getEvaluatedLevel).orElse(null))
                .latestCompletedAt(latestAttempt.map(UserTestAttempt::getCompletedAt).orElse(null))
                .build();
    }
}
