package com.quiz.result.client;

import com.quiz.result.dto.BaseResponse;
import com.quiz.result.dto.ExamQuestionResponse;
import com.quiz.result.dto.ExamPolicyDto;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "exam-service")
public interface ExamServiceClient {

    @GetMapping("/api/v1/internal/exams/{examId}/policy")
    BaseResponse<ExamPolicyDto> getPolicy(
            @PathVariable("examId") String examId,
            @RequestHeader("X-Service-Token") String serviceToken
    );

    @PostMapping("/api/v1/internal/exams/{examId}/validate-session/{userId}")
    BaseResponse<Void> validateSession(
            @PathVariable("examId") String examId,
            @PathVariable("userId") String userId,
            @RequestHeader("X-Service-Token") String serviceToken
    );

    @PostMapping("/api/v1/internal/exams/{examId}/complete-session/{userId}")
    BaseResponse<Void> completeSession(
            @PathVariable("examId") String examId,
            @PathVariable("userId") String userId,
            @RequestHeader("X-Service-Token") String serviceToken
    );

        @GetMapping("/api/v1/exams/{examId}/questions")
        BaseResponse<List<ExamQuestionResponse>> getExamQuestions(@PathVariable("examId") String examId);
}
