# Commission Quote App

A full-stack web application that captures loan details, securely calls a Commission Quote API via a backend proxy, and displays the resulting commission quote.

Built as a solution to the Bendigo Bank Full-Stack Engineering Code Challenge.

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
│   │   ├── auth/                    # AuthProvider, AuthContext (Keycloak integration)
│   │   ├── App.tsx                  # Root component
│   │   ├── useQuote.ts              # State management hook
│   │   ├── validation.ts            # Client-side form validation
│   │   ├── formatCurrency.ts        # AUD currency formatter
│   │   ├── keycloak.ts              # Keycloak JS client instance
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

- Java 21, Node.js 20+, Docker

### Environment variables

```bash
cp .env.example .env
```

Set `COMMISSION_API_KEY` to any non-empty value for local dev (the mock adapter doesn't call any real service). Everything else has sensible defaults.

### Run

```bash
make dev-full
```

Starts Keycloak in Docker (port 9090), the Spring Boot backend (port 8080), and the Vite dev server (port 5173). Open http://localhost:5173 and log in with `staff-user` / `password123`.

Press `Ctrl+C` to stop the frontend/backend, then `make docker-down` to stop Keycloak.

### Run in Docker

```bash
make docker-build   # build images (server jar built on host first)
make docker-up      # start full stack
```

| Service | URL |
|---|---|
| Frontend (Nginx) | http://localhost:80 |
| Backend | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Keycloak | http://localhost:9090 |

```bash
make docker-logs    # tail logs
make docker-down    # stop and remove containers
```

---

## API

```
POST /api/commission-quote
Authorization: Bearer <token>
Content-Type: application/json

{ "loanAmount": 50000.0, "loanTermMonths": 36, "riskBand": "low" | "medium" | "high" }
```

**200 OK:**
```json
{ "quoteId": "...", "commission": 5250.00, "totalRepayable": 55250.00 }
```

**Error:** `{ "error": "Human-readable message" }`

Commission formula: `loanAmount × riskMultiplier × (loanTermMonths / 12)`
where `low = 0.02`, `medium = 0.035`, `high = 0.05`. The mock adapter simulates ~20% random failures.

Swagger UI at http://localhost:8080/swagger-ui.html supports OAuth2 login for interactive testing.

---

## Authentication

Secured with OAuth2 / JWT via Keycloak. Every request to `/api/**` requires a valid Bearer token.

| | |
|---|---|
| Realm | `commission-app` |
| Test user | `staff-user` / `password123` |
| Admin console | http://localhost:9090/admin (admin / admin) |

Get a token and call the API:

```bash
TOKEN=$(curl -s -X POST http://localhost:9090/realms/commission-app/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=commission-app-client&username=staff-user&password=password123" \
  | jq -r '.access_token')

curl -s -X POST http://localhost:8080/api/commission-quote \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"loanAmount": 50000, "loanTermMonths": 36, "riskBand": "medium"}'
```

---

## Tests

```bash
make test           # backend + frontend unit tests
make server-test    # backend only
make client-test    # frontend only
make e2e            # Playwright (requires servers running via make dev-full)
make docker-e2e     # Playwright against Docker stack (requires make docker-up)
```

| Layer | Tests |
|---|---|
| Domain (RiskBand, LoanDetails, CommissionCalculator) | 16 |
| Application service (QuoteApplicationService) | 5 |
| Mock adapter (MockCommissionApiAdapter) | 6 |
| Controller (@WebMvcTest) | 10 |
| Integration (@SpringBootTest, dev profile) | 8 |
| Frontend — validation, formatting, hook, components | 53 |
| E2E (Playwright) | 6 |
| **Total** | **104** |

---

## AI Usage

Built with AI assistance (Kiro IDE and Claude) for boilerplate, scaffolding, and tests. All generated code was reviewed for correctness, security, and architectural alignment. Spec documents in `/.kiro/specs/commission-quote-app/`.
