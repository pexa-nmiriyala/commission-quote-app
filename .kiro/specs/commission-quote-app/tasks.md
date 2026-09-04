# Implementation Plan: Commission Quote App

## Overview

Implement the Commission Quote App as a React (Vite) frontend + Spring Boot (Kotlin) backend. The backend follows Domain-Driven Design (DDD) with a Hexagonal Architecture (Ports and Adapters). The frontend renders a loan details form, a loading indicator, a quote result panel, and an error message. Backend tests use JUnit 5 and MockK. Frontend tests use Jest and React Testing Library. E2E tests use Playwright.

---

## Tasks

- [x] 1. Scaffold project structure and tooling
  - Initialise `/client` as a Vite + React + TypeScript project
  - Initialise `/server` as a Gradle Spring Boot 3.x project using the Kotlin DSL (`build.gradle.kts`) with dependencies: `spring-boot-starter-web`, `spring-boot-starter-webflux` (for WebClient), `jackson-module-kotlin`, `kotlin-reflect`
  - Add test dependencies to `build.gradle.kts`: `spring-boot-starter-test`, `mockk`, `springmockk`
  - Create the full DDD hexagonal package skeleton under `server/src/main/kotlin/com/example/commissionquote/`: `domain/model/`, `domain/service/`, `domain/port/inbound/`, `domain/port/outbound/`, `application/`, `adapter/inbound/web/`, `adapter/outbound/http/`, `adapter/outbound/mock/`
  - Install Jest and testing dependencies in `/client`:
    - `jest`, `ts-jest`, `@types/jest`, `jest-environment-jsdom`
    - `@testing-library/react`, `@testing-library/jest-dom`, `@testing-library/user-event`
    - `jest-axe`
  - Create `client/jest.config.ts`:
    ```typescript
    export default {
      preset: 'ts-jest',
      testEnvironment: 'jsdom',
      setupFilesAfterFramework: ['@testing-library/jest-dom'],
    }
    ```
  - Add a root-level `Makefile` with `dev`, `test`, and `build` targets for both modules
    - `test` target: `./gradlew test` (server) and `cd client && npx jest --runInBand` (client)
  - Configure Tailwind CSS (or plain CSS) in the client
  - Create `server/src/main/resources/application.yml` with `server.port: 8080` and a placeholder `commission.api.url` property
  - Add a `.env.example` file documenting `COMMISSION_API_KEY=`
  - Add `springdoc-openapi-starter-webmvc-ui` dependency to `server/build.gradle.kts`
  - Add `server/src/main/resources/openapi.yaml` — the hand-authored OpenAPI 3.0 contract-first YAML file documenting the `/api/commission-quote` endpoint (see design document for full content)
  - Add `springdoc` configuration block to `server/src/main/resources/application.yml`:
    ```yaml
    springdoc:
      swagger-ui:
        path: /swagger-ui.html
      api-docs:
        path: /v3/api-docs
    ```
  - _Requirements: 2.1, 2.3_

- [x] 2. Domain layer — Value Objects, Enum, and Domain Service
  - [x] 2.1 Create `RiskBand.kt` in `server/src/main/kotlin/com/example/commissionquote/domain/model/`
    - `enum class RiskBand(val multiplier: Double)` with entries `LOW(0.02)`, `MEDIUM(0.035)`, `HIGH(0.05)`
    - Companion object `fun from(value: String): RiskBand` — case-insensitive lookup; throws `IllegalArgumentException("Invalid riskBand: $value")` for unrecognised inputs
    - _Requirements: 6.4_
  - [x] 2.2 Create `LoanDetails.kt` in `server/src/main/kotlin/com/example/commissionquote/domain/model/`
    - `data class LoanDetails(val loanAmount: Double, val loanTermMonths: Int, val riskBand: RiskBand)`
    - `init` block: `require(loanAmount > 0) { "loanAmount must be positive" }` and `require(loanTermMonths > 0) { "loanTermMonths must be a positive integer" }`
    - _Requirements: 1.5, 1.6, 1.7, 3.1_
  - [x] 2.3 Create `QuoteResult.kt` in `server/src/main/kotlin/com/example/commissionquote/domain/model/`
    - `data class QuoteResult(val quoteId: String, val commission: Double, val totalRepayable: Double)`
    - _Requirements: 3.2, 6.6_
  - [x] 2.4 Create `CommissionCalculator.kt` in `server/src/main/kotlin/com/example/commissionquote/domain/service/`
    - `@Service class CommissionCalculator` with `fun calculate(details: LoanDetails): Pair<Double, Double>`
    - Formula: `commission = loanAmount × riskBand.multiplier × (loanTermMonths / 12.0)`, `totalRepayable = loanAmount + commission`
    - Annotated with `@Service` so Spring registers it as a bean (enabling constructor injection into `MockCommissionApiAdapter`); the logic itself has no I/O or framework coupling — it remains a pure function
    - _Requirements: 6.4_
  - [ ]* 2.5 Write pure unit tests for domain model (`DomainModelTest.kt`)
    - `RiskBand.from()` with every valid string (upper/lower/mixed case) → correct enum
    - `RiskBand.from()` with invalid strings → `IllegalArgumentException`
    - `LoanDetails` with `loanAmount ≤ 0` → `IllegalArgumentException`
    - `LoanDetails` with `loanTermMonths ≤ 0` → `IllegalArgumentException`
    - `CommissionCalculator.calculate()` with representative inputs (e.g. `50000, 36, MEDIUM` → `(5250.0, 55250.0)`) → expected `(commission, totalRepayable)` pairs
    - No Spring context loaded
    - _Requirements: 1.5, 1.6, 1.7, 6.4_

