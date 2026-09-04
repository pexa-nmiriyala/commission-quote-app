.PHONY: dev test build e2e client-dev client-test client-build server-dev server-test server-build

# ── Top-level targets (run both modules) ────────────────────────────────────

dev: client-dev server-dev

test: client-test server-test

build: client-build server-build

e2e:
	@echo "Running Playwright E2E tests..."
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
