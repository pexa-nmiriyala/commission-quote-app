.PHONY: dev test build e2e \
        client-dev client-test client-build \
        server-dev server-test server-build \
        docker-build docker-up docker-down docker-logs \
        docker-test docker-e2e docker-restart

# ── Local dev (no Docker) ────────────────────────────────────────────────────

dev: client-dev server-dev

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
	cd server && SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun

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
docker-up:
	@echo "Starting Docker stack..."
	docker compose up -d
	@echo ""
	@echo "  Frontend: http://localhost:80"
	@echo "  Backend:  http://localhost:8080"
	@echo "  Swagger:  http://localhost:8080/swagger-ui.html"
	@echo ""
	@echo "Run 'make docker-logs' to tail logs."

## Stop and remove containers
docker-down:
	@echo "Stopping Docker stack..."
	docker compose down

## Tail logs from all running containers
docker-logs:
	docker compose logs -f

## Rebuild images and restart the stack
docker-restart: docker-down docker-build docker-up

## Run backend + frontend tests inside Docker
docker-test:
	@echo "Running tests in Docker..."
	docker compose -f docker-compose.test.yml run --rm server-test
	docker compose -f docker-compose.test.yml run --rm client-test

## Run Playwright E2E tests against the running Docker stack
## Requires: make docker-up (stack must be running first)
docker-e2e:
	@echo "Running Playwright E2E against Docker stack..."
	cd client && USE_DOCKER=true npx playwright test
