.PHONY: dev dev-full dev-down test build e2e \
        client-dev client-test client-build \
        server-dev server-test server-build \
        docker-build docker-up docker-down docker-logs \
        docker-test docker-e2e docker-restart

# ── Local dev (no Docker) ────────────────────────────────────────────────────

dev: client-dev server-dev

## Kill any local dev processes holding ports 8080 (Spring Boot) and 5173 (Vite).
## Run this before switching between 'make dev-full' and 'make docker-up'.
dev-down:
	@echo "Stopping local dev processes on ports 8080 and 5173..."
	@lsof -ti:8080 | xargs kill -9 2>/dev/null || true
	@lsof -ti:5173 | xargs kill -9 2>/dev/null || true
	@echo "Done."

## Start Keycloak (Docker), backend, and frontend in one command.
## Keycloak runs in Docker on port 9090; backend and frontend run on the host.
## Use Ctrl+C to stop the frontend/backend; then run 'make docker-down' to stop Keycloak.
dev-full: dev-down
	@echo "Starting Keycloak..."
	docker compose up -d keycloak
	@echo "Waiting for Keycloak to be healthy..."
	@until docker compose ps keycloak | grep -q "healthy"; do sleep 2; done
	@echo "Keycloak ready."
	@echo "Starting backend..."
	set -a && [ -f .env ] && . ./.env; set +a; cd server && SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun &
	@echo "Starting frontend..."
	cd client && npm run dev

test: client-test server-test

build: client-build server-build

e2e:
	@echo "Running Playwright E2E tests (local)..."
	cd client && npx playwright test

# ── Client (Vite + React + TypeScript) ──────────────────────────────────────

client-dev:
	cd client && npm run dev

client-test:
	cd client && npx vitest run

client-build:
	cd client && npm run build

# ── Server (Spring Boot + Kotlin + Gradle) ──────────────────────────────────

server-dev:
	set -a && [ -f ../.env ] && . ../.env; set +a; cd server && SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun

server-test:
	cd server && ./gradlew test

server-build:
	cd server && ./gradlew build

# ── Docker ───────────────────────────────────────────────────────────────────

## Build all Docker images (builds artifacts on host first, then packages into images)
docker-build:
	@echo "Building server jar..."
	cd server && ./gradlew bootJar --no-daemon -x test
	@echo "Building Docker images..."
	docker compose build

## Start the full stack in Docker (server + client)
## App will be available at http://localhost:80
## API at http://localhost:8080
docker-up: dev-down
	@echo "Starting Docker stack..."
	docker compose up -d
	@echo ""
	@echo "  Frontend: http://localhost:80"
	@echo "  Backend:  http://localhost:8080"
	@echo "  Swagger:  http://localhost:8080/swagger-ui.html"
	@echo ""
	@echo "Run 'make docker-logs' to tail logs."

## Stop and remove containers, and kill any host processes on ports 8080 and 5173
docker-down:
	@echo "Stopping Docker stack..."
	docker compose down
	@echo "Stopping any host processes on ports 8080 and 5173..."
	@lsof -ti:8080 | xargs kill -9 2>/dev/null || true
	@lsof -ti:5173 | xargs kill -9 2>/dev/null || true
	@echo "Done."

## Tail logs from all running containers
docker-logs:
	docker compose logs -f

## Rebuild images and restart the stack
docker-restart: docker-down docker-build docker-up

## Run tests: backend on host (Gradle), frontend (Vitest) in Docker
## Note: server tests run on the host because the Gradle wrapper requires
## network access to download its distribution, which is unavailable inside
## the Docker build environment.
docker-test:
	@echo "Running server tests on host..."
	cd server && ./gradlew test
	@echo "Running client tests in Docker..."
	docker compose -f docker-compose.test.yml run --rm client-test

## Run Playwright E2E tests against the running Docker stack
## Requires: make docker-up (stack must be running first)
docker-e2e:
	@echo "Running Playwright E2E against Docker stack..."
	cd client && USE_DOCKER=true npx playwright test