- [x] 3. Domain layer — Ports (interfaces)
  - [x] 3.1 Create `QuoteUseCase.kt` in `server/src/main/kotlin/com/example/commissionquote/domain/port/inbound/`
    - `interface QuoteUseCase { fun generateQuote(details: LoanDetails): QuoteResult }`
    - _Requirements: 3.1, 3.2_
  - [x] 3.2 Create `CommissionApiPort.kt` in `server/src/main/kotlin/com/example/commissionquote/domain/port/outbound/`
    - `interface CommissionApiPort { fun fetchQuote(details: LoanDetails): QuoteResult }`
    - _Requirements: 3.1, 3.2_

- [x] 4. Application layer — QuoteApplicationService
  - [x] 4.1 Create `QuoteApplicationService.kt` in `server/src/main/kotlin/com/example/commissionquote/application/`
    - `@Service class QuoteApplicationService(private val commissionApiPort: CommissionApiPort, @Value("\${COMMISSION_API_KEY:}") private val apiKey: String) : QuoteUseCase`
    - The `apiKey` value is injected directly via `@Value` (Spring reads it from the environment); `AppConfig.kt` owns the startup WARN log if blank and the `WebClient` bean — the service should not duplicate that logic
    - `override fun generateQuote(details: LoanDetails): QuoteResult`:
      - If `apiKey.isBlank()` → throw `ConfigurationException("Server configuration error")`
      - Otherwise delegate to `commissionApiPort.fetchQuote(details)`
    - _Requirements: 2.1, 2.4, 3.1_
  - [ ]* 4.2 Write MockK unit tests for `QuoteApplicationService` (`QuoteApplicationServiceTest.kt`)
    - Use `@ExtendWith(MockKExtension::class)`, no Spring context
    - Test: blank API key → `ConfigurationException` thrown
    - Test: `commissionApiPort.fetchQuote()` returns `QuoteResult` → service returns same `QuoteResult`
    - Test: `commissionApiPort.fetchQuote()` throws infrastructure exception → exception propagates
    - _Requirements: 2.4, 3.1, 3.2_

