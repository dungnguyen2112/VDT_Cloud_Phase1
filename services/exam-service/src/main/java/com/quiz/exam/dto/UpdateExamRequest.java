package com.quiz.exam.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateExamRequest {
    String title;
    Integer duration;
    Instant availableFrom;
    Instant availableUntil;
    Integer maxAttempts;
    Boolean showCorrectAnswers;
    Boolean showScoreImmediately;
}
