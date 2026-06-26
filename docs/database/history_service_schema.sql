CREATE TABLE IF NOT EXISTS `exam_history` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` CHAR(36) NOT NULL,
    `exam_id` CHAR(36) NOT NULL,
    `score` INT NOT NULL,
    `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (`user_id`),
    INDEX idx_exam_id (`exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
