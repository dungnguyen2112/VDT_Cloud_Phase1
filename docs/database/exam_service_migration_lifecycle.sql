-- Run once on existing exam_service_db before deploying new exam-service.
USE exam_service_db;

ALTER TABLE exams
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' AFTER created_at,
    ADD COLUMN available_from TIMESTAMP(6) NULL AFTER status,
    ADD COLUMN available_until TIMESTAMP(6) NULL AFTER available_from,
    ADD COLUMN max_attempts INT NOT NULL DEFAULT 1 AFTER available_until,
    ADD COLUMN show_correct_answers TINYINT(1) NOT NULL DEFAULT 0 AFTER max_attempts,
    ADD COLUMN show_score_immediately TINYINT(1) NOT NULL DEFAULT 0 AFTER show_correct_answers;

UPDATE exams SET status = 'PUBLISHED' WHERE status IS NULL OR status = '';
