package com.quiz.exam.service;

import com.quiz.exam.client.ClassServiceClient;
import com.quiz.exam.dto.BaseResponse;
import com.quiz.exam.dto.ClassServiceClassResponse;
import com.quiz.exam.dto.ClassServiceStudentResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ClassServiceGateway {

    private static final Logger log = LoggerFactory.getLogger(ClassServiceGateway.class);

    private final ClassServiceClient classServiceClient;

    public ClassServiceGateway(ClassServiceClient classServiceClient) {
        this.classServiceClient = classServiceClient;
    }

    @Retry(name = "classServiceCalls", fallbackMethod = "fallbackClasses")
    @CircuitBreaker(name = "classServiceCalls", fallbackMethod = "fallbackClasses")
    public BaseResponse<List<ClassServiceClassResponse>> getClassesByUserId(String userId) {
        return classServiceClient.getClassesByUserId(userId);
    }

    @Retry(name = "classServiceCalls", fallbackMethod = "fallbackStudents")
    @CircuitBreaker(name = "classServiceCalls", fallbackMethod = "fallbackStudents")
    public BaseResponse<List<ClassServiceStudentResponse>> getStudentsByClassId(String classId) {
        return classServiceClient.getStudentsByClassId(classId);
    }

    @SuppressWarnings("unused")
    private BaseResponse<List<ClassServiceClassResponse>> fallbackClasses(String userId, Throwable t) {
        log.warn("[RESILIENCE] class-service fallback getClassesByUserId userId={} due to: {}", userId, t.toString());
        return BaseResponse.of(503, "class-service unavailable", List.of());
    }

    @SuppressWarnings("unused")
    private BaseResponse<List<ClassServiceStudentResponse>> fallbackStudents(String classId, Throwable t) {
        log.warn("[RESILIENCE] class-service fallback getStudentsByClassId classId={} due to: {}", classId, t.toString());
        return BaseResponse.of(503, "class-service unavailable", List.of());
    }
}