- [x] 5. Outbound adapters
  - [x] 5.1 Create `MockCommissionApiAdapter.kt` in `server/src/main/kotlin/com/example/commissionquote/adapter/outbound/mock/`
    - `@Profile("dev") @Service class MockCommissionApiAdapter(private val calculator: CommissionCalculator) : CommissionApiPort`
    - `override fun fetchQuote(details: LoanDetails): QuoteResult`:
      - Simulate ~20% random failures by throwing `UpstreamApiException("Commission API error")` using `Math.random() < 0.2`
      - Compute `(commission, totalRepayable)` via `calculator.calculate(details)`
      - Return `QuoteResult(quoteId = UUID.randomUUID().toString(), commission = commission, totalRepayable = totalRepayable)`
    - _Requirements: 6.3, 6.4, 6.5, 6.6_
  - [ ]* 5.2 Write unit tests for `MockCommissionApiAdapter` (`MockCommissionApiAdapterTest.kt`)
    - Test: call with valid `LoanDetails` → returns `QuoteResult` with non-blank `quoteId`, `commission > 0`, `totalRepayable > loanAmount`
    - Test: formula matches `CommissionCalculator.calculate()` output (stub random failure to always pass)
    - Statistical test: call 100 times (random failure not stubbed); assert failure count is between 10 and 30
    - Test: each successful response has a unique `quoteId`
    - _Requirements: 6.3, 6.4, 6.5, 6.6_
  - [x] 5.3 Create `CommissionApiAdapter.kt` in `server/src/main/kotlin/com/example/commissionquote/adapter/outbound/http/`
    - `@ConditionalOnMissingBean(CommissionApiPort::class) @Service class CommissionApiAdapter(...) : CommissionApiPort`
    - This means: if no other `CommissionApiPort` bean is present (i.e. dev profile is not active), use this real adapter. Prefer this over `@Profile("!dev")` for robustness in test contexts.
    - Inject `WebClient` with a 10-second response timeout and `@Value("\${commission.api.url}")` base URL
    - `override fun fetchQuote(details: LoanDetails): QuoteResult`:
      - POST to the configured URL with `api-key` header and JSON body built from `details`
      - Validate upstream response contains `quoteId`, `commission`, `totalRepayable`; throw `InvalidResponseException` if not
      - Map upstream 401 → throw `UnauthorisedException`
      - Map upstream 5xx → throw `UpstreamApiException`
      - Map timeout (`WebClientRequestException` / `TimeoutException`) → throw `TimeoutException`
    - _Requirements: 2.1, 2.2, 3.1, 3.2, 3.3, 3.4, 3.5_
  - [ ]* 5.4 Write MockK unit tests for `CommissionApiAdapter` (`CommissionApiAdapterTest.kt`)
    - Mock `WebClient` chain for each scenario
    - Test: upstream 200 with valid body → returns `QuoteResult`
    - Test: upstream 200 with missing `quoteId` → throws `InvalidResponseException`
    - Test: upstream 401 → throws `UnauthorisedException`
    - Test: upstream 500 → throws `UpstreamApiException`
    - Test: timeout → throws `TimeoutException`
    - Test: outbound request to mock URL has `api-key` header injected — verify `CommissionApiAdapter` always sets the `api-key` header regardless of payload (covers Requirement 6.2; the mock adapter does not perform this check because header injection is the responsibility of the outbound HTTP adapter)
    - _Requirements: 2.2, 3.3, 3.4, 3.5, 6.2_

