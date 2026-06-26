package com.quiz.question.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = PRIVATE)
public class Question {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    String id;

    @Column(nullable = false, columnDefinition = "TEXT")
    String content;

    @Column(name = "option_a", nullable = false, length = 255)
    String optionA;

    @Column(name = "option_b", nullable = false, length = 255)
    String optionB;

    @Column(name = "option_c", nullable = false, length = 255)
    String optionC;

    @Column(name = "option_d", nullable = false, length = 255)
    String optionD;

    @Column(name = "correct_answer", nullable = false, length = 1)
    String correctAnswer;

    @Column(name = "category", nullable = false, length = 100)
    String category;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.category == null || this.category.isBlank()) {
            this.category = "GENERAL";
        }
    }
}
