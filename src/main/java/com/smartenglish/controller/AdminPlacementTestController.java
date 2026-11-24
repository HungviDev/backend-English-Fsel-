package com.smartenglish.controller;

import com.smartenglish.dto.request.CreatePlacementQuestionRequest;
import com.smartenglish.dto.request.CreatePlacementTestRequest;
import com.smartenglish.dto.request.CreateQuestionPassageRequest;
import com.smartenglish.dto.response.ApiResponse;
import com.smartenglish.dto.response.PageResponse;
import com.smartenglish.dto.response.PlacementTestResponse;
import com.smartenglish.entity.PlacementQuestion;
import com.smartenglish.entity.PlacementTest;
import com.smartenglish.entity.QuestionPassage;
import com.smartenglish.entity.User;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/placement-tests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin 1. Quản lý Đề thi Đầu vào (Placement Tests)",
     description = "Giai đoạn 1: Admin tạo đề thi, thêm đoạn văn đọc / audio nghe và tạo câu hỏi trắc nghiệm gắn subSkillTag")
public class AdminPlacementTestController {

    private final PlacementTestService placementTestService;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "Tạo đề thi đầu vào mới",
               description = "Khởi tạo thông tin chung của đề kiểm tra (thời gian làm bài, tổng số câu)")
    public ResponseEntity<ApiResponse<PlacementTestResponse>> createPlacementTest(
            @Valid @RequestBody CreatePlacementTestRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long adminId = userDetails != null ? userRepository.findByEmail(userDetails.getUsername())
                .map(User::getUserId).orElse(null) : null;

        PlacementTest test = placementTestService.createPlacementTest(request, adminId);
        PlacementTestResponse response = placementTestService.mapToTestResponse(test);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo đề thi đầu vào thành công", response));
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả đề thi có phân trang & tìm kiếm (Admin)",
               description = "Hỗ trợ phân trang (page, size, sortBy, sortDir), tìm kiếm theo tiêu đề/mô tả (search) và lọc theo trạng thái kích hoạt (isActive)")
    public ResponseEntity<ApiResponse<PageResponse<PlacementTestResponse>>> getAllTests(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        PageResponse<PlacementTestResponse> tests = placementTestService.getAllTestsPaged(
                search, isActive, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách đề thi thành công", tests));
    }

    @GetMapping("/{testId}")
    @Operation(summary = "Lấy chi tiết đề thi theo testId")
    public ResponseEntity<ApiResponse<PlacementTestResponse>> getTestDetail(@PathVariable Long testId) {
        PlacementTestResponse test = placementTestService.getTestDetailResponse(testId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy chi tiết đề thi thành công", test));
    }

    @PostMapping("/{testId}/passages")
    @Operation(summary = "Thêm đoạn văn đọc hiểu hoặc audio nghe (Passage) vào đề thi",
               description = "Gắn passageId vào testId, phân loại rõ skillType (READING hoặc LISTENING)")
    public ResponseEntity<ApiResponse<QuestionPassage>> addPassage(
            @PathVariable Long testId,
            @Valid @RequestBody CreateQuestionPassageRequest request) {

        QuestionPassage passage = placementTestService.addPassage(testId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Thêm Passage vào đề thi thành công", passage));
    }

    @GetMapping("/{testId}/passages")
    @Operation(summary = "Xem danh sách các Passage thuộc đề thi")
    public ResponseEntity<ApiResponse<List<QuestionPassage>>> getPassagesByTest(@PathVariable Long testId) {
        List<QuestionPassage> passages = placementTestService.getPassagesByTest(testId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách Passage thành công", passages));
    }

    @PostMapping("/passages/{passageId}/questions")
    @Operation(summary = "Thêm câu hỏi trắc nghiệm vào Passage",
               description = "Tạo câu hỏi gắn với passageId và nhãn subSkillTag (ví dụ: LISTENING_DETAIL, READING_VOCAB)")
    public ResponseEntity<ApiResponse<PlacementQuestion>> addQuestionToPassage(
            @PathVariable Long passageId,
            @Valid @RequestBody CreatePlacementQuestionRequest request) {

        PlacementQuestion question = placementTestService.addQuestionToPassage(passageId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Thêm câu hỏi trắc nghiệm thành công", question));
    }

    @GetMapping("/passages/{passageId}/questions")
    @Operation(summary = "Xem danh sách câu hỏi trắc nghiệm thuộc Passage")
    public ResponseEntity<ApiResponse<List<PlacementQuestion>>> getQuestionsByPassage(@PathVariable Long passageId) {
        List<PlacementQuestion> questions = placementTestService.getQuestionsByPassage(passageId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách câu hỏi thành công", questions));
    }
}
