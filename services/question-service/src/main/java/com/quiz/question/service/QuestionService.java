package com.quiz.question.service;

import com.quiz.question.dto.CreateQuestionRequest;
import com.quiz.question.dto.GenerateQuestionRequest;
import com.quiz.question.dto.QuestionAnswerResponse;
import com.quiz.question.dto.QuestionResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface QuestionService {
    QuestionResponse createQuestion(CreateQuestionRequest request);

    List<QuestionResponse> getAllQuestions();

    List<QuestionResponse> getQuestionBank(String keyword, String category);

    List<String> getQuestionCategories();

    QuestionResponse getQuestionById(String questionId);

    QuestionResponse updateQuestion(String questionId, CreateQuestionRequest request);

    void deleteQuestion(String questionId);

    List<QuestionResponse> getQuestionsByExamId(String examId);

    List<QuestionAnswerResponse> getAnswersByExamId(String examId);

    List<QuestionResponse> importQuestions(MultipartFile file, String examId);

    List<QuestionResponse> generateQuestions(GenerateQuestionRequest request, String examId);
}