- [x] 6. Inbound adapter — QuoteController and DTOs
  - [x] 6.1 Create `QuoteRequest.kt` in `server/src/main/kotlin/com/example/commissionquote/adapter/inbound/web/`
    - `data class QuoteRequest(@field:NotNull @field:Positive @Schema(description = "Loan amount in dollars", example = "50000.0", minimum = "0.01") val loanAmount: Double?, @field:NotNull @field:Positive @Schema(description = "Loan term in months", example = "36", minimum = "1") val loanTermMonths: Int?, @field:NotBlank @Schema(description = "Risk band", example = "medium", allowableValues = ["low", "medium", "high"]) val riskBand: String?)`
    - Note: `@Positive` alone does NOT reject null — `@NotNull` is required alongside it for the null case to trigger a 400
    - _Requirements: 1.1, 1.2, 1.3_
  - [x] 6.2 Create `QuoteResponse.kt` in `server/src/main/kotlin/com/example/commissionquote/adapter/inbound/web/`
    - `data class QuoteResponse(@Schema(description = "Unique quote identifier") val quoteId: String, @Schema(description = "Commission amount in dollars", example = "583.33") val commission: Double, @Schema(description = "Total repayable amount in dollars", example = "50583.33") val totalRepayable: Double)`
    - _Requirements: 3.2_
  - [x] 6.3 Create `QuoteController.kt` in `server/src/main/kotlin/com/example/commissionquote/adapter/inbound/web/`
    - Annotate the class with `@Tag(name = "Quote", description = "Commission quote generation")`
    - Annotate `generateQuote` with `@Operation(summary = "Generate a commission quote", description = "Accepts loan details and returns a commission quote via the backend proxy")` and the full `@ApiResponses` block covering 200, 400, 401, 500, 502, 504 (see design document for exact annotation)
    - `@RestController class QuoteController(private val quoteUseCase: QuoteUseCase)`
    - `@PostMapping("/api/commission-quote") fun generateQuote(@RequestBody @Valid request: QuoteRequest): ResponseEntity<QuoteResponse>`:
      - Map `QuoteRequest` → `LoanDetails` (calls `RiskBand.from(request.riskBand!!)`)
      - Call `quoteUseCase.generateQuote(loanDetails)`
      - Map `QuoteResult` → `QuoteResponse`; return `ResponseEntity.ok(response)`
    - `@ExceptionHandler` methods:
      - `IllegalArgumentException`, `MethodArgumentNotValidException` → `400 { "error": "..." }`
      - `ConfigurationException` → `500 { "error": "Server configuration error" }`
      - `UnauthorisedException` → `401 { "error": "Unauthorised — check API key" }`
      - `UpstreamApiException` → `502 { "error": "Commission API error" }`
      - `TimeoutException` → `504 { "error": "Request timed out" }`
      - `InvalidResponseException` → `502 { "error": "Invalid response from API" }`
    - _Requirements: 2.4, 3.1, 3.2, 3.3, 3.4, 3.5_
  - [x] 6.4 Create `ErrorResponse.kt` in `server/src/main/kotlin/com/example/commissionquote/adapter/inbound/web/` (not in domain)
    - `data class ErrorResponse(@Schema(description = "Human-readable error message") val error: String)`
    - This DTO lives in the adapter layer alongside `QuoteRequest` and `QuoteResponse`; the domain layer must not depend on OpenAPI/Swagger libraries
  - [x] 6.5 Create `AppConfig.kt` and `OpenApiConfig.kt` in `server/src/main/kotlin/com/example/commissionquote/config/`
    - `AppConfig.kt`:
      - `@Configuration class AppConfig(@Value("\${COMMISSION_API_KEY:}") val apiKey: String)`
      - `@PostConstruct fun validateApiKey()` — log WARN if `apiKey.isBlank()`
      - `@Bean fun webClient(builder: WebClient.Builder): WebClient` — apply 10-second response timeout
    - `OpenApiConfig.kt`:
      ```kotlin
      @Configuration
      class OpenApiConfig {
          @Bean
          fun openApiInfo(): OpenAPI = OpenAPI()
              .info(Info()
                  .title("Commission Quote API")
                  .version("1.0.0")
                  .description("Backend proxy for generating commission quotes on loan applications"))
      }
      ```
  - [ ]* 6.6 Write `@WebMvcTest` tests for `QuoteController` (`QuoteControllerTest.kt`)
    - `@WebMvcTest(QuoteController::class)` with `@MockkBean QuoteUseCase`
    - Test: valid request → `quoteUseCase.generateQuote()` called with correct `LoanDetails` → 200 with `QuoteResponse` JSON
    - Test: `loanAmount` missing → 400 (Bean Validation)
    - Test: `loanTermMonths` = 0 → 400 (Bean Validation)
    - Test: `riskBand = "invalid"` → 400 (`IllegalArgumentException` from `RiskBand.from()`)
    - Test: `quoteUseCase` throws `ConfigurationException` → 500
    - Test: `quoteUseCase` throws `UnauthorisedException` → 401
    - Test: `quoteUseCase` throws `UpstreamApiException` → 502
    - Test: `quoteUseCase` throws `TimeoutException` → 504
    - Test: `quoteUseCase` throws `InvalidResponseException` → 502
    - _Requirements: 1.5, 1.6, 1.7, 2.4, 3.2, 3.3, 3.4, 3.5_

- [x] 7. Wire up the Spring Boot application
  - [x] 7.1 Create `CommissionQuoteApplication.kt` in `server/src/main/kotlin/com/example/commissionquote/`
    - `@SpringBootApplication` entry point with `fun main(args: Array<String>) = runApplication<CommissionQuoteApplication>(*args)`
    - _Requirements: 2.1_
  - [x] 7.2 Configure `application.yml` and `application-dev.yml`:
    - `server/src/main/resources/application.yml`:
      - `server.port: 8080`
      - `commission.api.url` pointing to the real external API URL placeholder (e.g. `https://api.example.com/commission-quote`) — overridable via environment variable for prod
      - `spring.profiles.active: dev` for local development default
      - Verify the `springdoc` block is present (added in Task 1): `springdoc.swagger-ui.path: /swagger-ui.html` and `springdoc.api-docs.path: /v3/api-docs`
    - `server/src/main/resources/application-dev.yml`:
      - Add a comment: "Not used in dev — MockCommissionApiAdapter is injected directly; no HTTP call is made"
      - Override `commission.api.url` with a local placeholder or comment it out to make it explicit that the URL is unused when the dev profile is active
    - _Requirements: 2.1, 2.4_
  - [x] 7.3 Configure `vite.config.ts` in `/client` to proxy `/api/*` requests to `http://localhost:8080`
    - _Requirements: 3.1_
  - [ ]* 7.4 Write smoke test for startup configuration (`StartupIntegrationTest.kt`)
    - `@SpringBootTest` without `dev` profile, `COMMISSION_API_KEY` unset; POST to `/api/commission-quote`; verify HTTP 500 response
    - _Requirements: 2.4_

