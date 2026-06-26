CREATE DATABASE IF NOT EXISTS class_service_db;
USE class_service_db;

CREATE TABLE IF NOT EXISTS classes (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    teacher_id CHAR(36) NOT NULL,
    join_code VARCHAR(16) NULL UNIQUE,
    INDEX idx_classes_join_code (join_code)
);

CREATE TABLE IF NOT EXISTS user_class (
    user_id CHAR(36) NOT NULL,
    class_id CHAR(36) NOT NULL,
    PRIMARY KEY (user_id, class_id),
    INDEX idx_user_class_class_id (class_id)
);
