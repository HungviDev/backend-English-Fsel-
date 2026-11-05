package com.smartenglish.controller;

import com.smartenglish.dto.request.CreateCourseRequest;
import com.smartenglish.dto.request.CreateLessonExerciseRequest;
import com.smartenglish.dto.request.CreateLessonRequest;
import com.smartenglish.dto.response.ApiResponse;
import com.smartenglish.entity.Course;
import com.smartenglish.entity.Lesson;
import com.smartenglish.entity.LessonExercise;
import com.smartenglish.entity.User;
import com.smartenglish.repository.UserRepository;
import com.smartenglish.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/courses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "2. Admin Courses & Lessons", description = "Quản lý khóa học và bài giảng - Dành riêng cho ROLE_ADMIN")
public class AdminCourseController {

    private final CourseService courseService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy danh sách tất cả khóa học (Admin)", description = "Chỉ tài khoản ROLE_ADMIN mới có quyền gọi API này")
    public ResponseEntity<ApiResponse<List<Course>>> getAllCourses() {
        List<Course> courses = courseService.getAllCourses();
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách khóa học thành công", courses));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo khóa học mới (Admin)", description = "Tạo khung khóa học với cấp độ mục tiêu (A1-C2)")
    public ResponseEntity<ApiResponse<Course>> createCourse(@Valid @RequestBody CreateCourseRequest request,
                                                            @AuthenticationPrincipal UserDetails userDetails) {
        Long adminId = null;
        if (userDetails != null) {
            adminId = userRepository.findByEmail(userDetails.getUsername())
                    .map(User::getUserId)
                    .orElse(null);
        }

        Course course = courseService.createCourse(request, adminId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo khóa học thành công", course));
    }

    @PostMapping("/{courseId}/lessons")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thêm bài giảng vào khóa học (Admin)", description = "Thêm bài học kèm subSkillTag làm cơ sở dữ liệu tri thức cho RAG Engine")
    public ResponseEntity<ApiResponse<Lesson>> addLessonToCourse(@PathVariable Long courseId,
                                                                 @Valid @RequestBody CreateLessonRequest request) {
        Lesson lesson = courseService.addLessonToCourse(courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Thêm bài giảng vào khóa học thành công", lesson));
    }

    @GetMapping("/{courseId}/lessons")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xem danh sách bài giảng theo khóa học (Admin)")
    public ResponseEntity<ApiResponse<List<Lesson>>> getLessonsByCourse(@PathVariable Long courseId) {
        List<Lesson> lessons = courseService.getLessonsByCourse(courseId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách bài giảng thành công", lessons));
    }

    @PostMapping("/lessons/{lessonId}/exercises")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thêm câu hỏi luyện tập vào bài giảng (Admin)",
               description = "Giai đoạn 1: Admin thêm bài tập trắc nghiệm gắn với lessonId")
    public ResponseEntity<ApiResponse<LessonExercise>> addExerciseToLesson(
            @PathVariable Long lessonId,
            @Valid @RequestBody CreateLessonExerciseRequest request) {
        LessonExercise exercise = courseService.addExerciseToLesson(lessonId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Thêm câu hỏi luyện tập vào bài giảng thành công", exercise));
    }

    @GetMapping("/lessons/{lessonId}/exercises")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xem danh sách câu hỏi luyện tập của bài giảng (Admin)")
    public ResponseEntity<ApiResponse<List<LessonExercise>>> getExercisesByLesson(@PathVariable Long lessonId) {
        List<LessonExercise> exercises = courseService.getExercisesByLesson(lessonId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách bài tập thành công", exercises));
    }
}

