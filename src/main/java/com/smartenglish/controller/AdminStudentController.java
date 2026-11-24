package com.smartenglish.controller;

import com.smartenglish.dto.request.*;
import com.smartenglish.dto.response.*;
import com.smartenglish.service.AdminStudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/students")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin 3. Quản lý Học sinh & Bài thi (Student Management)", description = "Dành riêng cho Quản trị viên (ROLE_ADMIN): Thêm/sửa/xóa học sinh, theo dõi trạng thái làm bài, xem chi tiết và can thiệp chỉnh sửa kết quả bài thi của học sinh")
public class AdminStudentController {

    private final AdminStudentService adminStudentService;

    // ==========================================
    // 1. QUẢN LÝ TÀI KHOẢN & HỒ SƠ HỌC SINH
    // ==========================================

    @GetMapping
    @Operation(summary = "Lấy danh sách học sinh có phân trang (Tìm kiếm & Lọc trạng thái)", description = "Hỗ trợ phân trang (page, size, sortBy, sortDir), tìm kiếm theo tên/email, lọc theo trạng thái đã làm bài thi đầu vào hay chưa (placementCompleted) hoặc trạng thái kích hoạt (isActive)")
    public ResponseEntity<ApiResponse<PageResponse<StudentSummaryResponse>>> getAllStudents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean placementCompleted,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        PageResponse<StudentSummaryResponse> response = adminStudentService.getStudentsPaged(
                search, placementCompleted, isActive, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách học sinh phân trang thành công", response));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Xem chi tiết hồ sơ học sinh", description = "Xem thông tin chi tiết cá nhân, trạng thái bài test, lộ trình AI 7 ngày đang học và lịch sử các lần thi đầu vào")
    public ResponseEntity<ApiResponse<StudentDetailResponse>> getStudentDetail(@PathVariable Long userId) {
        StudentDetailResponse detail = adminStudentService.getStudentDetail(userId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy chi tiết hồ sơ học sinh thành công", detail));
    }

    @PostMapping
    @Operation(summary = "Tạo tài khoản học sinh mới thủ công", description = "Admin khởi tạo tài khoản học sinh với email, mật khẩu, họ tên và trình độ mặc định")
    public ResponseEntity<ApiResponse<StudentSummaryResponse>> createStudent(
            @Valid @RequestBody CreateStudentRequest request) {

        StudentSummaryResponse created = adminStudentService.createStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo tài khoản học sinh mới thành công", created));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Cập nhật thông tin học sinh", description = "Chỉnh sửa họ tên, cấp độ CEFR, avatar, mật khẩu hoặc cấu hình trạng thái của học sinh")
    public ResponseEntity<ApiResponse<StudentSummaryResponse>> updateStudent(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateStudentRequest request) {

        StudentSummaryResponse updated = adminStudentService.updateStudent(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thông tin học sinh thành công", updated));
    }

    @PatchMapping("/{userId}/status")
    @Operation(summary = "Kích hoạt hoặc khóa tài khoản học sinh", description = "Chuyển đổi trạng thái isActive (true: Hoạt động, false: Khóa tài khoản)")
    public ResponseEntity<ApiResponse<Void>> updateStudentStatus(
            @PathVariable Long userId,
            @RequestParam Boolean isActive) {

        adminStudentService.updateStudentStatus(userId, isActive);
        String msg = Boolean.TRUE.equals(isActive) ? "Kích hoạt tài khoản học sinh thành công"
                : "Khóa tài khoản học sinh thành công";
        return ResponseEntity.ok(ApiResponse.ok(msg, null));
    }

    @PostMapping("/{userId}/reset-placement")
    @Operation(summary = "Reset trạng thái bài thi đầu vào", description = "Đặt lại isPlacementCompleted = false để học sinh có thể làm lại bài kiểm tra phân loại trình độ từ đầu")
    public ResponseEntity<ApiResponse<Void>> resetStudentPlacement(@PathVariable Long userId) {
        adminStudentService.resetStudentPlacement(userId);
        return ResponseEntity.ok(
                ApiResponse.ok("Reset trạng thái bài thi đầu vào thành công. Học sinh có thể làm lại bài test!", null));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Xóa học sinh (Mặc định xóa mềm/Khóa tài khoản)", description = "permanent=false: Khóa tài khoản (khuyên dùng). permanent=true: Xóa vĩnh viễn học sinh và toàn bộ lịch sử thi, lộ trình")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "false") boolean permanent) {

        adminStudentService.deleteStudent(userId, permanent);
        String msg = permanent ? "Đã xóa vĩnh viễn học sinh và dữ liệu liên quan"
                : "Đã khóa (xóa mềm) tài khoản học sinh";
        return ResponseEntity.ok(ApiResponse.ok(msg, null));
    }

    // ==========================================
    // 2. THEO DÕI & CHỈNH SỬA CHI TIẾT BÀI THI
    // ==========================================

    @GetMapping("/{userId}/test-attempts")
    @Operation(summary = "Xem danh sách các lượt thi đầu vào của học sinh", description = "Lấy lịch sử tất cả các lần học sinh thực hiện bài test phân loại")
    public ResponseEntity<ApiResponse<List<StudentDetailResponse.TestAttemptSummaryDto>>> getStudentAttempts(
            @PathVariable Long userId) {

        List<StudentDetailResponse.TestAttemptSummaryDto> attempts = adminStudentService.getStudentAttempts(userId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách lượt thi của học sinh thành công", attempts));
    }

    @GetMapping("/test-attempts/{attemptId}")
    @Operation(summary = "Xem chi tiết bài thi của học sinh (Kèm đáp án từng câu hỏi)", description = "Xem chi tiết điểm số, trình độ đánh giá, nhận xét AI và toàn bộ các câu hỏi kèm đáp án học sinh đã chọn, đáp án đúng và giải thích")
    public ResponseEntity<ApiResponse<StudentTestDetailResponse>> getTestAttemptDetail(
            @PathVariable Long attemptId) {

        StudentTestDetailResponse detail = adminStudentService.getTestAttemptDetail(attemptId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy chi tiết bài thi thành công", detail));
    }

    @PutMapping("/test-attempts/{attemptId}")
    @Operation(summary = "Chỉnh sửa kết quả bài thi của học sinh", description = "Admin có thể can thiệp điều chỉnh điểm Nghe, điểm Đọc, Tổng điểm, Xếp loại trình độ CEFR (A1-C2) và lời nhận xét đánh giá")
    public ResponseEntity<ApiResponse<StudentTestDetailResponse>> updateTestAttempt(
            @PathVariable Long attemptId,
            @RequestBody UpdateTestAttemptRequest request) {

        StudentTestDetailResponse updated = adminStudentService.updateTestAttempt(attemptId, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật kết quả bài thi thành công", updated));
    }

    @PutMapping("/test-attempts/{attemptId}/answers/{answerId}")
    @Operation(summary = "Chỉnh sửa chi tiết một câu trả lời trong bài thi", description = "Admin có thể sửa đáp án học sinh đã chọn, chấm lại đúng/sai cho câu hỏi đó. Hệ thống sẽ tự động tính lại tổng điểm và cập nhật trình độ nếu recalculateScores=true")
    public ResponseEntity<ApiResponse<StudentTestDetailResponse>> updateTestAnswer(
            @PathVariable Long attemptId,
            @PathVariable Long answerId,
            @RequestBody UpdateUserTestAnswerRequest request) {

        StudentTestDetailResponse updated = adminStudentService.updateTestAnswer(attemptId, answerId, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật câu trả lời trong bài thi thành công", updated));
    }

}


