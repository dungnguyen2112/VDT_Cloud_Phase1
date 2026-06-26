package com.quiz.exam.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = PRIVATE)
public class AttachQuestionsBulkRequest {

    @NotEmpty(message = "questionIds must not be empty")
    @Size(max = 50, message = "You can attach at most 50 questions per request")
    List<String> questionIds;
}

