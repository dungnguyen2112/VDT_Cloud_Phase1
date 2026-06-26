package com.quiz.result.service;

import com.quiz.result.client.ExamServiceClient;
import com.quiz.result.dto.BaseResponse;
import com.quiz.result.dto.ExamPolicyDto;
import com.quiz.result.exception.BadRequestException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ExamServiceGateway {

    private static final Logger log = LoggerFactory.getLogger(ExamServiceGateway.class);

    private final ExamServiceClient examServiceClient;

    public ExamServiceGateway(ExamServiceClient examServiceClient) {
        this.examServiceClient = examServiceClient;
    }

    @Retry(name = "examServiceInternal", fallbackMethod = "fallbackPolicy")
    @CircuitBreaker(name = "examServiceInternal", fallbackMethod = "fallbackPolicy")
    public BaseResponse<ExamPolicyDto> getPolicy(String examId, String serviceToken) {
        return examServiceClient.getPolicy(examId, serviceToken);
    }

    @Retry(name = "examServiceInternal", fallbackMethod = "fallbackValidateSession")
    @CircuitBreaker(name = "examServiceInternal", fallbackMethod = "fallbackValidateSession")
    public BaseResponse<Void> validateSession(String examId, String userId, String serviceToken) {
        return examServiceClient.validateSession(examId, userId, serviceToken);
    }

    @Retry(name = "examServiceInternal", fallbackMethod = "fallbackCompleteSession")
    @CircuitBreaker(name = "examServiceInternal", fallbackMethod = "fallbackCompleteSession")
    public BaseResponse<Void> completeSession(String examId, String userId, String serviceToken) {
        return examServiceClient.completeSession(examId, userId, serviceToken);
    }

    private BaseResponse<ExamPolicyDto> fallbackPolicy(String examId, String serviceToken, Throwable t) {
        log.warn("[RESILIENCE] exam-service fallback getPolicy examId={} due to: {}", examId, t.toString());
        if (t instanceof FeignException.NotFound) {
            throw new BadRequestException("Exam not found");
        }
        throw new IllegalStateException("Exam service unavailable");
    }

    private BaseResponse<Void> fallbackValidateSession(String examId, String userId, String serviceToken, Throwable t) {
        log.warn("[RESILIENCE] exam-service fallback validateSession examId={} userId={} due to: {}", examId, userId, t.toString());
        if (t instanceof FeignException) {
            FeignException fe = (FeignException) t;
            if (fe.status() >= 400 && fe.status() < 500) {
                throw new BadRequestException("Exam session invalid: start the exam before submitting");
            }
        }
        throw new IllegalStateException("Exam service unavailable");
    }

    private BaseResponse<Void> fallbackCompleteSession(String examId, String userId, String serviceToken, Throwable t) {
        log.warn("[RESILIENCE] exam-service fallback completeSession examId={} userId={} due to: {}", examId, userId, t.toString());
        return null;
    }
}

