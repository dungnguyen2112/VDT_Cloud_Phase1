USE exam_service_db;

DELETE FROM exam_questions;
DELETE FROM exams;

INSERT INTO exams (
    id, title, duration, class_id, created_by, created_at, status,
    available_from, available_until, max_attempts, show_correct_answers, show_score_immediately
) VALUES
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Midterm Microservices - 2026', 60, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', '2026-04-03 08:00:00', 'PUBLISHED', '2026-04-10 08:00:00.000000', '2026-04-10 23:59:59.000000', 2, 1, 1),
('cccccccc-cccc-cccc-cccc-cccccccccccc', 'Quiz Java Spring - Practice', 45, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '11111111-1111-1111-1111-111111111111', '2026-04-04 09:00:00', 'DRAFT', NULL, NULL, 1, 0, 0);

INSERT INTO exam_questions (exam_id, question_id) VALUES
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'q0000000-0000-0000-0000-000000000001'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'q0000000-0000-0000-0000-000000000002'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'q0000000-0000-0000-0000-000000000003'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'q0000000-0000-0000-0000-000000000004'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'q0000000-0000-0000-0000-000000000005'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'q0000000-0000-0000-0000-000000000006'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'q0000000-0000-0000-0000-000000000007'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'q0000000-0000-0000-0000-000000000008'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'q0000000-0000-0000-0000-000000000009'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'q0000000-0000-0000-0000-000000000010');
