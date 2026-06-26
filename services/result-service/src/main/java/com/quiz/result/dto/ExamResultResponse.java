package com.quiz.result.dto;

import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExamResultResponse {
    Long id;
    String userId;
    String examId;
    Integer score;
    Integer totalQuestions;
    Integer violationCount;
    Instant submittedAt;
    List<QuestionAnswerResponse> revealedAnswers;

    public ExamResultResponse(
            Long id,
            String userId,
            String examId,
            Integer score,
            Integer totalQuestions,
            Integer violationCount,
            Instant submittedAt,
            List<QuestionAnswerResponse> revealedAnswers) {
        this.id = id;
        this.userId = userId;
        this.examId = examId;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.violationCount = violationCount;
        this.submittedAt = submittedAt;
        this.revealedAnswers = revealedAnswers;
    }

    public ExamResultResponse(
            Long id,
            String userId,
            String examId,
            Integer score,
            Integer totalQuestions,
            Integer violationCount,
            Instant submittedAt) {
        this(id, userId, examId, score, totalQuestions, violationCount, submittedAt, null);
    }
}
