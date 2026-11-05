package com.smartenglish.controller;

import com.smartenglish.dto.request.CompleteTaskRequest;
import com.smartenglish.dto.request.GenerateRoadmapRequest;
import com.smartenglish.dto.response.ApiResponse;
import com.smartenglish.dto.response.RoadmapResponse;
import com.smartenglish.dto.response.TaskCompletionResponse;
import com.smartenglish.entity.User;
import com.smartenglish.exception.AppException;
import com.smartenglish.repository.UserRepository;
import com.smartenglish.service.RagRoadmapService;
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

@RestController
@RequestMapping("/api/v1/roadmaps")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Student 2. Menu Học theo Lộ trình AI (Roadmap 7 Days)",
     description = "Giai đoạn 5 - Luồng 1: Học viên học theo lộ trình 7 ngày do AI cá nhân hóa (ai_roadmaps, ai_roadmap_days, ai_roadmap_tasks)")
public class StudentRoadmapController {

    private final RagRoadmapService ragRoadmapService;
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
                        "Giai đoạn 2: Bạn chưa hoàn thành bài kiểm tra đầu vào (isPlacementCompleted = 0). Vui lòng hoàn thành bài test tại /api/v1/placement-tests/active trước khi mở khóa Menu học Lộ trình AI!");
            }
        }
        return user;
    }

    @GetMapping("/current")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Giai đoạn 5 - Luồng 1: Lấy lộ trình 7 ngày đang hoạt động",
               description = "Truy vấn bảng ai_roadmaps (status = 'ACTIVE') -> lấy 7 ngày trong ai_roadmap_days -> hiển thị danh sách nhiệm vụ trong ai_roadmap_tasks")
    public ResponseEntity<ApiResponse<RoadmapResponse>> getCurrentRoadmap(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = validateStudentPlacement(userDetails);
        RoadmapResponse response = ragRoadmapService.getCurrentActiveRoadmap(user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Lấy lộ trình 7 ngày hiện tại thành công", response));
    }

    @PostMapping("/tasks/{taskId}/complete")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Giai đoạn 5 - Luồng 1: Hoàn thành một nhiệm vụ học tập (Task)",
               description = "Cập nhật ai_roadmap_tasks (isCompleted = 1, userScore). Khi tất cả task trong ngày hoàn tất -> ai_roadmap_days.isCompleted = 1 và tự động mở khóa ngày tiếp theo")
    public ResponseEntity<ApiResponse<TaskCompletionResponse>> completeTask(
            @PathVariable Long taskId,
            @RequestBody(required = false) CompleteTaskRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = validateStudentPlacement(userDetails);
        Double score = (request != null && request.getUserScore() != null) ? request.getUserScore() : 100.0;
        TaskCompletionResponse response = ragRoadmapService.completeTask(taskId, score, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Hoàn thành nhiệm vụ thành công", response));
    }

    @GetMapping("/{roadmapId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Xem chi tiết một lộ trình theo roadmapId")
    public ResponseEntity<ApiResponse<RoadmapResponse>> getRoadmapById(@PathVariable Long roadmapId) {
        RoadmapResponse response = ragRoadmapService.getRoadmapById(roadmapId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy thông tin lộ trình thành công", response));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Thử nghiệm kích hoạt RAG + LLM sinh lộ trình thủ công (Debug/Test)",
               description = "Cho phép Admin hoặc học viên kích hoạt lại việc sinh lộ trình với bộ điểm yếu tùy ý")
    public ResponseEntity<ApiResponse<RoadmapResponse>> generateRoadmapManually(
            @Valid @RequestBody GenerateRoadmapRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long currentUserId = null;
        if (userDetails != null) {
            currentUserId = userRepository.findByEmail(userDetails.getUsername())
                    .map(User::getUserId)
                    .orElse(null);
        }

        RoadmapResponse response = ragRoadmapService.generateAndSaveRoadmap(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Sinh lộ trình học 7 ngày thành công", response));
    }
}
