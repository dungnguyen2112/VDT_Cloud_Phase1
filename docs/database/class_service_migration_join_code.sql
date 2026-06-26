-- One-time migration for existing class_service_db deployments created before join_code existed.
USE class_service_db;

ALTER TABLE classes
    ADD COLUMN join_code VARCHAR(16) NULL UNIQUE AFTER teacher_id;

CREATE INDEX idx_classes_join_code ON classes (join_code);
