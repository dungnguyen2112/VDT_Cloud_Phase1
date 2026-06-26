package com.quiz.question.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    Instant timestamp;
    int status;
    String error;
    String message;
    String path;
}
