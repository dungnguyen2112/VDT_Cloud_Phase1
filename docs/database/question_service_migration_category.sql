-- Migration: add category for question bank filtering/grouping
-- Usage:
--   mysql -u root -p question_service_db < docs/database/question_service_migration_category.sql

USE question_service_db;

ALTER TABLE questions
    ADD COLUMN category VARCHAR(100) NOT NULL DEFAULT 'GENERAL';

