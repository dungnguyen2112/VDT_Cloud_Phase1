# Eureka Server

## Overview

Eureka Server cung cấp service registry và service discovery cho hệ thống microservices.
- Duy trì danh sách các service instance đang chạy.
- Cho phép client (gateway và các service) tìm thấy nhau theo service name.
- Hỗ trợ giám sát sức khỏe qua các actuator endpoint.

## Tech Stack

| Component  | Choice |
|------------|--------|
| Language   | Java 21 |
| Framework  | Spring Boot + Netflix Eureka Server |
| Registry   | Eureka |
| Build Tool | Maven |

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | / | Trang dashboard Eureka |
| GET | /eureka/apps | Danh sách ứng dụng đã đăng ký |
| GET | /actuator/health | Kiểm tra sức khỏe |

## Running Locally

```bash
# From project root
docker compose up eureka-server --build

# Or run standalone
cd eureka-server
mvn spring-boot:run
```

## Project Structure

```text
eureka-server/
|-- Dockerfile
|-- pom.xml
|-- readme.md
`-- src/
    |-- main/
    |   |-- java/com/example/eureka_server/
    |   |   `-- EurekaServerApplication.java
    |   `-- resources/
    |       `-- application.yaml
    `-- test/
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| SERVER_PORT | Cổng HTTP | 8761 |

## Testing

```bash
# From eureka folder
cd eureka-server
mvn test

# Health check
curl http://localhost:8761/actuator/health
```