- [x] 8. Checkpoint — backend tests pass
  - Ensure all server-side unit and integration tests pass (`./gradlew test`). Ask the user if any questions arise.

- [x] 9. Implement frontend validation logic
  - [x] 9.1 Create `client/src/types.ts` with `LoanDetails`, `QuoteResult`, `RequestStatus`, and `AppState` TypeScript interfaces
    - _Requirements: 1.1, 1.2, 1.3, 4.1_
  - [x] 9.2 Create `client/src/validation.ts` with pure validation functions
    - `validateLoanAmount(value: unknown): string | null` — returns error string or null
    - `validateLoanTermMonths(value: unknown): string | null`
    - `validateRiskBand(value: unknown): string | null`
    - _Requirements: 1.5, 1.6, 1.7_
  - [ ]* 9.3 Write unit tests for all three validation functions (`validation.test.ts`)
    - `validateLoanAmount`: zero → error; negative → error; NaN → error; empty string → error; valid positive float → null
    - `validateLoanTermMonths`: zero → error; negative → error; float (e.g. 1.5) → error; non-numeric string → error; valid positive integer → null
    - `validateRiskBand`: arbitrary string (e.g. "unknown") → error; "low" / "medium" / "high" → null
    - _Requirements: 1.5, 1.6, 1.7_

- [x] 10. Implement the currency formatting utility
  - [x] 10.1 Create `client/src/formatCurrency.ts`
    - Use `Intl.NumberFormat('en-AU', { style: 'currency', currency: 'AUD' })` to format numbers as AUD currency strings
    - _Requirements: 4.2_
  - [ ]* 10.2 Write unit tests for currency formatting (`formatCurrency.test.ts`)
    - Test with representative finite numbers: `0` → `"A$0.00"`, `583.33` → `"A$583.33"`, `50000` → `"A$50,000.00"`
    - Assert all results are non-empty strings containing the AUD currency symbol
    - Assert the same input always produces the same output (determinism)
    - _Requirements: 4.2_

- [x] 11. Implement frontend application state management
  - [x] 11.1 Create `client/src/useQuote.ts` — a custom React hook managing `AppState` (`status`, `quoteResult`, `errorMessage`)
    - Expose `submitQuote(details: LoanDetails): Promise<void>`
    - On submit: set `status = 'loading'`, clear `quoteResult` and `errorMessage`
    - On success: set `status = 'success'`, set `quoteResult`, clear `errorMessage`
    - On error: set `status = 'error'`, set `errorMessage`, clear `quoteResult`
    - Enforce invariant: `quoteResult` and `errorMessage` are never both non-null
    - _Requirements: 4.3, 5.1, 5.3, 5.4, 5.5, 5.6_
  - [ ]* 11.2 Write unit tests for `useQuote` state transitions (`useQuote.test.ts`)
    - Mock `fetch`; trigger success → assert `status = 'success'`, `quoteResult` set, `errorMessage` null
    - Mock `fetch`; trigger error → assert `status = 'error'`, `errorMessage` set, `quoteResult` null
    - Trigger success then error → assert `quoteResult` is cleared on error
    - Trigger error then success → assert `errorMessage` is cleared on success
    - Assert `quoteResult` and `errorMessage` are never both non-null after any transition
    - _Requirements: 4.3, 5.5_

