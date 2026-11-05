package com.smartenglish.controller;

import com.smartenglish.dto.request.SubmitLessonExercisesRequest;
import com.smartenglish.dto.response.*;
import com.smartenglish.entity.Course;
import com.smartenglish.entity.Lesson;
import com.smartenglish.entity.User;
import com.smartenglish.exception.AppException;
import com.smartenglish.repository.UserRepository;
import com.smartenglish.service.StudentLearningService;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
@Tag(name = "Student 3. Menu Học theo Admin Cấu hình (Courses & Lessons)",
     description = "Giai đoạn 5 - Luồng 2: Học viên duyệt khóa học (courses), bài học (lessons), làm bài tập (lesson_exercises) và lưu tiến độ (user_lesson_progress)")
public class StudentCourseController {

    private final StudentLearningService studentLearningService;
    private final UserRepository userRepository;

    private User validateStudentPlacement(UserDetails userDetails) {
        if (userDetails == null) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "User authentication required");
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "User not found"));

        // Giai đoạn 2 Check: Chặn học viên chưa làm bài test đầu vào
        if (user.getRole() != null && "ROLE_STUDENT".equals(user.getRole().getRoleName())) {
            if (!Boolean.TRUE.equals(user.getIsPlacementCompleted())) {
                throw new AppException(HttpStatus.FORBIDDEN,
                        "Giai đoạn 2: Bạn chưa hoàn thành bài kiểm tra đầu vào (isPlacementCompleted = 0). Vui lòng hoàn thành bài test tại /api/v1/placement-tests/active trước khi mở khóa Menu Khóa học!");
            }
        }
        return user;
    }

    // ==========================================
    // KHÓA HỌC (COURSES)
    // ==========================================

    @GetMapping("/courses")
    @Operation(summary = "Giai đoạn 5 - Luồng 2: Duyệt danh sách khóa học tiêu chuẩn (A1 - C1)",
               description = "Lấy danh sách khóa học do Admin tạo, có thể lọc theo targetLevel (ví dụ: ?targetLevel=B1)")
    public ResponseEntity<ApiResponse<List<Course>>> getCourses(
            @RequestParam(required = false) String targetLevel,
            @AuthenticationPrincipal UserDetails userDetails) {
        validateStudentPlacement(userDetails);
        List<Course> courses = studentLearningService.getAvailableCourses(targetLevel);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách khóa học thành công", courses));
    }

    @GetMapping("/courses/{courseId}")
    @Operation(summary = "Giai đoạn 5 - Luồng 2: Xem chi tiết khóa học và danh sách bài học")
    public ResponseEntity<ApiResponse<Course>> getCourseDetail(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        validateStudentPlacement(userDetails);
        Course course = studentLearningService.getCourseDetail(courseId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy chi tiết khóa học thành công", course));
    }

    @GetMapping("/courses/{courseId}/lessons")
    @Operation(summary = "Giai đoạn 5 - Luồng 2: Lấy danh sách bài giảng thuộc khóa học")
    public ResponseEntity<ApiResponse<List<Lesson>>> getLessonsByCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        validateStudentPlacement(userDetails);
        List<Lesson> lessons = studentLearningService.getLessonsOfCourse(courseId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách bài giảng thành công", lessons));
    }

    // ==========================================
    // BÀI HỌC & TIẾN ĐỘ (LESSONS & USER_LESSON_PROGRESS)
    // ==========================================

    @GetMapping("/lessons/{lessonId}")
    @Operation(summary = "Giai đoạn 5 - Luồng 2: Mở bài học (Lesson) & Tự động ghi nhận tiến độ",
               description = "Học viên mở bài học -> hệ thống trả về nội dung và tự động tạo/cập nhật user_lesson_progress thành IN_PROGRESS")
    public ResponseEntity<ApiResponse<Lesson>> openLesson(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = validateStudentPlacement(userDetails);
        Lesson lesson = studentLearningService.openLesson(lessonId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Mở bài học thành công", lesson));
    }

    @GetMapping("/lessons/{lessonId}/exercises")
    @Operation(summary = "Giai đoạn 5 - Luồng 2: Lấy bộ câu hỏi luyện tập của bài học (lesson_exercises)",
               description = "Lấy các bài tập trắc nghiệm đi kèm với lessonId (đã giấu đáp án đúng để học viên làm bài)")
    public ResponseEntity<ApiResponse<List<LessonExerciseStudentDto>>> getLessonExercises(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal UserDetails userDetails) {
        validateStudentPlacement(userDetails);
        List<LessonExerciseStudentDto> exercises = studentLearningService.getLessonExercisesForStudent(lessonId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy bài tập ôn tập thành công", exercises));
    }

    @PostMapping("/lessons/{lessonId}/exercises/submit")
    @Operation(summary = "Giai đoạn 5 - Luồng 2: Nộp bài tập ôn tập bài học & Cập nhật user_lesson_progress",
               description = "Chấm điểm bộ câu hỏi luyện tập, lưu điểm practiceScore và cập nhật trạng thái status = COMPLETED trong user_lesson_progress")
    public ResponseEntity<ApiResponse<LessonExerciseResultResponse>> submitLessonExercises(
            @PathVariable Long lessonId,
            @Valid @RequestBody SubmitLessonExercisesRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = validateStudentPlacement(userDetails);
        LessonExerciseResultResponse result = studentLearningService.submitLessonExercises(lessonId, request, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Nộp bài tập và cập nhật tiến độ thành công", result));
    }

    @GetMapping("/lessons/{lessonId}/progress")
    @Operation(summary = "Giai đoạn 5 - Luồng 2: Xem tiến độ học tập bài giảng (user_lesson_progress)",
               description = "Lấy trạng thái (NOT_STARTED, IN_PROGRESS, COMPLETED) và điểm bài tập practiceScore của học viên đối với bài học này")
    public ResponseEntity<ApiResponse<UserLessonProgressResponse>> getLessonProgress(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = validateStudentPlacement(userDetails);
        UserLessonProgressResponse progress = studentLearningService.getLessonProgress(lessonId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Lấy tiến độ bài học thành công", progress));
    }
}

