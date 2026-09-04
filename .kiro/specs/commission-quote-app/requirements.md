# Requirements Document

## Introduction

This document defines requirements for a Full-Stack Commission Quote Application. The app allows users to input loan details via a web form, securely calls an external Commission Quote API (mocked server-side), and displays the resulting quote or appropriate error feedback. The application must handle network failures, API errors, and invalid user inputs gracefully. Security is a key concern: the API key used to authenticate with the Commission Quote API must never be exposed to the client.

## Glossary

- **Commission Quote App**: The full-stack web application described in this document.
- **Quote Form**: The client-side form that collects loan details from the user.
- **Backend Proxy**: The server-side service that forwards quote requests to the Commission Quote API, injecting the API key.
- **Commission Quote API**: The external API (mocked) that accepts loan details and returns a commission quote.
- **Quote Result**: The response data returned by the Commission Quote API: `quoteId`, `commission`, and `totalRepayable`.
- **Risk Band**: A string category representing the risk level of a loan (e.g., "low", "medium", "high").
- **API Key**: A secret credential required to authenticate with the Commission Quote API; must never be sent to the browser.
- **Loading State**: A visual indicator shown while a quote request is in-flight.
- **Error State**: A visual indicator shown when a quote request fails for any reason.

---

## Requirements

### Requirement 1: Loan Details Form

**User Story:** As a user, I want to enter loan details into a form, so that I can request a commission quote.

#### Acceptance Criteria

1. THE Quote_Form SHALL display an input field for `loanAmount` that accepts positive numeric values.
2. THE Quote_Form SHALL display an input field for `loanTermMonths` that accepts positive integer values.
3. THE Quote_Form SHALL display a selector or input field for `riskBand` that accepts one of the predefined string values: "low", "medium", or "high".
4. THE Quote_Form SHALL display a "Generate Quote" button that triggers quote generation when clicked.
5. WHEN a user submits the form with a `loanAmount` that is not a positive number, THE Quote_Form SHALL display a validation error message and prevent submission.
6. WHEN a user submits the form with a `loanTermMonths` that is not a positive integer, THE Quote_Form SHALL display a validation error message and prevent submission.
7. WHEN a user submits the form with a `riskBand` value that is not one of "low", "medium", or "high", THE Quote_Form SHALL display a validation error message and prevent submission.
8. WHEN a user submits a valid form, THE Quote_Form SHALL disable the "Generate Quote" button until the request completes.

---

### Requirement 2: Secure API Key Handling

**User Story:** As a system operator, I want the Commission Quote API key to be kept server-side, so that the credential is never exposed to browser clients.

#### Acceptance Criteria

1. THE Backend_Proxy SHALL read the Commission Quote API key from a server-side environment variable.
2. THE Backend_Proxy SHALL inject the `api-key` header into all outbound requests to the Commission Quote API.
3. THE Commission_Quote_App SHALL NOT include the API key in any client-side JavaScript bundle, HTML, or HTTP response body sent to the browser.
4. IF the API key environment variable is not set at startup, THEN THE Backend_Proxy SHALL log a configuration error and reject all quote requests with an HTTP 500 response.

---

### Requirement 3: Commission Quote API Integration

**User Story:** As a user, I want my loan details forwarded to the Commission Quote API, so that I receive an accurate commission quote.

#### Acceptance Criteria

1. WHEN a valid quote request is received by the Backend_Proxy, THE Backend_Proxy SHALL forward a POST request to `/api/commission-quote` with the JSON body `{ "loanAmount": number, "loanTermMonths": number, "riskBand": string }` and the `api-key` header.
2. WHEN the Commission Quote API returns a successful response, THE Backend_Proxy SHALL return the `quoteId`, `commission`, and `totalRepayable` fields to the client with HTTP 200.
3. WHEN the Commission Quote API returns an error response (4xx or 5xx), THE Backend_Proxy SHALL return an appropriate error message to the client.
4. WHEN the Commission Quote API does not respond within 10 seconds, THE Backend_Proxy SHALL return a timeout error to the client.
5. THE Backend_Proxy SHALL validate that the response from the Commission Quote API contains `quoteId`, `commission`, and `totalRepayable` before forwarding to the client.

---

### Requirement 4: Quote Result Display

**User Story:** As a user, I want to see the commission quote result after submitting my loan details, so that I can understand the cost of the loan.

#### Acceptance Criteria

1. WHEN the Backend_Proxy returns a successful quote response, THE Commission_Quote_App SHALL display `quoteId`, `commission`, and `totalRepayable` to the user.
2. WHEN a quote result is displayed, THE Commission_Quote_App SHALL format `commission` and `totalRepayable` as currency values.
3. WHEN a new quote request is initiated, THE Commission_Quote_App SHALL clear any previously displayed quote result.

---

### Requirement 5: Loading and Error States

**User Story:** As a user, I want clear feedback during quote generation and when errors occur, so that I understand the application state at all times.

#### Acceptance Criteria

1. WHEN a quote request is in-flight, THE Commission_Quote_App SHALL display a visible loading indicator.
2. WHEN a quote request is in-flight, THE Commission_Quote_App SHALL hide or disable the quote result display area.
3. WHEN the Backend_Proxy returns an error response, THE Commission_Quote_App SHALL display a human-readable error message.
4. WHEN a network error occurs before reaching the Backend_Proxy, THE Commission_Quote_App SHALL display a human-readable connectivity error message.
5. WHEN an error is displayed, THE Commission_Quote_App SHALL not display stale quote result data alongside the error.
6. WHEN a new quote request is initiated after a previous error, THE Commission_Quote_App SHALL clear the previous error message.

---

### Requirement 6: Mock Commission Quote API

**User Story:** As a developer, I want a mock implementation of the Commission Quote API, so that I can develop and test the application without a live external service.

#### Acceptance Criteria

1. THE Mock_API SHALL accept POST requests to `/api/commission-quote` with the payload `{ "loanAmount": number, "loanTermMonths": number, "riskBand": string }`.
2. WHEN a request to the Mock_API is missing the `api-key` header, THE Mock_API SHALL return HTTP 401 with an error message.
3. WHEN a request to the Mock_API contains a valid `api-key` header, THE Mock_API SHALL return HTTP 200 with a response containing `quoteId`, `commission`, and `totalRepayable`.
4. THE Mock_API SHALL calculate `commission` as a deterministic function of `loanAmount`, `loanTermMonths`, and `riskBand`.
5. THE Mock_API SHALL simulate random failures by returning HTTP 500 on approximately 20% of otherwise-valid requests.
6. WHEN the Mock_API returns HTTP 200, THE Mock_API SHALL include a unique `quoteId` string in the response.

---

### Requirement 7: Accessible and Responsive UI

**User Story:** As a user, I want the application to be accessible and usable across different screen sizes, so that I can use it on both desktop and mobile devices.

#### Acceptance Criteria

1. THE Commission_Quote_App SHALL render correctly on viewport widths from 320px to 1440px.
2. THE Quote_Form SHALL associate each input field with a visible label using accessible HTML attributes.
3. THE Quote_Form SHALL expose validation error messages to screen readers using ARIA attributes.
4. WHEN a quote result is displayed, THE Commission_Quote_App SHALL present the data in a visually distinct result section with sufficient colour contrast (WCAG AA).
