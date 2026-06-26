# ==============================================
# Frontend — Dockerfile
# Replace this template with your actual setup
# ==============================================
#
# Examples for common frontend frameworks:
#
# ── React / Vue / Angular (Node-based) ──
# FROM node:20-alpine AS builder
# WORKDIR /app
# COPY package*.json ./
# RUN npm ci
# COPY . .
# RUN npm run build
#
# FROM nginx:alpine
# COPY --from=builder /app/dist /usr/share/nginx/html
# EXPOSE 3000
# CMD ["nginx", "-g", "daemon off;"]
#
# ── Simple static site ──
# FROM nginx:alpine
# COPY src/ /usr/share/nginx/html
# EXPOSE 3000
# CMD ["nginx", "-g", "daemon off;"]
#
# ── Next.js / Nuxt.js (SSR) ──
# FROM node:20-alpine
# WORKDIR /app
# COPY package*.json ./
# RUN npm ci
# COPY . .
# RUN npm run build
# EXPOSE 3000
# CMD ["npm", "start"]

# TODO: Uncomment one of the examples above and adapt it,
#       or write your own Dockerfile for your chosen stack.
FROM node:20-alpine AS builder
WORKDIR /app

# Install deps first for better caching
COPY package.json ./
RUN npm install

COPY . .
RUN npm run build

FROM nginx:alpine

# Serve on port 3000 to match docker-compose `ports: ${FRONTEND_PORT}:3000`
EXPOSE 3000

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

CMD ["nginx", "-g", "daemon off;"]
