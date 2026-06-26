# Question Service

> Dịch vụ ngân hàng câu hỏi (CRUD, lọc/tìm kiếm theo category), import/sinh câu hỏi bằng AI và cung cấp đáp án cho chấm điểm.

## Overview

Question Service quản lý ngân hàng câu hỏi cho hệ thống quiz online.
- Sở hữu dữ liệu câu hỏi và ánh xạ đề thi-câu hỏi.
- Cung cấp API để tạo, import, sinh và truy vấn câu hỏi.
- Tích hợp với MySQL, Redis và Eureka service discovery.

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
| POST | /api/v1/questions | Tạo câu hỏi |
| GET | /api/v1/questions/bank | Tìm kiếm/liệt kê ngân hàng câu hỏi |
| POST | /api/v1/questions/import | Import câu hỏi từ file |
| POST | /api/v1/questions/generate | Sinh câu hỏi theo chủ đề |
| GET | /api/v1/questions/{id} | Lấy câu hỏi theo id |
| PUT | /api/v1/questions/{id} | Cập nhật câu hỏi |
| DELETE | /api/v1/questions/{id} | Xóa câu hỏi |
| GET | /api/v1/questions/exam/{examId} | Lấy câu hỏi theo exam id |
| GET | /api/v1/questions/exam/{examId}/answers | Lấy đáp án theo exam id |

> Đặc tả API đầy đủ: [docs/api-specs/question-service.yaml](../../docs/api-specs/question-service.yaml)

## Running Locally

```bash
# From project root
docker compose up question-service --build

# Or run standalone
cd services/question-service
mvn spring-boot:run
```

Swagger UI: `http://localhost:8083/swagger-ui/index.html`

## Project Structure

```
question-service/
|-- Dockerfile
|-- pom.xml
|-- readme.md
`-- src/
    |-- main/
    |   |-- java/com/quiz/question/
    |   |   |-- QuestionServiceApplication.java
    |   |   |-- config/
    |   |   |   |-- OpenApiConfig.java
    |   |   |   |-- RedisCacheConfig.java
    |   |   |   `-- SecurityConfig.java
    |   |   |-- controller/
    |   |   |   |-- HealthController.java
    |   |   |   `-- QuestionController.java
    |   |   |-- dto/
    |   |   |   |-- BaseResponse.java
    |   |   |   |-- CreateQuestionRequest.java
    |   |   |   |-- GenerateQuestionRequest.java
    |   |   |   |-- QuestionAnswerResponse.java
    |   |   |   `-- QuestionResponse.java
    |   |   |-- entity/
    |   |   |   `-- Question.java
    |   |   |-- exception/
    |   |   |   |-- ApiError.java
    |   |   |   |-- GlobalExceptionHandler.java
    |   |   |   `-- ResourceNotFoundException.java
    |   |   |-- mapper/
    |   |   |   `-- QuestionMapper.java
    |   |   |-- repository/
    |   |   |   `-- QuestionRepository.java
    |   |   `-- service/
    |   |       |-- GeminiQuestionGenerator.java
    |   |       |-- OpenAiQuestionGenerator.java
    |   |       |-- QuestionService.java
    |   |       `-- QuestionServiceImpl.java
    |   `-- resources/
    |       `-- application.yaml
    `-- test/
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| SERVER_PORT | Cổng HTTP | 8083 |
| DB_URL | JDBC URL tới question database | jdbc:mysql://localhost:3306/question-service-db?... |
| DB_USERNAME | Tên người dùng database | required |
| DB_PASSWORD | Mật khẩu database | required |
| REDIS_HOST | Host Redis | localhost |
| REDIS_PORT | Cổng Redis | 6379 |
| EUREKA_SERVER_URL | URL Eureka server | http://eureka-server:8761/eureka/ |
| JWT_SECRET | Khóa ký JWT | change-me-super-secret-key-change-me-super-secret-key |
| AI_PROVIDER | Bộ chọn nhà cung cấp AI | gemini |
| GEMINI_API_KEY | API key Gemini | empty |
| GEMINI_MODEL | Tên model Gemini | gemini-3.1-flash-lite-preview |
| OPENAI_API_KEY | API key OpenAI | empty |
| OPENAI_MODEL | Tên model OpenAI | gpt-4.1-mini |

