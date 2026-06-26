USE notification_service_db;

-- Align existing table types with JPA validation expectations.
ALTER TABLE notifications
    MODIFY COLUMN id VARCHAR(36) NOT NULL,
    MODIFY COLUMN user_id VARCHAR(36) NOT NULL;
