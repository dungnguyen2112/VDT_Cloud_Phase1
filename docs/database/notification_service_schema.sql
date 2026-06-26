CREATE DATABASE IF NOT EXISTS notification_service_db;
USE notification_service_db;

CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    type VARCHAR(64) NOT NULL,
    title VARCHAR(512) NOT NULL,
    body TEXT NOT NULL,
    channel VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    INDEX idx_notifications_user_id (user_id),
    INDEX idx_notifications_created_at (created_at)
);
