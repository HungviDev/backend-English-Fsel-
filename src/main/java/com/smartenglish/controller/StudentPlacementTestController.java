package com.smartenglish.controller;

import com.smartenglish.dto.request.BatchSubmitAnswersRequest;
import com.smartenglish.dto.request.SubmitAnswerRequest;
import com.smartenglish.dto.response.ApiResponse;
import com.smartenglish.dto.response.PlacementTestStudentDto;
import com.smartenglish.dto.response.TestAttemptResponse;
import com.smartenglish.dto.response.TestSubmissionResultResponse;
import com.smartenglish.entity.User;
import com.smartenglish.exception.AppException;
import com.smartenglish.repository.UserRepository;
import com.smartenglish.service.PlacementTestService;
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

import java.util.Map;

@RestController
@RequestMapping("/api/v1/placement-tests")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
@Tag(name = "Student 1. Kiểm tra Đầu vào & Kích hoạt Lộ trình AI",
     description = "Giai đoạn 2, 3 & 4: Kiểm tra điều kiện đầu vào, làm bài thi trắc nghiệm Nghe & Đọc, nộp bài kích hoạt RAG AI sinh lộ trình 7 ngày")
public class StudentPlacementTestController {

    private final PlacementTestService placementTestService;
    private final UserRepository userRepository;

    private Long getUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "User authentication required");
        }
        return userRepository.findByEmail(userDetails.getUsername())
                .map(User::getUserId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    // ==========================================
    // GIAI ĐOẠN 2: KIỂM TRA ĐIỀU KIỆN ĐẦU VÀO
    // ==========================================

    @GetMapping("/status")
    @Operation(summary = "Giai đoạn 2: Kiểm tra cờ isPlacementCompleted của học viên",
               description = "Frontend kiểm tra cờ này: nếu false -> chặn menu học tập và điều hướng bắt buộc sang làm test; nếu true -> mở khóa 2 menu học")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkPlacementStatus(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        boolean isCompleted = Boolean.TRUE.equals(user.getIsPlacementCompleted());

        Map<String, Object> data = Map.of(
                "userId", userId,
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "isPlacementCompleted", isCompleted,
                "currentLevel", user.getCurrentLevel() != null ? user.getCurrentLevel() : "NOT_EVALUATED",
                "message", isCompleted
                        ? "Học viên đã hoàn thành test đầu vào. Được phép truy cập Menu Lộ trình AI và Menu Khóa học."
                        : "Học viên chưa làm test đầu vào. Bắt buộc làm bài test tại /api/v1/placement-tests/active trước khi học tập."
        );

        return ResponseEntity.ok(ApiResponse.ok("Kiểm tra điều kiện thành công", data));
    }

    // ==========================================
    // GIAI ĐOẠN 3: LÀM BÀI TEST ĐẦU VÀO
    // ==========================================

    @GetMapping("/active")
    @Operation(summary = "Giai đoạn 3: Lấy nội dung đề thi đầu vào cho học viên",
               description = "Lấy đề thi đang kích hoạt cùng danh sách passages và câu hỏi (đã giấu đáp án đúng để chống gian lận)")
    public ResponseEntity<ApiResponse<PlacementTestStudentDto>> getActiveTest() {
        PlacementTestStudentDto testDto = placementTestService.getActivePlacementTestForStudent();
        return ResponseEntity.ok(ApiResponse.ok("Lấy đề thi thành công", testDto));
    }

    @PostMapping("/{testId}/start")
    @Operation(summary = "Giai đoạn 3: Bắt đầu lượt thi mới",
               description = "Khởi tạo một bản ghi lượt thi trong user_test_attempts gắn với userId và testId")
    public ResponseEntity<ApiResponse<TestAttemptResponse>> startAttempt(
            @PathVariable Long testId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        TestAttemptResponse response = placementTestService.startAttempt(testId, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Bắt đầu bài thi thành công", response));
    }

    @PostMapping("/attempts/{attemptId}/answers")
    @Operation(summary = "Giai đoạn 3: Lưu đáp án một câu hỏi",
               description = "Ghi nhận lựa chọn của học viên vào user_test_answers và tự động so khớp isCorrect")
    public ResponseEntity<ApiResponse<String>> recordSingleAnswer(
            @PathVariable Long attemptId,
            @Valid @RequestBody SubmitAnswerRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        placementTestService.recordAnswer(attemptId, request, userId);
        return ResponseEntity.ok(ApiResponse.ok("Lưu câu trả lời thành công", "ANSWER_SAVED"));
    }

    @PostMapping("/attempts/{attemptId}/batch-answers")
    @Operation(summary = "Giai đoạn 3: Lưu danh sách đáp án nhiều câu hỏi cùng lúc",
               description = "Gửi mảng answers để lưu toàn bộ trước khi nộp bài")
    public ResponseEntity<ApiResponse<String>> recordBatchAnswers(
            @PathVariable Long attemptId,
            @Valid @RequestBody BatchSubmitAnswersRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        placementTestService.batchRecordAnswers(attemptId, request, userId);
        return ResponseEntity.ok(ApiResponse.ok("Lưu danh sách câu trả lời thành công", "BATCH_ANSWERS_SAVED"));
    }

    // ==========================================
    // GIAI ĐOẠN 4: NỘP BÀI & KÍCH HOẠT AI RAG
    // ==========================================

    @PostMapping("/attempts/{attemptId}/submit")
    @Operation(summary = "Giai đoạn 4: Nộp bài test đầu vào -> Tính điểm, phân tích điểm yếu & RAG AI sinh lộ trình 7 ngày",
               description = "Chấm điểm Nghe/Đọc, gom nhóm lỗi sai theo subSkillTag lưu weaknessAnalysisJson, gọi LLM sinh 7 ngày học lưu vào 3 bảng ai_roadmaps, ai_roadmap_days, ai_roadmap_tasks và đổi isPlacementCompleted = true")
    public ResponseEntity<ApiResponse<TestSubmissionResultResponse>> submitAttempt(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        TestSubmissionResultResponse response = placementTestService.submitAttempt(attemptId, userId);
        return ResponseEntity.ok(ApiResponse.ok("Nộp bài thi và sinh lộ trình AI 7 ngày thành công!", response));
    }
}

