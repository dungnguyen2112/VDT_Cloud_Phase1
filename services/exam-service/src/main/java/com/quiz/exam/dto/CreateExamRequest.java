package com.quiz.exam.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import lombok.*;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateExamRequest {
    @NotBlank
    String title;

    @NotBlank
    String classId;

    @Min(1)
    Integer duration;

    /** Defaults to DRAFT; set PUBLISHED to publish immediately (notifies students). */
    String status;

    Instant availableFrom;
    Instant availableUntil;

    @Min(1)
    Integer maxAttempts;

    Boolean showCorrectAnswers;

    /**
     * If true (default), students see their score immediately after submit.
     * If false, submit response will hide score/totalQuestions.
     */
    Boolean showScoreImmediately;
}
