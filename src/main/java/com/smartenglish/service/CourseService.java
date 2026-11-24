package com.smartenglish.service;

import com.smartenglish.dto.request.CreateCourseRequest;
import com.smartenglish.dto.request.CreateLessonExerciseRequest;
import com.smartenglish.dto.request.CreateLessonRequest;
import com.smartenglish.dto.response.CourseResponse;
import com.smartenglish.dto.response.PageResponse;
import com.smartenglish.entity.Course;
import com.smartenglish.entity.Lesson;
import com.smartenglish.entity.LessonExercise;
import com.smartenglish.entity.User;
import com.smartenglish.exception.ResourceNotFoundException;
import com.smartenglish.repository.CourseRepository;
import com.smartenglish.repository.LessonExerciseRepository;
import com.smartenglish.repository.LessonRepository;
import com.smartenglish.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final LessonExerciseRepository exerciseRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getAllCoursesPaged(
            String search,
            String targetLevel,
            Boolean isPublished,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String cleanLevel = (targetLevel != null && !targetLevel.trim().isEmpty()) ? targetLevel.trim().toUpperCase() : null;
        String safeSortBy = (sortBy != null && !sortBy.trim().isEmpty()) ? sortBy.trim() : "orderIndex";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(direction, safeSortBy));

        Page<Course> coursePage = courseRepository.searchCoursesPaged(cleanSearch, cleanLevel, isPublished, pageable);
        List<CourseResponse> responseList = coursePage.getContent().stream()
                .map(this::mapToCourseResponse)
                .collect(Collectors.toList());

        return PageResponse.of(coursePage, responseList);
    }

    public CourseResponse mapToCourseResponse(Course course) {
        if (course == null) return null;

        Long creatorId = null;
        String creatorName = null;
        String creatorEmail = null;

        try {
            if (course.getCreatedBy() != null) {
                creatorId = course.getCreatedBy().getUserId();
                creatorName = course.getCreatedBy().getFullName();
                creatorEmail = course.getCreatedBy().getEmail();
            }
        } catch (Exception ignored) {
            // Lazy loading protection
        }

        int totalLessons = 0;
        try {
            totalLessons = (int) lessonRepository.countByCourse_CourseId(course.getCourseId());
        } catch (Exception ignored) {
        }

        return CourseResponse.builder()
                .courseId(course.getCourseId())
                .title(course.getTitle())
                .description(course.getDescription())
                .thumbnailUrl(course.getThumbnailUrl())
                .targetLevel(course.getTargetLevel())
                .orderIndex(course.getOrderIndex())
                .isPublished(course.getIsPublished())
                .createdById(creatorId)
                .createdByName(creatorName)
                .createdByEmail(creatorEmail)
                .totalLessons(totalLessons)
                .createdAt(course.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Course getCourseById(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "courseId", courseId));
    }

    @Transactional
    public Course createCourse(CreateCourseRequest request, Long currentUserId) {
        User creator = currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null;

        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .thumbnailUrl(request.getThumbnailUrl())
                .targetLevel(request.getTargetLevel())
                .orderIndex(request.getOrderIndex())
                .isPublished(request.getIsPublished())
                .createdBy(creator)
                .build();

        return courseRepository.save(course);
    }

    @Transactional
    public Lesson addLessonToCourse(Long courseId, CreateLessonRequest request) {
        Course course = getCourseById(courseId);

        Lesson lesson = Lesson.builder()
                .course(course)
                .title(request.getTitle())
                .skillType(request.getSkillType().toUpperCase())
                .subSkillTag(request.getSubSkillTag().toUpperCase())
                .contentHtml(request.getContentHtml())
                .audioUrl(request.getAudioUrl())
                .videoUrl(request.getVideoUrl())
                .estimatedMinutes(request.getEstimatedMinutes())
                .orderIndex(request.getOrderIndex())
                .isPublished(request.getIsPublished())
                .build();

        return lessonRepository.save(lesson);
    }

    @Transactional(readOnly = true)
    public List<Lesson> getLessonsByCourse(Long courseId) {
        return lessonRepository.findByCourse_CourseIdAndIsPublishedTrueOrderByOrderIndexAsc(courseId);
    }

    @Transactional
    public LessonExercise addExerciseToLesson(Long lessonId, CreateLessonExerciseRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "lessonId", lessonId));

        LessonExercise exercise = LessonExercise.builder()
                .lesson(lesson)
                .questionText(request.getQuestionText())
                .optionA(request.getOptionA())
                .optionB(request.getOptionB())
                .optionC(request.getOptionC())
                .optionD(request.getOptionD())
                .correctOption(request.getCorrectOption().toUpperCase().trim())
                .explanation(request.getExplanation())
                .build();

        return exerciseRepository.save(exercise);
    }

    @Transactional(readOnly = true)
    public List<LessonExercise> getExercisesByLesson(Long lessonId) {
        return exerciseRepository.findByLesson_LessonId(lessonId);
    }
}
