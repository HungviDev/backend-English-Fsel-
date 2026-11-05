package com.smartenglish.service;

import com.smartenglish.dto.request.CreateCourseRequest;
import com.smartenglish.dto.request.CreateLessonExerciseRequest;
import com.smartenglish.dto.request.CreateLessonRequest;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

