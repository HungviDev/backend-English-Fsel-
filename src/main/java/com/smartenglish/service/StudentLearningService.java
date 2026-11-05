package com.smartenglish.service;

import com.smartenglish.dto.request.SubmitLessonExercisesRequest;
import com.smartenglish.dto.response.LessonExerciseResultResponse;
import com.smartenglish.dto.response.LessonExerciseStudentDto;
import com.smartenglish.dto.response.UserLessonProgressResponse;
import com.smartenglish.entity.Course;
import com.smartenglish.entity.Lesson;
import com.smartenglish.entity.LessonExercise;
import com.smartenglish.entity.User;
import com.smartenglish.entity.UserLessonProgress;
import com.smartenglish.exception.AppException;
import com.smartenglish.exception.ResourceNotFoundException;
import com.smartenglish.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentLearningService {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final LessonExerciseRepository exerciseRepository;
    private final UserLessonProgressRepository progressRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Course> getAvailableCourses(String targetLevel) {
        if (StringUtils.hasText(targetLevel)) {
            return courseRepository.findByTargetLevel(targetLevel.toUpperCase());
        }
        return courseRepository.findByIsPublishedTrueOrderByOrderIndexAsc();
    }

    @Transactional(readOnly = true)
    public Course getCourseDetail(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "courseId", courseId));
    }

    @Transactional(readOnly = true)
    public List<Lesson> getLessonsOfCourse(Long courseId) {
        return lessonRepository.findByCourse_CourseIdAndIsPublishedTrueOrderByOrderIndexAsc(courseId);
    }

    /**
     * Khi học viên mở một bài học:
     * - Trả về chi tiết bài giảng
     * - Tự động ghi nhận/cập nhật user_lesson_progress thành IN_PROGRESS và cập
     * nhật lastAccessedAt
     */
    @Transactional
    public Lesson openLesson(Long lessonId, Long userId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "lessonId", lessonId));

        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                UserLessonProgress progress = progressRepository.findByUser_UserIdAndLesson_LessonId(userId, lessonId)
                        .orElseGet(() -> UserLessonProgress.builder()
                                .user(user)
                                .lesson(lesson)
                                .status("IN_PROGRESS")
                                .build());

                progress.setLastAccessedAt(LocalDateTime.now());
                if ("NOT_STARTED".equals(progress.getStatus())) {
                    progress.setStatus("IN_PROGRESS");
                }
                progressRepository.save(progress);
            }
        }

        return lesson;
    }

    @Transactional(readOnly = true)
    public List<LessonExerciseStudentDto> getLessonExercisesForStudent(Long lessonId) {
        List<LessonExercise> exercises = exerciseRepository.findByLesson_LessonId(lessonId);

        return exercises.stream().map(e -> LessonExerciseStudentDto.builder()
                .exerciseId(e.getExerciseId())
                .lessonId(lessonId)
                .questionText(e.getQuestionText())
                .optionA(e.getOptionA())
                .optionB(e.getOptionB())
                .optionC(e.getOptionC())
                .optionD(e.getOptionD())
                .build()).collect(Collectors.toList());
    }

    /**
     * Chấm điểm bài tập ôn tập của bài giảng và lưu vào user_lesson_progress
     */
    @Transactional
    public LessonExerciseResultResponse submitLessonExercises(Long lessonId,
            SubmitLessonExercisesRequest request,
            Long userId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "lessonId", lessonId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        List<LessonExercise> exercises = exerciseRepository.findByLesson_LessonId(lessonId);
        if (exercises.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Bài học này chưa có câu hỏi luyện tập nào");
        }

        Map<Long, LessonExercise> exerciseMap = exercises.stream()
                .collect(Collectors.toMap(LessonExercise::getExerciseId, e -> e));

        int total = request.getAnswers().size();
        int correctCount = 0;
        List<LessonExerciseResultResponse.ExerciseFeedbackItem> feedbackList = new ArrayList<>();

        for (SubmitLessonExercisesRequest.ExerciseAnswerItem item : request.getAnswers()) {
            LessonExercise ex = exerciseMap.get(item.getExerciseId());
            if (ex != null) {
                String selected = normalizeOption(item.getSelectedOption());
                String correct = normalizeOption(ex.getCorrectOption());
                boolean isCorrect = selected.equalsIgnoreCase(correct);
                if (isCorrect) {
                    correctCount++;
                }
                feedbackList.add(LessonExerciseResultResponse.ExerciseFeedbackItem.builder()
                        .exerciseId(ex.getExerciseId())
                        .questionText(ex.getQuestionText())
                        .selectedOption(selected)
                        .correctOption(correct)
                        .isCorrect(isCorrect)
                        .explanation(ex.getExplanation())
                        .build());
            }
        }

        double score = total > 0 ? ((double) correctCount / total) * 100.0 : 0.0;
        score = Math.round(score * 10.0) / 10.0;

        // Cập nhật UserLessonProgress
        UserLessonProgress progress = progressRepository.findByUser_UserIdAndLesson_LessonId(userId, lessonId)
                .orElseGet(() -> UserLessonProgress.builder()
                        .user(user)
                        .lesson(lesson)
                        .build());

        progress.setPracticeScore(score);
        progress.setStatus("COMPLETED");
        progress.setCompletedAt(LocalDateTime.now());
        progress.setLastAccessedAt(LocalDateTime.now());
        progressRepository.save(progress);

        log.info("User {} completed exercises for Lesson {}. Score: {}%", userId, lessonId, score);

        return LessonExerciseResultResponse.builder()
                .lessonId(lessonId)
                .totalQuestions(total)
                .correctCount(correctCount)
                .practiceScore(score)
                .status("COMPLETED")
                .feedback(feedbackList)
                .build();
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

    @Transactional(readOnly = true)
    public UserLessonProgressResponse getLessonProgress(Long lessonId, Long userId) {
        UserLessonProgress progress = progressRepository.findByUser_UserIdAndLesson_LessonId(userId, lessonId)
                .orElse(null);

        if (progress == null) {
            return UserLessonProgressResponse.builder()
                    .userId(userId)
                    .lessonId(lessonId)
                    .status("NOT_STARTED")
                    .practiceScore(null)
                    .build();
        }

        return UserLessonProgressResponse.builder()
                .progressId(progress.getProgressId())
                .userId(userId)
                .lessonId(lessonId)
                .lessonTitle(progress.getLesson() != null ? progress.getLesson().getTitle() : null)
                .status(progress.getStatus())
                .practiceScore(progress.getPracticeScore())
                .lastAccessedAt(progress.getLastAccessedAt())
                .completedAt(progress.getCompletedAt())
                .build();
    }
}
