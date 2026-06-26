package com.quiz.classservice.dto;

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
public class ClassResponse {
    String id;
    String name;
    String teacherId;
    /** Only returned to the class teacher (or after create). */
    String joinCode;
    /**
     * Optional deep link for QR / share; only set when {@code app.class.join-url-base} is configured
     * and {@link #joinCode} is present.
     */
    String joinUrl;
}