- [x] 12. Implement React UI components
  - [x] 12.1 Implement `QuoteForm` component (`client/src/components/QuoteForm.tsx`)
    - Render inputs for `loanAmount`, `loanTermMonths`, and `riskBand` (select)
    - Associate each input with a visible `<label>` (htmlFor / id)
    - Attach validation on submit using `client/src/validation.ts` functions
    - Display inline validation errors with `role="alert"` or `aria-live="polite"` for screen readers
    - Disable the "Generate Quote" button when `status === 'loading'`
    - Call `onSubmit(loanDetails)` with validated data when the form is valid
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 7.2, 7.3_
  - [x] 12.2 Implement `LoadingIndicator` component (`client/src/components/LoadingIndicator.tsx`)
    - Render a spinner or text indicator with `aria-label="Loading"`
    - _Requirements: 5.1_
  - [x] 12.3 Implement `QuoteResult` component (`client/src/components/QuoteResult.tsx`)
    - Display `quoteId`, formatted `commission`, and formatted `totalRepayable`
    - Use `formatCurrency` from task 10
    - Provide sufficient colour contrast (WCAG AA — verify with jest-axe)
    - _Requirements: 4.1, 4.2, 7.4_
  - [x] 12.4 Implement `ErrorMessage` component (`client/src/components/ErrorMessage.tsx`)
    - Display the error string with `role="alert"` for screen reader accessibility
    - _Requirements: 5.3, 5.4_
  - [ ]* 12.5 Write unit tests for QuoteForm rendering and accessibility
    - Verify each input is present and labelled
    - Verify submit button is disabled during loading
    - Verify validation errors are shown and have correct ARIA attributes
    - Run `jest-axe` on the rendered form
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.8, 7.2, 7.3_
  - [ ]* 12.6 Write unit tests for QuoteResult rendering
    - Render `QuoteResult` with a representative `QuoteResult` object; assert `quoteId`, formatted `commission`, and formatted `totalRepayable` are all visible in the output
    - _Requirements: 4.1_
  - [ ]* 12.7 Write unit tests for error and loading state display
    - `status = 'loading'` → LoadingIndicator visible, QuoteResult hidden
    - `status = 'error'` → ErrorMessage visible, QuoteResult hidden
    - `status = 'success'` → QuoteResult visible, ErrorMessage hidden
    - _Requirements: 5.1, 5.2, 5.5_

- [x] 13. Wire up the App component
  - [x] 13.1 Create `client/src/App.tsx` — root component
    - Integrate `useQuote` hook
    - Render `QuoteForm` with `onSubmit` and `isLoading` props
    - Conditionally render `LoadingIndicator`, `QuoteResult`, and `ErrorMessage` based on `status`
    - _Requirements: 4.3, 5.1, 5.2, 5.3, 5.5, 5.6_
  - [ ]* 13.2 Write integration test for the full client-side flow
    - Mock `fetch`; simulate success → verify QuoteResult rendered
    - Mock `fetch`; simulate 500 error → verify ErrorMessage rendered
    - Simulate new submit after error → verify errorMessage cleared
    - _Requirements: 4.3, 5.3, 5.6_

- [x] 14. Checkpoint — all frontend tests pass
  - Ensure all client-side unit and integration tests pass (`npx jest --runInBand`). Ask the user if any questions arise.

- [x] 15. End-to-end integration and final wiring
  - [x] 15.1 Verify `vite.config.ts` proxy configuration forwards `/api/*` requests to `http://localhost:8080`
    - _Requirements: 3.1_
  - [x] 15.2 Write a Spring Boot integration test (`@SpringBootTest(webEnvironment = RANDOM_PORT)`) that POSTs to `/api/commission-quote` with `dev` profile active and asserts the response shape
    - _Requirements: 3.1, 3.2, 4.1_
  - [ ]* 15.3 Write integration test for the missing-API-key path through the full server
    - Run `@SpringBootTest` without `dev` profile; `COMMISSION_API_KEY` absent; POST to `/api/commission-quote`; verify HTTP 500
    - _Requirements: 2.4_
  - [ ]* 15.4 Write an OpenAPI spec integration test (`OpenApiSpecTest.kt`)
    - `@SpringBootTest(webEnvironment = RANDOM_PORT)` with `dev` profile active
    - GET `/v3/api-docs`; assert HTTP 200 and `Content-Type: application/json`
    - Parse the response body as JSON; assert it contains a `paths` object with key `/api/commission-quote`
    - Assert the `info.title` field equals `"Commission Quote API"`
    - _Requirements: (non-functional — API documentation correctness)_

- [x] 16. Final checkpoint — all tests pass
  - Run the complete test suite (server: `./gradlew test`; client: `npx jest --runInBand`). Ensure all tests pass. Ask the user if any questions arise.

