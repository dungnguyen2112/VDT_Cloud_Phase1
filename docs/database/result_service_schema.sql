CREATE DATABASE IF NOT EXISTS result_service_db;
USE result_service_db;

CREATE TABLE IF NOT EXISTS exam_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    exam_id CHAR(36) NOT NULL,
    score INT NOT NULL,
    total_questions INT NOT NULL,
    violation_count INT NOT NULL DEFAULT 0,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_exam_result_user_exam (user_id, exam_id),
    INDEX idx_exam_result_exam_submitted (exam_id, submitted_at)
);

CREATE TABLE IF NOT EXISTS exam_violations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    exam_id CHAR(36) NOT NULL,
    violation_count INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_exam_violation_user_exam UNIQUE (user_id, exam_id)
);

CREATE TABLE IF NOT EXISTS exam_violation_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    exam_id CHAR(36) NOT NULL,
    type VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_exam_violation_events_exam_time (exam_id, created_at),
    INDEX idx_exam_violation_events_user_exam_time (user_id, exam_id, created_at)
);

