package com.quiz.auth.dto;

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
public class BaseResponse<T> {
    Instant timestamp;
    int status;
    String message;
    T data;

    public static <T> BaseResponse<T> of(int status, String message, T data) {
        return BaseResponse.<T>builder()
                .timestamp(Instant.now())
                .status(status)
                .message(message)
                .data(data)
                .build();
    }
}
