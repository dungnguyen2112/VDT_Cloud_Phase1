package com.quiz.result.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "exam_results")
public class ExamResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "exam_id", nullable = false, length = 36)
    private String examId;

    @Column(nullable = false)
    private Integer score;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Column(name = "violation_count", nullable = false)
    private Integer violationCount;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @PrePersist
    public void prePersist() {
        if (this.submittedAt == null) {
            this.submittedAt = Instant.now();
        }
        if (this.violationCount == null) {
            this.violationCount = 0;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getExamId() {
        return examId;
    }

    public void setExamId(String examId) {
        this.examId = examId;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Integer getViolationCount() {
        return violationCount;
    }

    public void setViolationCount(Integer violationCount) {
        this.violationCount = violationCount;
    }
}

