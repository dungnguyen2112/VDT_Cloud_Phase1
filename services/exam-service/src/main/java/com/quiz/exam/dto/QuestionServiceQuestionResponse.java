package com.quiz.exam.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = PRIVATE)
public class QuestionServiceQuestionResponse {
    String id;
    String content;
    String optionA;
    String optionB;
    String optionC;
    String optionD;
    String correctAnswer;
    Instant createdAt;
}
