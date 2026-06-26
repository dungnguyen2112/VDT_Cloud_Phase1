package com.quiz.exam.dto;

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
public class ExamPolicyDto {
    String examId;
    String title;
    String status;
    String createdBy;
    Instant availableFrom;
    Instant availableUntil;
    int durationMinutes;
    int maxAttempts;
    boolean showCorrectAnswers;
    boolean showScoreImmediately;
}
