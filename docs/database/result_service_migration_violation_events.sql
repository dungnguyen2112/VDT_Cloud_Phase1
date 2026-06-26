-- Migration: add violation events timeline table
-- Usage:
--   mysql -u root -p result_service_db < docs/database/result_service_migration_violation_events.sql

USE result_service_db;

CREATE TABLE IF NOT EXISTS exam_violation_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    exam_id CHAR(36) NOT NULL,
    type VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_exam_violation_events_exam_time (exam_id, created_at),
    INDEX idx_exam_violation_events_user_exam_time (user_id, exam_id, created_at)
);

