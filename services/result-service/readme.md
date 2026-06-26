# Result Service

> Dịch vụ chấm điểm bài thi, lưu lượt làm bài, chống gian lận (violation + idempotency Redis), phát sự kiện `exam.submitted`.

## Overview

- **Business domain**: Nộp bài, báo cáo vi phạm, báo cáo điểm cho giảng viên, export CSV.
- **Data owned**: Bảng kết quả và vi phạm trong MySQL; idempotency key trong Redis.
- **Operations exposed**: Submit, timeline vi phạm, báo cáo theo đề/người dùng; `GET /health`.

## Tech Stack

| Component | Choice |
|-----------|--------|
| Language  | Java 17 |
| Framework | Spring Boot 3 (Web, Data JPA, OAuth2 Resource Server, OpenFeign, AMQP, Redis, AOP) |
| Database  | MySQL |
| Resilience | Resilience4j (calls to exam/question) |

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Health check → `{"status":"ok"}` |
| POST | `/api/v1/results/submit` | Nộp bài (`STUDENT`, **bắt buộc header `Idempotency-Key`**) |
| POST | `/api/v1/results/exam/violation` | Báo cáo vi phạm (`STUDENT`) |
| GET | `/api/v1/results/me` | Kết quả của tôi (`STUDENT` hoặc `INSTRUCTOR`) |
| GET | `/api/v1/results/{userId}` | Kết quả theo user (`INSTRUCTOR`) |
| GET | `/api/v1/results/exams/{examId}/report` | Báo cáo cho đề thi sở hữu (`INSTRUCTOR`) |
| GET | `/api/v1/results/exams/{examId}/report.csv` | Xuất CSV (`INSTRUCTOR`) |
| GET | `/api/v1/results/exams/{examId}/violations` | Timeline vi phạm, tùy chọn `?userId=` (`INSTRUCTOR`) |

**Gateway:** `/api/result/**` hoặc `/api/results/**` → `/api/v1/results/**`

> Đặc tả API đầy đủ: [`docs/api-specs/result-service.yaml`](../../docs/api-specs/result-service.yaml)

## Running Locally

```bash
# From project root (Docker)
docker compose up result-service --build

# Or run with Maven
cd services/result-service
mvn spring-boot:run
```

Swagger UI: `http://localhost:8084/swagger-ui/index.html`

## Project Structure

```
result-service/
├── Dockerfile
├── pom.xml
├── readme.md
└── src/
    ├── main/java/com/quiz/result/...
    └── main/resources/application.yml
```

## Environment Variables

| Variable | Description | Default (typical) |
|----------|-------------|-------------------|
| `SERVER_PORT` | Cổng HTTP | `8084` |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | MySQL | *(`.env`)* |
| `JWT_SECRET` | Khóa xác thực JWT | *(required)* |
| `REDIS_HOST` / `REDIS_PORT` | Idempotency | `redis` / `6379` |
| `RABBITMQ_*` | Publish sự kiện `exam.submitted` | guest |
| `INTERNAL_SERVICE_TOKEN` | Gọi exam-service nội bộ (`X-Service-Token`) | *(required)* |
| `IDEMPOTENCY_TTL_MINUTES` | TTL của idempotency key | `15` |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | Eureka | `http://eureka-server:8761/eureka/` |

