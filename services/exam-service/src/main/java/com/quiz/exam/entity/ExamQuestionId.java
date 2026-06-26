package com.quiz.exam.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = PRIVATE)
public class ExamQuestionId implements Serializable {

    @Column(name = "exam_id", nullable = false, length = 36)
    String examId;

    @Column(name = "question_id", nullable = false, length = 36)
    String questionId;
}
