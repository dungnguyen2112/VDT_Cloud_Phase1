# API Gateway

## Overview

API Gateway là điểm vào duy nhất cho tất cả request từ client. Gateway định tuyến request đến microservice backend phù hợp.

## Responsibilities

- **Request routing**: Chuyển request đến đúng service đích
- **Load balancing**: Phân phối lưu lượng truy cập (nếu áp dụng)
- **Authentication**: Xác thực token/thông tin đăng nhập (tùy cấu hình)
- **Rate limiting**: Bảo vệ service khỏi quá tải (tùy cấu hình)
- **CORS handling**: Cho phép frontend gọi cross-origin
- **Request/Response transformation**: Chỉnh sửa header, path khi cần

## Tech Stack

| Component  | Choice               |
|------------|----------------------|
| Approach   | Spring Cloud Gateway |

## Routing Table

| External Path          | Target Service        | Internal URL                    |
|------------------------|-----------------------|----------------------------------|
| `/api/auth/**`         | Auth Service          | `lb://auth-service/**`           |
| `/api/class/**`        | Class Service         | `lb://class-service/**`          |
| `/api/question/**`     | Question Service      | `lb://question-service/**`       |
| `/api/exam/**`         | Exam Service          | `lb://exam-service/**`           |
| `/api/result/**`       | Result Service        | `lb://result-service/**`         |
| `/api/results/**`      | Result Service        | `lb://result-service/**`         |
| `/api/notification/**` | Notification Service  | `lb://notification-service/**`   |
| `/api/class/health`    | Class Service         | `lb://class-service/health`      |

## Running

```bash
# From project root
docker compose up gateway --build
```

## Configuration

Gateway sử dụng mạng Docker Compose. Các service được truy cập qua
service name định nghĩa trong `docker-compose.yml` (ví dụ: `auth-service`, `exam-service`, `result-service`).

## Notes

- Sử dụng service name (không dùng `localhost`) cho upstream URL bên trong Docker
- Gateway expose cổng 8080 ra máy host
