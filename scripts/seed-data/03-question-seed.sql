USE question_service_db;

DELETE FROM exam_questions;
DELETE FROM questions;

INSERT INTO questions (id, content, category, option_a, option_b, option_c, option_d, correct_answer, created_at) VALUES
('f0000000-0000-0000-0000-000000000001', 'Mục tiêu chính của API Gateway trong hệ thống microservices là gì?', 'ARCHITECTURE', 'Định tuyến và kiểm soát truy cập tập trung', 'Lưu toàn bộ dữ liệu nghiệp vụ', 'Thay thế hoàn toàn các service', 'Chỉ dùng để hiển thị giao diện', 'A', '2026-04-01 09:00:00'),
('f0000000-0000-0000-0000-000000000002', 'Trong hệ thống thi online, lớp học thường dùng join code để làm gì?', 'CLASS', 'Sinh viên tự ghi danh vào lớp', 'Giảng viên tạo đề thi mới', 'Hệ thống sinh báo cáo CSV', 'Xóa bài thi đã xuất bản', 'A', '2026-04-01 09:01:00'),
('f0000000-0000-0000-0000-000000000003', 'Khi sinh viên bắt đầu làm bài, hệ thống nên tạo gì để quản lý thời gian và trạng thái làm bài?', 'EXAM', 'Phiên làm bài trên server', 'Một database mới cho mỗi lượt thi', 'Một tài khoản mới', 'Một bảng điểm tạm trên frontend', 'A', '2026-04-01 09:02:00'),
('f0000000-0000-0000-0000-000000000004', 'Trong kiến trúc database per service, mỗi service có nguyên tắc nào?', 'ARCHITECTURE', 'Dùng chung một database để dễ join dữ liệu', 'Mỗi service sở hữu database riêng', 'Chỉ có service lớn mới được dùng database', 'Chỉ frontend mới được lưu dữ liệu', 'B', '2026-04-01 09:03:00'),
('f0000000-0000-0000-0000-000000000005', 'Mục đích chính của Idempotency-Key khi nộp bài là gì?', 'RESULT', 'Ngăn submit trùng và xử lý lặp lại an toàn', 'Tăng số lượng câu hỏi trong bài thi', 'Mã hóa password người dùng', 'Tạo join code cho lớp học', 'A', '2026-04-01 09:04:00'),
('f0000000-0000-0000-0000-000000000006', 'Khi publish bài thi, hệ thống nên làm gì tiếp theo?', 'EXAM', 'Gửi sự kiện cho các dịch vụ liên quan', 'Xóa toàn bộ dữ liệu cũ', 'Tự động đổi mật khẩu người dùng', 'Tắt gateway để bảo trì', 'A', '2026-04-01 09:05:00'),
('f0000000-0000-0000-0000-000000000007', 'RabbitMQ được dùng chủ yếu để làm gì trong hệ thống này?', 'MESSAGING', 'Giao tiếp bất đồng bộ theo sự kiện', 'Lưu trữ ảnh bài thi', 'Thay thế hoàn toàn REST API', 'Quản lý giao diện frontend', 'A', '2026-04-01 09:06:00'),
('f0000000-0000-0000-0000-000000000008', 'Resilience4j Circuit Breaker giúp ích gì khi service phụ bị lỗi?', 'RESILIENCE', 'Ngăn lỗi lan truyền và cho phép fallback', 'Tăng dung lượng database', 'Tự động sửa dữ liệu sai', 'Xóa cache Redis', 'A', '2026-04-01 09:07:00'),
('f0000000-0000-0000-0000-000000000009', 'Endpoint nào dùng để kiểm tra trạng thái của một service?', 'OPS', '/health', '/login', '/swagger', '/metrics-only', 'A', '2026-04-01 09:08:00'),
('f0000000-0000-0000-0000-000000000010', 'Trong result-service, yêu cầu nào giúp chống việc người dùng bấm nộp nhiều lần?', 'RESULT', 'Idempotency-Key', 'Refresh Token', 'Join Code', 'CORS Origin', 'A', '2026-04-01 09:09:00');

INSERT INTO exam_questions (exam_id, question_id) VALUES
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'f0000000-0000-0000-0000-000000000001'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'f0000000-0000-0000-0000-000000000002'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'f0000000-0000-0000-0000-000000000003'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'f0000000-0000-0000-0000-000000000004'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'f0000000-0000-0000-0000-000000000005'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'f0000000-0000-0000-0000-000000000006'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'f0000000-0000-0000-0000-000000000007'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'f0000000-0000-0000-0000-000000000008'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'f0000000-0000-0000-0000-000000000009'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'f0000000-0000-0000-0000-000000000010');
