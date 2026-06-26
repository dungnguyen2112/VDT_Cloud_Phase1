CREATE DATABASE IF NOT EXISTS question_service_db;
USE question_service_db;

CREATE TABLE IF NOT EXISTS questions (
    id CHAR(36) PRIMARY KEY,
    content TEXT NOT NULL,
    category VARCHAR(100) NOT NULL DEFAULT 'GENERAL',
    option_a VARCHAR(255) NOT NULL,
    option_b VARCHAR(255) NOT NULL,
    option_c VARCHAR(255) NOT NULL,
    option_d VARCHAR(255) NOT NULL,
    correct_answer CHAR(1) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_correct_answer CHECK (correct_answer IN ('A', 'B', 'C', 'D'))
);

CREATE TABLE IF NOT EXISTS exam_questions (
    exam_id CHAR(36) NOT NULL,
    question_id CHAR(36) NOT NULL,
    PRIMARY KEY (exam_id, question_id)
);
