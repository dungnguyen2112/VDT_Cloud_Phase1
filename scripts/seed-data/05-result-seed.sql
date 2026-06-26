USE result_service_db;

DELETE FROM exam_violation_events;
DELETE FROM exam_violations;
DELETE FROM exam_results;

INSERT INTO exam_results (id, user_id, exam_id, score, total_questions, violation_count, submitted_at) VALUES
(1, '22222222-2222-2222-2222-222222222222', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 9, 10, 0, '2026-04-10 09:15:00'),
(2, '33333333-3333-3333-3333-333333333333', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 7, 10, 1, '2026-04-10 09:18:00');

INSERT INTO exam_violations (id, user_id, exam_id, violation_count, updated_at) VALUES
(1, '33333333-3333-3333-3333-333333333333', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 1, '2026-04-10 09:17:30');

INSERT INTO exam_violation_events (id, user_id, exam_id, type, created_at) VALUES
(1, '33333333-3333-3333-3333-333333333333', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'TAB_HIDDEN', '2026-04-10 09:17:30'),
(2, '33333333-3333-3333-3333-333333333333', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'FULLSCREEN_EXIT', '2026-04-10 09:17:45');
