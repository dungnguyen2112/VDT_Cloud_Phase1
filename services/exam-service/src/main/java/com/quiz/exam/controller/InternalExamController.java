package com.quiz.exam.controller;

import com.quiz.exam.dto.BaseResponse;
import com.quiz.exam.dto.ExamPolicyDto;
import com.quiz.exam.service.ExamService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/internal/exams")
public class InternalExamController {

    private final ExamService examService;
    private final String serviceToken;

    public InternalExamController(
            ExamService examService,
            @Value("${app.internal.service-token:}") String serviceToken
    ) {
        this.examService = examService;
        this.serviceToken = serviceToken;
    }

    @GetMapping("/{examId}/policy")
    public ResponseEntity<BaseResponse<ExamPolicyDto>> policy(
            @RequestHeader(value = "X-Service-Token", required = false) String token,
            @PathVariable String examId
    ) {
        assertToken(token);
        ExamPolicyDto data = examService.getExamPolicyForResult(examId);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "OK", data));
    }

    @PostMapping("/{examId}/validate-session/{userId}")
    public ResponseEntity<BaseResponse<Void>> validateSession(
            @RequestHeader(value = "X-Service-Token", required = false) String token,
            @PathVariable String examId,
            @PathVariable String userId
    ) {
        assertToken(token);
        examService.assertSubmitSessionValid(examId, userId);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "OK", null));
    }

    @PostMapping("/{examId}/complete-session/{userId}")
    public ResponseEntity<BaseResponse<Void>> completeSession(
            @RequestHeader(value = "X-Service-Token", required = false) String token,
            @PathVariable String examId,
            @PathVariable String userId
    ) {
        assertToken(token);
        examService.completeExamSession(examId, userId);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "OK", null));
    }

    private void assertToken(String token) {
        if (serviceToken == null || serviceToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Internal API not configured");
        }
        if (token == null || !serviceToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid service token");
        }
    }
}
