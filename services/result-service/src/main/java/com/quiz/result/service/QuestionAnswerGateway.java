package com.quiz.result.service;

import com.quiz.result.client.ExamServiceClient;
import com.quiz.result.client.QuestionServiceClient;
import com.quiz.result.dto.BaseResponse;
import com.quiz.result.dto.ExamQuestionResponse;
import com.quiz.result.dto.QuestionAnswerResponse;
import com.quiz.result.dto.QuestionResponse;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class QuestionAnswerGateway {

    private static final Logger log = LoggerFactory.getLogger(QuestionAnswerGateway.class);

    private final QuestionServiceClient questionServiceClient;
    private final ExamServiceClient examServiceClient;

    public QuestionAnswerGateway(QuestionServiceClient questionServiceClient, ExamServiceClient examServiceClient) {
        this.questionServiceClient = questionServiceClient;
        this.examServiceClient = examServiceClient;
    }

    @Retry(name = "questionServiceAnswers", fallbackMethod = "fallbackAnswers")
    public List<QuestionAnswerResponse> fetchCorrectAnswers(String examId) {
        BaseResponse<List<QuestionAnswerResponse>> response = questionServiceClient.getAnswersByExamId(examId);
        List<QuestionAnswerResponse> data = response.getData();
        if (data != null && !data.isEmpty()) {
            return data;
        }
        return buildAnswersFromExamQuestions(examId);
    }

    private QuestionAnswerResponse loadQuestionAnswer(String questionId) {
        try {
            BaseResponse<QuestionResponse> response = questionServiceClient.getQuestionById(questionId);
            QuestionResponse question = response == null ? null : response.getData();
            if (question == null || question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank()) {
                return null;
            }
            return new QuestionAnswerResponse(question.getId(), question.getCorrectAnswer());
        } catch (RuntimeException ex) {
            log.warn("[EVENT] Failed to load answer for questionId={} due to: {}", questionId, ex.getMessage());
            return null;
        }
    }

    public List<QuestionAnswerResponse> fallbackAnswers(String examId, Throwable throwable) {
        log.warn("[EVENT] Fallback answers for examId={} due to: {}", examId, throwable.getMessage());
        return buildAnswersFromExamQuestions(examId);
    }

    private List<QuestionAnswerResponse> buildAnswersFromExamQuestions(String examId) {
        BaseResponse<List<ExamQuestionResponse>> examQuestionsResponse = examServiceClient.getExamQuestions(examId);
        List<ExamQuestionResponse> examQuestions = examQuestionsResponse == null ? null : examQuestionsResponse.getData();
        if (examQuestions == null || examQuestions.isEmpty()) {
            return List.of();
        }

        List<QuestionAnswerResponse> reconstructed = examQuestions.stream()
                .map(ExamQuestionResponse::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .map(this::loadQuestionAnswer)
                .filter(Objects::nonNull)
                .toList();

        if (!reconstructed.isEmpty()) {
            log.info("[EVENT] Reconstructed {} answer keys for examId={} via fallback", reconstructed.size(), examId);
        }
        return reconstructed;
    }
}
