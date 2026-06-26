package com.quiz.exam.controller;

import com.quiz.exam.dto.AttachQuestionRequest;
import com.quiz.exam.dto.AttachQuestionsBulkRequest;
import com.quiz.exam.dto.BaseResponse;
import com.quiz.exam.dto.CreateExamRequest;
import com.quiz.exam.dto.ExamQuestionResponse;
import com.quiz.exam.dto.ExamResponse;
import com.quiz.exam.dto.ExamStartResponse;
import com.quiz.exam.dto.UpdateExamRequest;
import com.quiz.exam.service.ExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exams")
@Tag(name = "Exam", description = "Exam management endpoints")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @PostMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @Operation(summary = "Create exam (default DRAFT; set status=PUBLISHED to notify immediately)")
    @ApiResponse(responseCode = "201", description = "Exam created")
    public ResponseEntity<BaseResponse<ExamResponse>> createExam(
            @Valid @RequestBody CreateExamRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String createdBy = jwt.getClaimAsString("userId");
        ExamResponse data = examService.createExam(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.of(HttpStatus.CREATED.value(), "Exam created", data));
    }

    @PostMapping("/{examId}/publish")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @Operation(summary = "Publish draft exam (notifies students)")
    @ApiResponse(responseCode = "200", description = "Exam published")
    public ResponseEntity<BaseResponse<ExamResponse>> publishExam(
            @PathVariable String examId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String instructorId = jwt.getClaimAsString("userId");
        ExamResponse data = examService.publishExam(examId, instructorId);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Exam published", data));
    }

    @PatchMapping("/{examId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @Operation(summary = "Update exam metadata (owner only)")
    @ApiResponse(responseCode = "200", description = "Exam updated")
    public ResponseEntity<BaseResponse<ExamResponse>> updateExam(
            @PathVariable String examId,
            @Valid @RequestBody UpdateExamRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String instructorId = jwt.getClaimAsString("userId");
        ExamResponse data = examService.updateExam(examId, request, instructorId);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Exam updated", data));
    }

    @DeleteMapping("/{examId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @Operation(summary = "Delete draft exam (owner only)")
    @ApiResponse(responseCode = "200", description = "Exam deleted")
    public ResponseEntity<BaseResponse<Void>> deleteExam(
            @PathVariable String examId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String instructorId = jwt.getClaimAsString("userId");
        examService.deleteExam(examId, instructorId);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Exam deleted", null));
    }

    @GetMapping
    @Operation(summary = "List exams")
    @ApiResponse(responseCode = "200", description = "Exams retrieved")
    public ResponseEntity<BaseResponse<List<ExamResponse>>> getAllExams() {
        List<ExamResponse> data = examService.getAllExams();
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Exams retrieved", data));
    }

    @GetMapping("/my-classes")
    @Operation(summary = "List published exams in my classes (student view)")
    @ApiResponse(responseCode = "200", description = "Exams retrieved")
    public ResponseEntity<BaseResponse<List<ExamResponse>>> getExamsByMyClasses(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("userId");
        List<ExamResponse> data = examService.getExamsByUserClasses(userId);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Exams retrieved", data));
    }

    @GetMapping("/{examId}")
    @Operation(summary = "Get exam details")
    @ApiResponse(responseCode = "200", description = "Exam retrieved")
    public ResponseEntity<BaseResponse<ExamResponse>> getExamById(
            @PathVariable String examId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt != null ? jwt.getClaimAsString("userId") : null;
        String role = jwt != null ? jwt.getClaimAsString("role") : null;
        ExamResponse data = examService.getExamById(examId, userId, role);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Exam retrieved", data));
    }

    @PostMapping("/{examId}/start")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Start exam attempt (opens server-tracked session)")
    @ApiResponse(responseCode = "200", description = "Exam start metadata")
    public ResponseEntity<BaseResponse<ExamStartResponse>> startExam(
            @PathVariable String examId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getClaimAsString("userId");
        ExamStartResponse data = examService.startExam(examId, userId);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Exam start metadata", data));
    }

    @PostMapping("/{examId}/questions")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @Operation(summary = "Attach question to exam")
    @ApiResponse(responseCode = "200", description = "Question attached")
    public ResponseEntity<BaseResponse<Void>> attachQuestion(@PathVariable String examId,
                                                             @Valid @RequestBody AttachQuestionRequest request) {
        examService.attachQuestion(examId, request);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Question attached", null));
    }

    @GetMapping("/{examId}/questions")
    @Operation(summary = "Get exam questions")
    @ApiResponse(responseCode = "200", description = "Questions retrieved")
    public ResponseEntity<BaseResponse<List<ExamQuestionResponse>>> getExamQuestions(@PathVariable String examId) {
        List<ExamQuestionResponse> data = examService.getExamQuestions(examId);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Questions retrieved", data));
    }

    @PostMapping("/{examId}/questions/bulk")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @Operation(summary = "Attach multiple questions to exam")
    @ApiResponse(responseCode = "200", description = "Questions attached")
    public ResponseEntity<BaseResponse<Void>> attachQuestionsBulk(@PathVariable String examId,
                                                                  @Valid @RequestBody AttachQuestionsBulkRequest request) {
        examService.attachQuestionsBulk(examId, request);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Questions attached", null));
    }
}
