# Auth Service

> Dịch vụ xác thực người dùng, đăng ký/đăng nhập, JWT, hồ sơ cá nhân và tra cứu người dùng phục vụ quản lý lớp.

## Overview

- **Business domain**: Tài khoản và phiên đăng nhập cho hệ thống quiz online (giảng viên / sinh viên / quản trị).
- **Data owned**: Dữ liệu người dùng trong MySQL (auth DB), mật khẩu băm, trạng thái xác minh email (theo schema dự án).
- **Operations exposed**: Đăng ký, xác minh email, gửi lại mã xác minh, đăng nhập, hồ sơ cá nhân, tra cứu người dùng theo email/id; `GET /health` để health check.

## Tech Stack

| Component | Choice |
|-----------|--------|
| Language  | Java 17 |
| Framework | Spring Boot 3 (Web, Security, Data JPA) |
| Database  | MySQL |

JWT luôn được ký bằng **HS256** để gateway và các service khác (Nimbus `MacAlgorithm.HS256`) có thể xác thực đồng nhất. Nếu secret quá dài, JJWT có thể tự chọn HS384 theo mặc định.

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Health check → `{"status":"ok"}` |
| POST | `/api/v1/auth/register` | Đăng ký (public) |
| POST | `/api/v1/auth/verify-email` | Xác minh email (public) |
| POST | `/api/v1/auth/resend-verification` | Gửi lại mã xác minh email (public) |
| POST | `/api/v1/auth/login` | Đăng nhập, trả về JWT (public) |
| GET | `/api/v1/auth/me` | Hồ sơ người dùng hiện tại (JWT) |
| GET | `/api/v1/auth/lookup-user?email=` | Tra `userId` theo email (`ADMIN`, `INSTRUCTOR`) |
| GET | `/api/v1/auth/lookup-user-by-id?userId=` | Tra người dùng theo id (`ADMIN`, `INSTRUCTOR`, `STUDENT`) |

> Đặc tả API đầy đủ: [`docs/api-specs/auth-service.yaml`](../../docs/api-specs/auth-service.yaml)

## Running Locally

```bash
# From project root (Docker)
docker compose up auth-service --build

# Or run with Maven
cd services/auth-service
mvn spring-boot:run
```

Swagger UI (default local port): `http://localhost:8081/swagger-ui/index.html`

## Project Structure

```
auth-service/
├── Dockerfile
├── pom.xml
├── readme.md
└── src/
    ├── main/
    │   ├── java/com/quiz/auth/
    │   │   ├── AuthServiceApplication.java
    │   │   ├── config/
    │   │   │   ├── OpenApiConfig.java
    │   │   │   ├── RabbitMqConfig.java
    │   │   │   └── SecurityConfig.java
    │   │   ├── controller/
    │   │   │   ├── AuthController.java
    │   │   │   └── HealthController.java
    │   │   ├── dto/
    │   │   │   ├── AuthResponse.java
    │   │   │   ├── BaseResponse.java
    │   │   │   ├── LoginRequest.java
    │   │   │   ├── RegisterRequest.java
    │   │   │   ├── ResendVerificationRequest.java
    │   │   │   ├── RegistrationPendingResponse.java
    │   │   │   ├── UserLookupResponse.java
    │   │   │   ├── UserProfileResponse.java
    │   │   │   └── VerifyEmailRequest.java
    │   │   ├── entity/
    │   │   │   ├── User.java
    │   │   │   └── UserRole.java
    │   │   ├── event/
    │   │   │   └── EmailVerificationRequestedEvent.java
    │   │   ├── exception/
    │   │   │   ├── ApiError.java
    │   │   │   ├── BadRequestException.java
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   └── ResourceNotFoundException.java
    │   │   ├── repository/
    │   │   │   └── UserRepository.java
    │   │   ├── security/
    │   │   │   ├── CustomUserDetailsService.java
    │   │   │   ├── JwtAuthenticationFilter.java
    │   │   │   └── JwtService.java
    │   │   └── service/
    │   │       ├── AuthService.java
    │   │       └── AuthServiceImpl.java
    │   └── resources/
    │       └── application.yml
    └── test/
        ├── java/com/quiz/auth/
        │   ├── AuthServiceApplicationTests.java
        │   └── controller/HealthControllerTest.java
        └── resources/
            └── application.yml
```

## Environment Variables

| Variable | Description | Default (typical) |
|----------|-------------|-------------------|
| `SERVER_PORT` | Cổng HTTP | `8081` |
| `DB_URL` | JDBC MySQL | xem `application.yml` |
| `DB_USERNAME` | Người dùng DB | `root` |
| `DB_PASSWORD` | Mật khẩu DB | *(bắt buộc qua `.env`)* |
| `JWT_SECRET` | Khóa ký JWT | *(bắt buộc)* |
| `JWT_EXPIRATION_MS` | Thời gian sống token (TTL) | `86400000` |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | Eureka | `http://eureka-server:8761/eureka/` |
| `RABBITMQ_*` | RabbitMQ (sự kiện đăng ký user) | guest/guest trên compose |

