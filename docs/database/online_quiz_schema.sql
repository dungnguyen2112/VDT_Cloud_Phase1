CREATE DATABASE IF NOT EXISTS online_quiz;
USE online_quiz;

CREATE TABLE IF NOT EXISTS users (
    id CHAR(36) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS questions (
    id CHAR(36) PRIMARY KEY,
    content TEXT NOT NULL,
    option_a VARCHAR(255) NOT NULL,
    option_b VARCHAR(255) NOT NULL,
    option_c VARCHAR(255) NOT NULL,
    option_d VARCHAR(255) NOT NULL,
    correct_answer CHAR(1) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_correct_answer CHECK (correct_answer IN ('A', 'B', 'C', 'D'))
);

CREATE TABLE IF NOT EXISTS exams (
    id CHAR(36) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    duration INT NOT NULL,
    created_by CHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_exams_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS exam_questions (
    exam_id CHAR(36) NOT NULL,
    question_id CHAR(36) NOT NULL,
    PRIMARY KEY (exam_id, question_id),
    CONSTRAINT fk_exam_questions_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
    CONSTRAINT fk_exam_questions_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS exam_results (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    exam_id CHAR(36) NOT NULL,
    score DECIMAL(5,2) NOT NULL,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_exam_results_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_exam_results_exam FOREIGN KEY (exam_id) REFERENCES exams(id)
);
