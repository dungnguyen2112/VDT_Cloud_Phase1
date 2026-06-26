CREATE DATABASE IF NOT EXISTS auth_service_db;
USE auth_service_db;

CREATE TABLE IF NOT EXISTS users (
    id CHAR(36) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    email_verified BOOLEAN NOT NULL DEFAULT 0,
    email_verification_code_hash VARCHAR(255),
    email_verification_expires_at TIMESTAMP NULL
);
