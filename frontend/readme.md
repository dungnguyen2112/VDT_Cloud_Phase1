# Frontend

## Overview

Đây là module frontend của hệ thống microservices. Frontend cung cấp giao diện người dùng và giao tiếp với các backend service thông qua API Gateway.

## Tech Stack

| Component        | Choice               |
|------------------|----------------------|
| Framework        | React                |
| Styling          | CSS                  |
| Package Manager  | npm                  |
| Build Tool       | Vite                 |

## Getting Started

```bash
# From project root
docker compose up frontend --build

# Or run locally (adapt to your stack)
cd frontend
npm install
npm run dev
```

## Project Structure

```
frontend/
├── Dockerfile
├── readme.md
└── src/           # Your source code goes here
```

## Environment Variables

| Variable            | Description                | Default                  |
|---------------------|----------------------------|--------------------------|
| `VITE_API_BASE_URL` | URL của API Gateway        | `http://localhost:8080`  |

## Build for Production

```bash
# Example:
npm run build
```

## Notes

- Tất cả API call nên đi qua **API Gateway** (`gateway`), không gọi trực tiếp đến từng service.
- Cấu hình proxy hoặc API base URL trỏ về gateway.
