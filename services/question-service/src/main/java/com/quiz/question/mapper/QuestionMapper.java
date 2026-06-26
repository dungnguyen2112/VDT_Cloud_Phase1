package com.quiz.question.mapper;

import com.quiz.question.dto.QuestionAnswerResponse;
import com.quiz.question.dto.QuestionResponse;
import com.quiz.question.entity.Question;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface QuestionMapper {

    QuestionResponse toResponse(Question question);

    default QuestionAnswerResponse toAnswerResponse(Question question) {
        return QuestionAnswerResponse.builder()
                .questionId(question.getId())
                .correctAnswer(question.getCorrectAnswer())
                .build();
    }
}
