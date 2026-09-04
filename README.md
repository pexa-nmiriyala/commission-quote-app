# Commission Quote App

A full-stack web application that captures loan details, securely calls a Commission Quote API via a backend proxy, and displays the resulting commission quote.

Built as a solution to the Bendigo Bank Full-Stack Engineering Code Challenge.

---

## Context

In our Lending Platform, staff members frequently need to generate "Commission Quotes" based on loan applications. The system processes loan details and sends them to an external Commission Service, which calculates the commission and returns a quote.

This application builds a mock version of that API and a full-stack UI on top of it.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 19, TypeScript, Vite, Tailwind CSS |
| Backend | Spring Boot 3.x, Kotlin, Gradle |
| Architecture | DDD + Hexagonal (Ports and Adapters) |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Frontend Tests | Vitest, React Testing Library |
| Backend Tests | JUnit 5, MockK, SpringMockK |
| E2E Tests | Playwright |
| Code Formatting | Prettier (frontend), ktlint (backend) |

---

## Project Structure

```
commission-quote-app/
├── client/                          # React (Vite) frontend
│   ├── src/
│   │   ├── components/              # QuoteForm, QuoteResult, ErrorMessage, LoadingIndicator
│   │   ├── App.tsx                  # Root component
│   │   ├── useQuote.ts              # State management hook
│   │   ├── validation.ts            # Client-side form validation
│   │   ├── formatCurrency.ts        # AUD currency formatter
│   │   └── types.ts                 # Shared TypeScript interfaces
│   ├── e2e/                         # Playwright E2E tests
│   ├── playwright.config.ts
│   └── vite.config.ts               # Dev proxy: /api/* → localhost:8080
│
├── server/                          # Spring Boot (Kotlin) backend
│   └── src/main/kotlin/.../
│       ├── domain/                  # Value objects, domain service, ports
│       │   ├── model/               # LoanDetails, QuoteResult, RiskBand
│       │   ├── service/             # CommissionCalculator
│       │   └── port/                # QuoteUseCase (inbound), CommissionApiPort (outbound)
│       ├── application/             # QuoteApplicationService
│       ├── adapter/
│       │   ├── inbound/web/         # QuoteController, DTOs, exception handlers
│       │   └── outbound/
│       │       ├── http/            # CommissionApiAdapter (real WebClient)
│       │       └── mock/            # MockCommissionApiAdapter (dev profile)
│       └── config/                  # AppConfig (WebClient), OpenApiConfig
│
├── Makefile                         # Top-level dev/test/build commands
└── .env.example                     # Required environment variables
```

---

## Getting Started

### Prerequisites

- Java 21
- Node.js 20+
- Gradle (or use the `./gradlew` wrapper)

### Environment variables

Copy `.env.example` and set your API key:

```bash
cp .env.example .env
```

```
COMMISSION_API_KEY=your-api-key-here
```

> In `dev` profile, the mock adapter is used and does not validate the API key at the adapter level. The key is still required by `QuoteApplicationService` to prevent misconfigured deployments.

### Run locally

```bash
make dev
```

This starts:
- Spring Boot backend on `http://localhost:8080` (`dev` profile — uses mock adapter)
- Vite dev server on `http://localhost:5173` (proxies `/api/*` to backend)

Open `http://localhost:5173` in your browser.

---

## Commission Quote API Spec

The backend exposes a single endpoint:

```
POST /api/commission-quote
Content-Type: application/json

{
  "loanAmount": 50000.0,
  "loanTermMonths": 36,
  "riskBand": "low" | "medium" | "high"
}
```

**Success (200):**
```json
{
  "quoteId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "commission": 5250.00,
  "totalRepayable": 55250.00
}
```

**Error (4xx/5xx):**
```json
{
  "error": "Human-readable error message"
}
```

**Security:** Requests require an `api-key` header. The key is injected server-side from the `COMMISSION_API_KEY` environment variable — it is never sent to the browser.

### Commission formula (mock)

```
commission     = loanAmount × riskMultiplier × (loanTermMonths / 12)
totalRepayable = loanAmount + commission

riskMultiplier: low = 0.02 | medium = 0.035 | high = 0.05
```

The mock adapter simulates ~20% random failures to mimic real-world API instability.

### Swagger UI

When the server is running:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

---

## Running with Docker

### Prerequisites
- Docker 24+ and Docker Compose v2
- Java 21 and Node.js 20+ (required for the host build step — see below)

### How the Docker build works

The server jar is built on the host before being packaged into the Docker image. This avoids Gradle needing network access inside the Alpine container to download its own distribution.

```
host: ./gradlew bootJar  →  server/build/libs/*.jar
                                  ↓
Docker:  COPY build/libs/*.jar app.jar  →  eclipse-temurin:21-jre-alpine
```

The client is built entirely inside Docker (Node.js is available in the `node:20-alpine` build stage).

### Start the full stack

```bash
make docker-build   # builds server jar on host, then packages both images
make docker-up      # start server + client in background
```

| Service | URL |
|---|---|
| Frontend (Nginx) | http://localhost:80 |
| Backend (Spring Boot) | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |

```bash
make docker-logs    # tail combined logs
make docker-down    # stop and remove containers
make docker-restart # rebuild images and restart
```

### Run tests

```bash
make docker-test    # server tests on host (Gradle) + client tests in Docker (Vitest)
```

> Server tests run on the host via `./gradlew test` because the Gradle wrapper
> requires downloading its distribution, which is unavailable inside the Docker
> build environment. Client tests run inside `node:20-alpine`.

### Run E2E against Docker stack

```bash
make docker-build   # build images (if not already built)
make docker-up      # stack must be running first
make docker-e2e     # Playwright hits http://localhost:80
make docker-down    # tear down when done
```

### Environment variables in Docker

The `COMMISSION_API_KEY` is passed through from your shell environment. If not set, it defaults to `dev-api-key` (fine for local dev with the mock adapter).

```bash
COMMISSION_API_KEY=my-key make docker-up
```

Or add it to a `.env` file at the project root — Docker Compose picks it up automatically.

---

## Running Tests

```bash
# All tests (backend + frontend)
make test

# Backend only
make server-test

# Frontend only
make client-test

# E2E (requires both servers running)
make e2e
```

**Test coverage:**

| Layer | Tests |
|---|---|
| Domain (RiskBand, LoanDetails, CommissionCalculator) | 16 |
| Application service (QuoteApplicationService) | 5 |
| Mock adapter (MockCommissionApiAdapter) | 6 |
| Controller (@WebMvcTest) | 10 |
| Integration (@SpringBootTest, dev profile) | 3 |
| Frontend — validation, formatting, hook, components | 53 |
| E2E (Playwright) | 5 |
| **Total** | **98** |

---

## Building for production

```bash
make build
```

This produces:
- `server/build/libs/commission-quote-*.jar` — executable Spring Boot jar
- `client/dist/` — static frontend assets (serve via Spring Boot or a CDN)

---

## AI Usage

This project was built with AI assistance (Kiro). AI was used to:

- Generate boilerplate project structure and configuration
- Scaffold Kotlin data classes, Spring controllers, and React components
- Write unit, integration, and E2E tests

All generated code was reviewed for correctness, security, and alignment with the architectural decisions in the spec.

The spec documents (`/.kiro/specs/commission-quote-app/`) contain the requirements, design decisions, and task breakdown used to guide implementation.
