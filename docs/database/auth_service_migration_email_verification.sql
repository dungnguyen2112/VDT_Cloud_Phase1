-- Migration for adding email verification columns to auth_service_db.users
-- Usage (example): mysql -u root -p auth_service_db < docs/database/auth_service_migration_email_verification.sql

USE auth_service_db;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS email_verification_code_hash VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS email_verification_expires_at TIMESTAMP NULL;