- [x] 17. E2E tests (Playwright)
  - [x] 17.1 Install and configure Playwright
    - Install `@playwright/test` in `/client`
    - Create `client/playwright.config.ts`:
      - `webServer[0]`: start Spring Boot (`./gradlew bootRun` in `server/`) on port 8080, wait for it
      - `webServer[1]`: start Vite dev server (`npm run dev` in `client/`) on port 5173, wait for it
      - `use.baseURL: 'http://localhost:5173'`
      - `testDir: './e2e'`
    - Add `e2e/` directory to `/client`
    - Add `make e2e` target to root Makefile: `cd client && npx playwright test`
  - [x] 17.2 Write happy path E2E test (`e2e/quote.spec.ts`)
    - Navigate to `/`
    - Fill `loanAmount` with `50000`, `loanTermMonths` with `36`, select `riskBand` = `medium`
    - Click "Generate Quote"
    - Assert the quote result panel is visible
    - Assert it contains a `quoteId` string
    - Assert `commission` is displayed as AUD currency (e.g. matches `/A\$[\d,]+\.\d{2}/`)
    - Assert `totalRepayable` is displayed as AUD currency
    - _Requirements: 1.4, 3.1, 3.2, 4.1, 4.2_
  - [x] 17.3 Write validation error E2E test
    - Navigate to `/`
    - Leave `loanAmount` empty
    - Click "Generate Quote"
    - Assert an inline validation error message is visible for `loanAmount`
    - Assert no quote result panel is visible
    - _Requirements: 1.5, 5.5_
  - [x] 17.4 Write API error E2E test
    - Use Playwright route intercept to return `{ "error": "Commission API error" }` with status 502 for `POST /api/commission-quote`
    - Fill the form with valid data and submit
    - Assert the error message panel is visible with a human-readable message
    - Assert no quote result is shown
    - _Requirements: 5.3, 5.5_
  - [x] 17.5 Write loading state E2E test
    - Use Playwright route intercept to delay the API response by 1 second
    - Fill the form and click "Generate Quote"
    - Assert loading indicator is visible before the response completes
    - Assert "Generate Quote" button is disabled during loading
    - _Requirements: 1.8, 5.1, 5.2_
  - [x] 17.6 Write retry-after-error E2E test
    - Trigger an error (via route intercept returning 502)
    - Assert error message is shown
    - Release the route intercept (allow real/mock response)
    - Submit the form again
    - Assert error message is cleared and quote result is shown
    - _Requirements: 5.6, 4.3_

---

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP; the core implementation remains correct without them.
- The API key must never appear in any HTTP response body sent to the browser.
- Domain objects (`LoanDetails`, `QuoteResult`) must never be used directly as HTTP serialisation targets; always map through `QuoteRequest` / `QuoteResponse` DTOs at the adapter layer.
- Full WCAG AA compliance requires manual testing with assistive technologies beyond what `jest-axe` can catch automatically.

## Task Dependency Graph

```json
{
  "waves": [
    { "wave": 1, "tasks": ["1"] },
    { "wave": 2, "tasks": ["2", "3"] },
    { "wave": 3, "tasks": ["4"] },
    { "wave": 4, "tasks": ["5", "9", "10"] },
    { "wave": 5, "tasks": ["6", "11"] },
    { "wave": 6, "tasks": ["7", "12"] },
    { "wave": 7, "tasks": ["8", "13"] },
    { "wave": 8, "tasks": ["14"] },
    { "wave": 9, "tasks": ["15"] },
    { "wave": 10, "tasks": ["16"] },
    { "wave": 11, "tasks": ["17"] }
  ]
}
```

- Tasks 2 and 3 (domain model + ports) are prerequisites for all subsequent backend tasks and can be parallelised with each other.
- Task 4 (application layer) depends on tasks 2 and 3.
- Task 5 (outbound adapters) depends on task 4 (outbound port is defined in task 3, but the application service wires the dependency).
- Task 6 (inbound adapter) depends on tasks 3 and 4.
- Tasks 9–11 (frontend logic) can proceed in parallel with backend tasks 5–7 after task 1.
- Each test sub-task depends on its parent implementation sub-task.
- Checkpoints (8, 14, 16) gate progression between phases.
- Task 17 (Playwright E2E) depends on Task 16 (final checkpoint) — requires the full stack to be working end-to-end.
