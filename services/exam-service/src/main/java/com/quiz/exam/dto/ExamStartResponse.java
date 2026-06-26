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
public class ExamStartResponse {
    ExamResponse exam;
    Instant serverTime;
    /** Latest moment the attempt may be submitted (server clock). */
    Instant deadlineAt;
}
