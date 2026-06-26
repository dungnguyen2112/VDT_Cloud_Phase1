package com.quiz.result.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.Map;
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
public class SubmitAnswerRequest {
    @NotBlank
    @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "examId must be a UUID")
    String examId;

    @NotEmpty
    Map<
                    @NotBlank @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "questionId must be a UUID") String,
                    @NotBlank(message = "answer is required") String>
            answers;
}
