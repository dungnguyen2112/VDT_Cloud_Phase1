package com.quiz.exam.client;

import com.quiz.exam.dto.QuestionServiceBaseResponse;
import com.quiz.exam.dto.QuestionServiceQuestionResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "question-service", path = "/api/v1/questions")
public interface QuestionServiceClient {

    @GetMapping("/{id}")
    QuestionServiceBaseResponse<QuestionServiceQuestionResponse> getQuestionById(@PathVariable("id") String questionId);

    @GetMapping("/exam/{examId}")
    QuestionServiceBaseResponse<List<QuestionServiceQuestionResponse>> getQuestionsByExamId(@PathVariable("examId") String examId);
}
