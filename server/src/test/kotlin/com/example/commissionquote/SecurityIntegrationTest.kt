package com.example.commissionquote

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

/**
 * Verifies that the Spring Security configuration correctly:
 * - Rejects unauthenticated requests to protected endpoints with 401
 * - Allows unauthenticated access to public endpoints (Swagger, OpenAPI docs)
 *
 * Note: Full JWT validation against a live Keycloak instance is not tested here
 * (no Keycloak available in CI). These tests use Spring Security's MockMvc
 * support to verify the security filter chain rules are correctly configured.
 *
 * @MockBean JwtDecoder prevents Spring Boot from trying to fetch the JWKS from
 * Keycloak at startup (which would fail in CI with no live Keycloak).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(
    properties = [
        "COMMISSION_API_KEY=test-api-key",
        // Provide a placeholder so the property is not blank — the actual JwtDecoder
        // is replaced by the @MockBean below, so this URI is never contacted.
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9090/realms/commission-app",
    ],
)
class SecurityIntegrationTest {
    /**
     * Replaces the real JwtDecoder bean so Spring Boot never tries to contact
     * Keycloak at startup (no issuer-uri fetch, no JWKS download).
     */
    @MockBean
    private lateinit var jwtDecoder: JwtDecoder

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val validRequestBody = """{"loanAmount": 50000.0, "loanTermMonths": 36, "riskBand": "medium"}"""

    @Test
    fun `POST commission-quote without Bearer token returns 401`() {
        mockMvc
            .post("/api/commission-quote") {
                contentType = MediaType.APPLICATION_JSON
                content = validRequestBody
            }.andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `GET swagger-ui is publicly accessible`() {
        // Swagger UI redirects to /swagger-ui/index.html — a 3xx is acceptable,
        // the important thing is it is NOT blocked with 401 or 403.
        mockMvc.get("/swagger-ui.html").andExpect {
            status { is3xxRedirection() }
        }
    }

    @Test
    fun `GET v3 api-docs is publicly accessible`() {
        mockMvc.get("/v3/api-docs").andExpect {
            status { isOk() }
        }
    }
}
