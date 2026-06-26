package com.quiz.result.controller;

import com.quiz.result.dto.BaseResponse;
import com.quiz.result.dto.ExamResultResponse;
import com.quiz.result.dto.SubmitAnswerRequest;
import com.quiz.result.dto.ViolationEventResponse;
import com.quiz.result.dto.ViolationRequest;
import com.quiz.result.dto.ViolationResponse;
import com.quiz.result.service.ResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/results")
@Tag(name = "Result", description = "Exam result endpoints")
public class ResultController {

    private final ResultService resultService;

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    @PostMapping("/submit")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Submit exam result")
    @ApiResponse(responseCode = "201", description = "Result submitted")
    public ResponseEntity<BaseResponse<ExamResultResponse>> submitExam(
            @Valid @RequestBody SubmitAnswerRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt
    ) {
         String userId = jwt.getClaimAsString("userId");
         String userEmail = jwt.getClaimAsString("email");
         ExamResultResponse data = resultService.submitExam(request, userId, userEmail, idempotencyKey);
         return ResponseEntity.status(HttpStatus.CREATED)
                 .body(BaseResponse.of(HttpStatus.CREATED.value(), "Result submitted", data));
    }

        @PostMapping("/exam/violation")
        @PreAuthorize("hasRole('STUDENT')")
        @Operation(summary = "Report exam violation")
        @ApiResponse(responseCode = "200", description = "Violation reported")
        public ResponseEntity<BaseResponse<ViolationResponse>> reportViolation(
                @Valid @RequestBody ViolationRequest request,
            @AuthenticationPrincipal Jwt jwt
        ) {
        String userId = jwt.getClaimAsString("userId");
        ViolationResponse data = resultService.reportViolation(request.getExamId(), userId, request.getType());
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Violation reported", data));
        }

    @GetMapping("/exams/{examId}/violations")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @Operation(summary = "Violation events timeline for an exam (owner only)")
    @ApiResponse(responseCode = "200", description = "Violation events retrieved")
    public ResponseEntity<BaseResponse<List<ViolationEventResponse>>> getViolationEvents(
            @PathVariable String examId,
            @RequestParam(name = "userId", required = false) String userId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String instructorId = jwt.getClaimAsString("userId");
        List<ViolationEventResponse> data = resultService.getViolationEventsForExam(examId, instructorId, userId);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Violation events retrieved", data));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @Operation(summary = "Get exam results by user ID (admin/instructor)")
    @ApiResponse(responseCode = "200", description = "Results retrieved")
    public ResponseEntity<BaseResponse<List<ExamResultResponse>>> getResults(@PathVariable String userId) {
        List<ExamResultResponse> data = resultService.getResultsByUserId(userId);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Results retrieved", data));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR')")
    @Operation(summary = "Get my exam results")
    @ApiResponse(responseCode = "200", description = "Results retrieved")
    public ResponseEntity<BaseResponse<List<ExamResultResponse>>> getMyResults(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("userId");
        String role = jwt.getClaimAsString("role");
        boolean applyStudentVisibilityPolicy = "STUDENT".equalsIgnoreCase(role);
        List<ExamResultResponse> data = resultService.getResultsByUserId(userId, applyStudentVisibilityPolicy);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Results retrieved", data));
    }

    @GetMapping("/exams/{examId}/report")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @Operation(summary = "Exam results report (exam owner only)")
    @ApiResponse(responseCode = "200", description = "Results retrieved")
    public ResponseEntity<BaseResponse<List<ExamResultResponse>>> getExamReport(
            @PathVariable String examId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String instructorId = jwt.getClaimAsString("userId");
        List<ExamResultResponse> data = resultService.getExamResultsForReport(examId, instructorId);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Results retrieved", data));
    }

    @GetMapping(value = "/exams/{examId}/report.csv", produces = "text/csv")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @Operation(summary = "Download exam results as CSV (exam owner only)")
    @ApiResponse(responseCode = "200", description = "CSV file")
    public ResponseEntity<byte[]> exportExamReportCsv(
            @PathVariable String examId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String instructorId = jwt.getClaimAsString("userId");
        String csv = resultService.exportExamResultsCsv(examId, instructorId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("exam-" + examId + "-results.csv").build()
        );
        return ResponseEntity.ok().headers(headers).body(csv.getBytes(StandardCharsets.UTF_8));
    }
}

