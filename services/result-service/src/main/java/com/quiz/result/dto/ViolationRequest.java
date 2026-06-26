package com.quiz.result.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class ViolationRequest {
    @NotBlank
    @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "examId must be a UUID")
    String examId;

    String type;
}
