# Class Service

## Overview

Class Service quản lý lớp học và thành viên lớp.
- Sở hữu dữ liệu lớp, join code và ánh xạ user-lớp.
- Cung cấp API cho CRUD lớp, tham gia lớp bằng mã và tra cứu danh sách lớp.
- Phát sự kiện liên quan thành viên lớp lên RabbitMQ.

## Tech Stack

| Component  | Choice |
|------------|--------|
| Language   | Java 21 |
| Framework  | Spring Boot |
| Database   | MySQL 8 |
| Build Tool | Maven |

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /health | Kiểm tra sức khỏe |
| POST | /classes | Tạo lớp học |
| GET | /classes | Danh sách lớp |
| GET | /classes/{id} | Chi tiết lớp |
| POST | /classes/join | Tham gia lớp bằng join code |
| POST | /classes/{id}/regenerate-join-code | Tạo lại join code |
| POST | /classes/{id}/add-user | Thêm người dùng vào lớp |
| GET | /classes/{id}/students | Danh sách sinh viên của lớp |
| GET | /users/{userId}/classes | Danh sách lớp theo user |

> Đặc tả API đầy đủ: [docs/api-specs/class-service.yaml](../../docs/api-specs/class-service.yaml)

## Running Locally

```bash
# From project root
docker compose up class-service --build

# Or run standalone
cd services/class-service
mvn spring-boot:run
```

## Project Structure

```text
class-service/
|-- Dockerfile
|-- pom.xml
|-- readme.md
`-- src/
    |-- main/
    |   |-- java/com/quiz/classservice/
    |   |   |-- ClassServiceApplication.java
    |   |   |-- config/
    |   |   |   |-- OpenApiConfig.java
    |   |   |   |-- RabbitMqConfig.java
    |   |   |   `-- SecurityConfig.java
    |   |   |-- controller/
    |   |   |   |-- ClassController.java
    |   |   |   `-- HealthController.java
    |   |   |-- dto/
    |   |   |   |-- AddUserToClassRequest.java
    |   |   |   |-- BaseResponse.java
    |   |   |   |-- ClassResponse.java
    |   |   |   |-- CreateClassRequest.java
    |   |   |   |-- JoinClassRequest.java
    |   |   |   `-- StudentResponse.java
    |   |   |-- entity/
    |   |   |   |-- Classroom.java
    |   |   |   |-- UserClass.java
    |   |   |   `-- UserClassId.java
    |   |   |-- event/
    |   |   |   |-- UserAddedToClassEvent.java
    |   |   |   `-- UserAddedToClassPublisher.java
    |   |   |-- exception/
    |   |   |   |-- BadRequestException.java
    |   |   |   |-- ConflictException.java
    |   |   |   |-- GlobalExceptionHandler.java
    |   |   |   `-- ResourceNotFoundException.java
    |   |   |-- repository/
    |   |   |   |-- ClassroomRepository.java
    |   |   |   `-- UserClassRepository.java
    |   |   |-- util/
    |   |   |   `-- JoinCodeGenerator.java
    |   |   `-- service/
    |   |       |-- ClassService.java
    |   |       `-- ClassServiceImpl.java
    |   `-- resources/
    |       `-- application.yml
    `-- test/
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| SERVER_PORT | Cổng HTTP | 8087 |
| DB_URL | JDBC URL tới class database | jdbc:mysql://localhost:3311/class_service_db?... |
| DB_USERNAME | Tên người dùng database | root |
| DB_PASSWORD | Mật khẩu database | root |
| RABBITMQ_HOST | Host RabbitMQ | localhost |
| RABBITMQ_PORT | Cổng RabbitMQ | 5672 |
| RABBITMQ_USERNAME | Tên người dùng RabbitMQ | guest |
| RABBITMQ_PASSWORD | Mật khẩu RabbitMQ | guest |
| EUREKA_CLIENT_SERVICEURL_DEFAULTZONE | URL Eureka server | http://eureka-server:8761/eureka/ |
| JWT_SECRET | Khóa ký JWT | change-me-super-secret-key-change-me-super-secret-key |
| CLASS_JOIN_URL_BASE | Base URL tùy chọn cho join link/QR | empty |

