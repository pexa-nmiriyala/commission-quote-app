package com.example.commissionquote

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.time.Instant

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@TestPropertySource(
    properties = [
        "COMMISSION_API_KEY=test-api-key",
        // Provide a placeholder so the property is not blank — the real JwtDecoder
        // is replaced by the @MockBean below, so this URI is never contacted.
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9090/realms/commission-app",
    ],
)
class QuoteIntegrationTest {
    /**
     * Replaces the real JwtDecoder so Spring Security never tries to fetch
     * the JWKS from Keycloak at startup (no live Keycloak in CI).
     * The mock is configured to accept any Bearer token string.
     */
    @MockBean
    private lateinit var jwtDecoder: JwtDecoder

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    /** A minimal valid JWT object returned by the mocked JwtDecoder. */
    private val mockJwt: Jwt =
        Jwt
            .withTokenValue("test-token")
            .header("alg", "none")
            .claim("sub", "test-user")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()

    @BeforeEach
    fun setUp() {
        // Allow any Bearer token to pass JWT validation in these tests.
        `when`(jwtDecoder.decode(anyString())).thenReturn(mockJwt)
    }

    /** Build headers with a fake Bearer token so Spring Security passes the request through. */
    private fun jsonHeadersWithAuth(): HttpHeaders =
        HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth("test-token")
        }

    @Test
    fun `POST commission-quote with valid input returns 200 with quote shape`() {
        val requestBody = """{"loanAmount": 50000.0, "loanTermMonths": 36, "riskBand": "medium"}"""
        val request = HttpEntity(requestBody, jsonHeadersWithAuth())

        // MockCommissionApiAdapter has ~20% failure rate — retry up to 10 times to get a success
        var successBody: Map<*, *>? = null
        for (attempt in 1..10) {
            val response =
                restTemplate.postForEntity(
                    "http://localhost:$port/api/commission-quote",
                    request,
                    Map::class.java,
                )
            if (response.statusCode == HttpStatus.OK) {
                successBody = response.body
                break
            }
        }

        // Assert we got at least one successful response
        assert(successBody != null) { "Expected a 200 response after 10 retries" }
        val responseBody = successBody!!
        assert(responseBody["quoteId"] != null) { "Expected quoteId in response" }
        assert(responseBody["commission"] != null) { "Expected commission in response" }
        assert(responseBody["totalRepayable"] != null) { "Expected totalRepayable in response" }
    }

    @Test
    fun `POST commission-quote with invalid loanAmount returns 400`() {
        val requestBody = """{"loanAmount": -100.0, "loanTermMonths": 36, "riskBand": "medium"}"""
        val request = HttpEntity(requestBody, jsonHeadersWithAuth())

        val response =
            restTemplate.postForEntity(
                "http://localhost:$port/api/commission-quote",
                request,
                Map::class.java,
            )

        assert(response.statusCode == HttpStatus.BAD_REQUEST) {
            "Expected 400, got ${response.statusCode}"
        }
    }

    @Test
    fun `POST commission-quote with invalid riskBand returns 400`() {
        val requestBody = """{"loanAmount": 50000.0, "loanTermMonths": 36, "riskBand": "invalid"}"""
        val request = HttpEntity(requestBody, jsonHeadersWithAuth())

        val response =
            restTemplate.postForEntity(
                "http://localhost:$port/api/commission-quote",
                request,
                Map::class.java,
            )

        assert(response.statusCode == HttpStatus.BAD_REQUEST) {
            "Expected 400, got ${response.statusCode}"
        }
    }
}
