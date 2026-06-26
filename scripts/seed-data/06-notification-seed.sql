USE notification_service_db;

DELETE FROM notifications;

INSERT INTO notifications (id, user_id, type, title, body, channel, created_at) VALUES
('n0000000-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', 'EXAM_CREATED', 'Bài thi mới trong lớp', 'Có bài thi mới: "Midterm Microservices - 2026".', 'IN_APP', '2026-04-03 08:05:00.000000'),
('n0000000-0000-0000-0000-000000000002', '22222222-2222-2222-2222-222222222222', 'CLASS_USER_ADDED', 'Bạn đã được thêm vào lớp', 'Lớp: Lập trình Hướng Dịch Vụ - Nhóm 01', 'IN_APP', '2026-04-01 08:20:00.000000'),
('n0000000-0000-0000-0000-000000000003', '33333333-3333-3333-3333-333333333333', 'EXAM_SUBMITTED', 'Kết quả bài thi', '"Midterm Microservices - 2026" — Điểm: 7/10', 'IN_APP', '2026-04-10 09:18:00.000000'),
('n0000000-0000-0000-0000-000000000004', '22222222-2222-2222-2222-222222222222', 'AUTH_USER_REGISTERED', 'Xác thực email', 'Mã xác thực email của bạn đã được gửi. Vui lòng kiểm tra hộp thư.', 'IN_APP', '2026-04-01 08:11:00.000000');
