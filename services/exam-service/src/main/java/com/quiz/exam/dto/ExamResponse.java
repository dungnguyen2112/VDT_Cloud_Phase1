package com.quiz.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = PRIVATE)
public class ExamResponse {
    String id;
    String title;
    Integer duration;
    String classId;
    String createdBy;
    Instant createdAt;
    String status;
    Instant availableFrom;
    Instant availableUntil;
    Integer maxAttempts;
    Boolean showCorrectAnswers;
    Boolean showScoreImmediately;
    List<String> questionIds;
}
