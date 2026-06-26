package com.quiz.result.client;

import com.quiz.result.dto.QuestionAnswerResponse;
import com.quiz.result.dto.QuestionResponse;
import com.quiz.result.dto.BaseResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "question-service", path = "/api/v1/questions")
public interface QuestionServiceClient {

    @GetMapping("/{id}")
    BaseResponse<QuestionResponse> getQuestionById(@PathVariable("id") String questionId);

    @GetMapping("/exam/{examId}/answers")
    BaseResponse<List<QuestionAnswerResponse>> getAnswersByExamId(@PathVariable("examId") String examId);
}

