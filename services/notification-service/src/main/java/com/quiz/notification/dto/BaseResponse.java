package com.quiz.notification.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
