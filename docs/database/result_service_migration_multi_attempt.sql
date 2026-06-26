-- Allow multiple attempts per user+exam (max controlled by exam-service policy).
USE result_service_db;

ALTER TABLE exam_results DROP INDEX uk_exam_result_user_exam;

CREATE INDEX idx_exam_result_user_exam ON exam_results (user_id, exam_id);
CREATE INDEX idx_exam_result_exam_submitted ON exam_results (exam_id, submitted_at);
