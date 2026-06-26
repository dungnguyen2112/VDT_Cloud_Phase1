package com.quiz.question.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = PRIVATE)
public class QuestionResponse {
    String id;
    String content;
    String optionA;
    String optionB;
    String optionC;
    String optionD;
    String correctAnswer;
    String category;
    Instant createdAt;
}
