# Exam Service

> Dịch vụ vòng đời đề thi (draft/publish), gắn câu hỏi, phiên làm bài lưu trên Redis và sự kiện `exam.created`.

## Overview

- **Business domain**: Tạo/cập nhật/xóa đề thi, publish đề, cho sinh viên bắt đầu lượt làm bài, liệt kê câu hỏi theo đề.
- **Data owned**: Dữ liệu đề thi và liên kết đề-câu hỏi trong MySQL; dữ liệu phiên làm bài trong Redis.
- **Operations exposed**: CRUD metadata đề thi, publish, start attempt, gắn câu hỏi (single/bulk), lấy câu hỏi; API nội bộ cho result-service; `GET /health`.

## Tech Stack

| Component | Choice |
|-----------|--------|
| Language  | Java 21 |
| Framework | Spring Boot 3 (Web, Data JPA, OAuth2 Resource Server, OpenFeign, AMQP) |
| Database  | MySQL |
| Cache     | Redis (session / exam start) |

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Health check → `{"status":"ok"}` |
| POST | `/api/v1/exams` | Tạo đề thi (`INSTRUCTOR`, mặc định `DRAFT`) |
| GET | `/api/v1/exams` | Danh sách đề thi (JWT) |
| GET | `/api/v1/exams/my-classes` | Đề thi theo lớp của người dùng (JWT) |
| GET | `/api/v1/exams/{examId}` | Chi tiết đề thi (JWT) |
| POST | `/api/v1/exams/{examId}/publish` | Publish + gửi thông báo (`INSTRUCTOR`) |
| PATCH | `/api/v1/exams/{examId}` | Cập nhật metadata (`INSTRUCTOR`) |
| DELETE | `/api/v1/exams/{examId}` | Xóa đề nháp (`INSTRUCTOR`) |
| POST | `/api/v1/exams/{examId}/start` | Bắt đầu lượt làm bài (`STUDENT`, Redis) |
| POST | `/api/v1/exams/{examId}/questions` | Gắn một câu hỏi (`INSTRUCTOR`) |
| POST | `/api/v1/exams/{examId}/questions/bulk` | Gắn nhiều câu hỏi (`INSTRUCTOR`) |
| GET | `/api/v1/exams/{examId}/questions` | Danh sách câu hỏi của đề (JWT; gọi question-service) |
| GET | `/api/v1/exams/circuit-breakers` | Snapshot Resilience4j CB (public; qua gateway: `/api/exam/circuit-breakers`) |

*(Các route dưới `/api/v1/internal/**` dùng `X-Service-Token`; xem code và OpenAPI.)*

> Đặc tả API đầy đủ: [`docs/api-specs/exam-service.yaml`](../../docs/api-specs/exam-service.yaml)

## Running Locally

```bash
# From project root (Docker)
docker compose up exam-service --build

# Or run with Maven
cd services/exam-service
mvn spring-boot:run
```

Swagger UI (default container port): `http://localhost:8082/swagger-ui/index.html` (map host port if needed)

## Project Structure

```
exam-service/
├── Dockerfile
├── pom.xml
├── readme.md
└── src/
    ├── main/java/com/quiz/exam/...
    └── main/resources/application.yml
```

## Environment Variables

| Variable | Description | Default (typical) |
|----------|-------------|-------------------|
| `SERVER_PORT` | Cổng HTTP | `8082` |
| `DB_URL` | JDBC MySQL | see `application.yml` |
| `DB_USERNAME` / `DB_PASSWORD` | Thông tin xác thực DB | *(`.env`)* |
| `JWT_SECRET` | Khóa xác thực JWT (resource server) | *(required)* |
| `REDIS_HOST` / `REDIS_PORT` | Kho phiên làm bài | `redis` / `6379` |
| `RABBITMQ_*` | Publish sự kiện `exam.created` | guest |
| `INTERNAL_SERVICE_TOKEN` | Token nội bộ (cùng giá trị với result-service) | *(required in production)* |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | Eureka | `http://eureka-server:8761/eureka/` |

