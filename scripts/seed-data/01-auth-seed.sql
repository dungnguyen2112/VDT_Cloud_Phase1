USE auth_service_db;

DELETE FROM users;

INSERT INTO users (
    id, username, email, password, role, created_at,
    email_verified, email_verification_code_hash, email_verification_expires_at
) VALUES
('11111111-1111-1111-1111-111111111111', 'teacher', 'teacher@emid.edu.vn', '$2a$10$trT3.R/Nfey62eczbKEnueTcIbJXW.u1ffAo/XfyLpofwNDbEB86O', 'INSTRUCTOR', '2026-04-01 08:00:00', 1, NULL, NULL),
('22222222-2222-2222-2222-222222222222', 'student', 'student@emid.edu.vn', '$2a$10$trT3.R/Nfey62eczbKEnueTcIbJXW.u1ffAo/XfyLpofwNDbEB86O', 'STUDENT', '2026-04-01 08:10:00', 1, NULL, NULL),
('33333333-3333-3333-3333-333333333333', 'student2', 'student2@emid.edu.vn', '$2a$10$trT3.R/Nfey62eczbKEnueTcIbJXW.u1ffAo/XfyLpofwNDbEB86O', 'STUDENT', '2026-04-01 08:12:00', 1, NULL, NULL),
('44444444-4444-4444-4444-444444444444', 'admin', 'admin@emid.edu.vn', '$2a$10$trT3.R/Nfey62eczbKEnueTcIbJXW.u1ffAo/XfyLpofwNDbEB86O', 'ADMIN', '2026-04-01 08:15:00', 1, NULL, NULL);
