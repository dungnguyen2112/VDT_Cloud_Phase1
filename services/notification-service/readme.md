# Notification Service

> Dịch vụ lắng nghe sự kiện RabbitMQ, lưu thông báo in-app và gửi email qua SMTP.

## Overview

- **Business domain**: Thông báo cho nộp bài thi, tạo đề thi, thêm user vào lớp, đăng ký user mới (theo queue đã cấu hình).
- **Data owned**: Bản ghi thông báo trong MySQL.
- **Operations exposed**: `GET /api/v1/notifications/me` (JWT); `GET /health`; các consumer chạy nền (không phải REST).

## Tech Stack

| Component | Choice |
|-----------|--------|
| Language  | Java 17 |
| Framework | Spring Boot 3 (Web, Data JPA, AMQP, Mail, OAuth2 Resource Server) |
| Database  | MySQL |
| Messaging | RabbitMQ |

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Health check → `{"status":"ok"}` |
| GET | `/api/v1/notifications/me` | Thông báo của người dùng đã đăng nhập (JWT). Qua gateway: `/api/notification/me` |

**Consumers (RabbitMQ)**

- Exchange: `exam.events` (direct)
- Queues / routing keys: xem `application.yaml` (`exam.submitted`, `exam.created`, `class.user.added`, `auth.user.registered`, …)

> Đặc tả API đầy đủ: [`docs/api-specs/notification-service.yaml`](../../docs/api-specs/notification-service.yaml)

## Running Locally

```bash
# From project root (Docker; requires RabbitMQ + DB)
docker compose up notification-service --build

# Or run with Maven
cd services/notification-service
mvn spring-boot:run
```

## Project Structure

```
notification-service/
├── Dockerfile
├── pom.xml
├── readme.md
└── src/
    ├── main/java/com/quiz/notification/...
    └── main/resources/application.yaml
```

## Environment Variables

| Variable | Description | Default (typical) |
|----------|-------------|-------------------|
| `SERVER_PORT` | Cổng HTTP | `8086` |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | MySQL | *(`.env`)* |
| `JPA_DDL_AUTO` | Hibernate | `validate` |
| `JWT_SECRET` | Khóa xác thực JWT (giống auth) | *(required)* |
| `RABBITMQ_*` | Kết nối broker | guest |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP | xem `application.yaml` |
| `MAIL_FROM_EMAIL` / `MAIL_FROM_NAME` | Người gửi | *(required)* |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | Eureka | `http://eureka-server:8761/eureka/` |

