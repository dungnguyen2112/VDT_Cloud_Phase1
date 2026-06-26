CREATE DATABASE IF NOT EXISTS exam_service_db;
USE exam_service_db;

CREATE TABLE IF NOT EXISTS exams (
    id CHAR(36) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    duration INT NOT NULL,
    class_id CHAR(36) NOT NULL,
    created_by CHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    available_from TIMESTAMP(6) NULL,
    available_until TIMESTAMP(6) NULL,
    max_attempts INT NOT NULL DEFAULT 1,
    show_correct_answers TINYINT(1) NOT NULL DEFAULT 0,
    show_score_immediately TINYINT(1) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS exam_questions (
    exam_id CHAR(36) NOT NULL,
    question_id CHAR(36) NOT NULL,
    PRIMARY KEY (exam_id, question_id)
);

CREATE TABLE IF NOT EXISTS exam_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    exam_id CHAR(36) NOT NULL,
    score INT NOT NULL,
    total_questions INT NOT NULL,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
