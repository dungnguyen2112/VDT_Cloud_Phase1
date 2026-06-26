package com.quiz.exam.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exams")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = PRIVATE)
public class Exam {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    String id;

    @Column(nullable = false, length = 255)
    String title;

    @Column(nullable = false)
    Integer duration;

    @Column(name = "class_id", nullable = false, length = 36)
    String classId;

    @Column(name = "created_by", nullable = false, length = 36)
    String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    ExamStatus status;

    @Column(name = "available_from")
    Instant availableFrom;

    @Column(name = "available_until")
    Instant availableUntil;

    @Column(name = "max_attempts", nullable = false)
    Integer maxAttempts;

    @Column(name = "show_correct_answers", nullable = false)
    Boolean showCorrectAnswers;

    @Column(name = "show_score_immediately", nullable = false)
    Boolean showScoreImmediately;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = ExamStatus.DRAFT;
        }
        if (this.maxAttempts == null) {
            this.maxAttempts = 1;
        }
        if (this.showCorrectAnswers == null) {
            this.showCorrectAnswers = false;
        }
        if (this.showScoreImmediately == null) {
            this.showScoreImmediately = true;
        }
    }
}
