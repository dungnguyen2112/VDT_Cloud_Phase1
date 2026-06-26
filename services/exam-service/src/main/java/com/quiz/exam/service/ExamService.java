package com.quiz.exam.service;

import com.quiz.exam.dto.AttachQuestionRequest;
import com.quiz.exam.dto.AttachQuestionsBulkRequest;
import com.quiz.exam.dto.CreateExamRequest;
import com.quiz.exam.dto.ExamPolicyDto;
import com.quiz.exam.dto.ExamQuestionResponse;
import com.quiz.exam.dto.ExamResponse;
import com.quiz.exam.dto.ExamStartResponse;
import com.quiz.exam.dto.UpdateExamRequest;
import java.util.List;

public interface ExamService {

    ExamResponse createExam(CreateExamRequest request, String createdBy);

    ExamResponse publishExam(String examId, String instructorId);

    ExamResponse updateExam(String examId, UpdateExamRequest request, String instructorId);

    void deleteExam(String examId, String instructorId);

    List<ExamResponse> getAllExams();

    List<ExamResponse> getExamsByUserClasses(String userId);

    ExamResponse getExamById(String examId, String viewerUserId, String viewerRole);

    ExamStartResponse startExam(String examId, String userId);

    void attachQuestion(String examId, AttachQuestionRequest request);

    void attachQuestionsBulk(String examId, AttachQuestionsBulkRequest request);

    List<String> getExamQuestionIds(String examId);

    List<ExamQuestionResponse> getExamQuestions(String examId);

    ExamPolicyDto getExamPolicyForResult(String examId);

    void assertSubmitSessionValid(String examId, String userId);

    void completeExamSession(String examId, String userId);
}
