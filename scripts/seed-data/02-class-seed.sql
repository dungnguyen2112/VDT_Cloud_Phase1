USE class_service_db;

DELETE FROM user_class;
DELETE FROM classes;

INSERT INTO classes (id, name, teacher_id, join_code) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Lập trình Hướng Dịch Vụ - Nhóm 01', '11111111-1111-1111-1111-111111111111', 'EMID2026'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Kiến Trúc Microservices - Nhóm 02', '11111111-1111-1111-1111-111111111111', 'MICRO2026');

INSERT INTO user_class (user_id, class_id) VALUES
('22222222-2222-2222-2222-222222222222', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
('33333333-3333-3333-3333-333333333333', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
('22222222-2222-2222-2222-222222222222', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb');
