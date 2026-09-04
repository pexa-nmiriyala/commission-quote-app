.PHONY: dev dev-full dev-down check-prereqs test build e2e \
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

## Check that required tools are installed and meet minimum version requirements.
check-prereqs:
	@echo "Checking prerequisites..."
	@command -v java >/dev/null 2>&1 || { echo "❌ Java not found. Please install Java 21."; exit 1; }
	@JAVA_VER=$$(java -version 2>&1 | awk -F'"' '/version/ {print $$2}' | cut -d'.' -f1); \
	  if [ "$$JAVA_VER" -lt 21 ]; then \
	    echo "❌ Java 21 required, found Java $$JAVA_VER. Please upgrade."; exit 1; \
	  else \
	    echo "✅ Java $$JAVA_VER"; \
	  fi
	@command -v node >/dev/null 2>&1 || { echo "❌ Node.js not found. Please install Node.js 20.19+ or 22.12+."; exit 1; }
	@NODE_VER=$$(node -e "process.stdout.write(process.version.slice(1))"); \
	  MAJOR=$$(echo $$NODE_VER | cut -d'.' -f1); \
	  MINOR=$$(echo $$NODE_VER | cut -d'.' -f2); \
	  OK=0; \
	  if [ "$$MAJOR" -eq 20 ] && [ "$$MINOR" -ge 19 ]; then OK=1; fi; \
	  if [ "$$MAJOR" -eq 22 ] && [ "$$MINOR" -ge 12 ]; then OK=1; fi; \
	  if [ "$$MAJOR" -gt 22 ]; then OK=1; fi; \
	  if [ "$$OK" -eq 0 ]; then \
	    echo "❌ Node.js 20.19+ or 22.12+ required, found v$$NODE_VER. Please upgrade."; exit 1; \
	  else \
	    echo "✅ Node.js v$$NODE_VER"; \
	  fi
	@command -v docker >/dev/null 2>&1 || { echo "❌ Docker not found. Please install Docker."; exit 1; }
	@docker info >/dev/null 2>&1 || { echo "❌ Docker is not running. Please start Docker."; exit 1; }
	@echo "✅ Docker"
	@echo "All prerequisites met."

## Start Keycloak (Docker), backend, and frontend in one command.
## Keycloak runs in Docker on port 9090; backend and frontend run on the host.
## Use Ctrl+C to stop the frontend/backend; then run 'make docker-down' to stop Keycloak.
dev-full: dev-down check-prereqs
	@echo "Starting Keycloak..."
	docker compose up -d keycloak
	@echo "Waiting for Keycloak to be healthy..."
	@until docker compose ps keycloak | grep -q "healthy"; do sleep 2; done
	@echo "Keycloak ready."
	@echo "Installing frontend dependencies..."
	cd client && npm install
	@echo "Starting backend (logs → /tmp/server.log)..."
	set -a && [ -f .env ] && . ./.env; set +a; cd server && SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun > /tmp/server.log 2>&1 &
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
