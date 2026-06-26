USE auth_service_db;

DELETE FROM users;

INSERT INTO users (
    id, username, email, password, role, created_at,
    email_verified, email_verification_code_hash, email_verification_expires_at
) VALUES
('11111111-1111-1111-1111-111111111111', 'giangvien01', 'giangvien01@emid.edu.vn', '$2a$10$demo-demo-demo-demo-demo-demo-demo-demo-demo-demo-demo', 'INSTRUCTOR', '2026-04-01 08:00:00', 1, NULL, NULL),
('22222222-2222-2222-2222-222222222222', 'sinhvien01', 'sinhvien01@emid.edu.vn', '$2a$10$demo-demo-demo-demo-demo-demo-demo-demo-demo-demo-demo', 'STUDENT', '2026-04-01 08:10:00', 1, NULL, NULL),
('33333333-3333-3333-3333-333333333333', 'sinhvien02', 'sinhvien02@emid.edu.vn', '$2a$10$demo-demo-demo-demo-demo-demo-demo-demo-demo-demo-demo', 'STUDENT', '2026-04-01 08:12:00', 1, NULL, NULL),
('44444444-4444-4444-4444-444444444444', 'admin01', 'admin01@emid.edu.vn', '$2a$10$demo-demo-demo-demo-demo-demo-demo-demo-demo-demo-demo', 'ADMIN', '2026-04-01 08:15:00', 1, NULL, NULL);
