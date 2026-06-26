package com.quiz.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class CreateQuestionRequest {
    @NotBlank
    String content;

    @NotBlank
    String optionA;

    @NotBlank
    String optionB;

    @NotBlank
    String optionC;

    @NotBlank
    String optionD;

    @NotBlank
    @Pattern(regexp = "^[ABCD]$", message = "correctAnswer must be A, B, C, or D")
    String correctAnswer;

    String category;
}
