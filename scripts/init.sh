#!/usr/bin/env bash
# ==============================================
# Project Initialization Script
# Usage: bash scripts/init.sh
# ==============================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

compose_cmd=()

load_env_file() {
  if [ -f "$PROJECT_DIR/.env" ]; then
    set -a
    # shellcheck disable=SC1090
    . "$PROJECT_DIR/.env"
    set +a
  fi
}

wait_for_mysql() {
  local service_name="$1"
  local root_password="$2"
  local max_attempts=60
  local attempt=1

  echo "⏳ Waiting for $service_name to accept MySQL connections..."
  until "${compose_cmd[@]}" exec -T "$service_name" sh -c "mysqladmin ping -uroot -p\"$root_password\" --silent" >/dev/null 2>&1; do
    if [ "$attempt" -ge "$max_attempts" ]; then
      echo "❌ Timed out waiting for $service_name"
      exit 1
    fi

    attempt=$((attempt + 1))
    sleep 5
  done

  echo "✅ $service_name is ready"
}

seed_database() {
  local service_name="$1"
  local root_password="$2"
  local database_name="$3"
  local seed_file="$4"

  echo "🌱 Seeding $service_name from $(basename "$seed_file")"
  "${compose_cmd[@]}" exec -T "$service_name" mysql -uroot -p"$root_password" "$database_name" < "$seed_file"
}

echo "🚀 Initializing Microservices Project..."
echo "==========================================="

# --- 1. Environment file ---
if [ ! -f "$PROJECT_DIR/.env" ]; then
  cp "$PROJECT_DIR/.env.example" "$PROJECT_DIR/.env"
  echo "✅ Created .env from .env.example"
else
  echo "ℹ️  .env already exists, skipping"
fi

load_env_file

# --- 2. Check Docker ---
if command -v docker &> /dev/null; then
  echo "✅ Docker found: $(docker --version)"
else
  echo "❌ Docker not found. Please install Docker Desktop."
  echo "   https://docs.docker.com/get-docker/"
  exit 1
fi

# --- 3. Check Docker Compose ---
if docker compose version &> /dev/null; then
  echo "✅ Docker Compose found: $(docker compose version --short)"
  compose_cmd=(docker compose)
elif command -v docker-compose &> /dev/null; then
  echo "✅ Docker Compose (legacy) found: $(docker-compose --version)"
  compose_cmd=(docker-compose)
else
  echo "❌ Docker Compose not found."
  exit 1
fi

# --- 4. Build and start containers ---
echo ""
echo "📦 Building and starting containers..."
cd "$PROJECT_DIR"
"${compose_cmd[@]}" up -d --build

# --- 5. Seed demo data ---
echo ""
echo "🧪 Seeding demo data..."
wait_for_mysql "auth-db" "${AUTH_DB_ROOT_PASSWORD}"
wait_for_mysql "class-db" "${CLASS_DB_ROOT_PASSWORD}"
wait_for_mysql "exam-db" "${EXAM_DB_ROOT_PASSWORD}"
wait_for_mysql "question-db" "${QUESTION_DB_ROOT_PASSWORD}"
wait_for_mysql "result-db" "${RESULT_DB_ROOT_PASSWORD}"
wait_for_mysql "notification-db" "${NOTIFICATION_DB_ROOT_PASSWORD}"

seed_database "auth-db" "${AUTH_DB_ROOT_PASSWORD}" "${AUTH_DB_NAME}" "$PROJECT_DIR/scripts/seed-data/01-auth-seed.sql"
seed_database "class-db" "${CLASS_DB_ROOT_PASSWORD}" "${CLASS_DB_NAME}" "$PROJECT_DIR/scripts/seed-data/02-class-seed.sql"
seed_database "exam-db" "${EXAM_DB_ROOT_PASSWORD}" "${EXAM_DB_NAME}" "$PROJECT_DIR/scripts/seed-data/04-exam-seed.sql"
seed_database "question-db" "${QUESTION_DB_ROOT_PASSWORD}" "${QUESTION_DB_NAME}" "$PROJECT_DIR/scripts/seed-data/03-question-seed.sql"
seed_database "result-db" "${RESULT_DB_ROOT_PASSWORD}" "${RESULT_DB_NAME}" "$PROJECT_DIR/scripts/seed-data/05-result-seed.sql"
seed_database "notification-db" "${NOTIFICATION_DB_ROOT_PASSWORD}" "${NOTIFICATION_DB_NAME}" "$PROJECT_DIR/scripts/seed-data/06-notification-seed.sql"

echo ""
echo "==========================================="
echo "✅ Project initialized and seeded successfully!"
echo ""
echo "Next steps:"
echo "  1. Open the app at the gateway and frontend ports from .env"
echo "  2. Tail logs with: docker compose logs -f"
echo "  3. Restart with: docker compose up -d"
echo "  4. Reset everything with: docker compose down -v"
echo "==========================================="