
package com.quiz.exam.service;

import com.quiz.exam.client.QuestionServiceClient;
import com.quiz.exam.dto.QuestionServiceBaseResponse;
import com.quiz.exam.dto.QuestionServiceQuestionResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class QuestionServiceGateway {

    private static final Logger log = LoggerFactory.getLogger(QuestionServiceGateway.class);

    private final QuestionServiceClient questionServiceClient;

    public QuestionServiceGateway(QuestionServiceClient questionServiceClient) {
        this.questionServiceClient = questionServiceClient;
    }

    @Retry(name = "questionServiceCalls", fallbackMethod = "fallbackQuestion")
    @CircuitBreaker(name = "questionServiceCalls", fallbackMethod = "fallbackQuestion")
    public QuestionServiceBaseResponse<QuestionServiceQuestionResponse> getQuestionById(String questionId) {
        return questionServiceClient.getQuestionById(questionId);
    }

    @Retry(name = "questionServiceCalls", fallbackMethod = "fallbackQuestionsByExam")
    @CircuitBreaker(name = "questionServiceCalls", fallbackMethod = "fallbackQuestionsByExam")
    public QuestionServiceBaseResponse<List<QuestionServiceQuestionResponse>> getQuestionsByExamId(String examId) {
        return questionServiceClient.getQuestionsByExamId(examId);
    }

    @SuppressWarnings("unused")
    private QuestionServiceBaseResponse<QuestionServiceQuestionResponse> fallbackQuestion(String questionId, Throwable t) {
        log.warn("[RESILIENCE] question-service fallback getQuestionById questionId={} due to: {}", questionId, t.toString());
        return QuestionServiceBaseResponse.<QuestionServiceQuestionResponse>builder()
                .timestamp(java.time.Instant.now())
                .status(503)
                .message("question-service unavailable")
                .data(null)
                .build();
    }

    @SuppressWarnings("unused")
    private QuestionServiceBaseResponse<List<QuestionServiceQuestionResponse>> fallbackQuestionsByExam(String examId, Throwable t) {
        log.warn("[RESILIENCE] question-service fallback getQuestionsByExamId examId={} due to: {}", examId, t.toString());
        return QuestionServiceBaseResponse.<List<QuestionServiceQuestionResponse>>builder()
                .timestamp(java.time.Instant.now())
                .status(503)
                .message("question-service unavailable")
                .data(List.of())
                .build();
    }
}

