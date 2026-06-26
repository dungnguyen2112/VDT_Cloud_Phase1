package com.quiz.result.service;

import com.quiz.result.dto.ExamResultResponse;
import com.quiz.result.dto.SubmitAnswerRequest;
import com.quiz.result.dto.ViolationEventResponse;
import com.quiz.result.dto.ViolationResponse;
import java.util.List;

public interface ResultService {

    ExamResultResponse submitExam(SubmitAnswerRequest request, String userId, String userEmail, String idempotencyKey);

    ViolationResponse reportViolation(String examId, String userId, String type);

    List<ViolationEventResponse> getViolationEventsForExam(String examId, String instructorUserId, String userId);

    List<ExamResultResponse> getResultsByUserId(String userId);

    List<ExamResultResponse> getResultsByUserId(String userId, boolean applyStudentVisibilityPolicy);

    List<ExamResultResponse> getExamResultsForReport(String examId, String instructorUserId);

    String exportExamResultsCsv(String examId, String instructorUserId);
}
