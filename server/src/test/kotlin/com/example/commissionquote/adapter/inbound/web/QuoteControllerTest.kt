package com.example.commissionquote.adapter.inbound.web

import com.example.commissionquote.adapter.outbound.http.CommissionApiTimeoutException
import com.example.commissionquote.adapter.outbound.http.InvalidResponseException
import com.example.commissionquote.adapter.outbound.http.UnauthorisedException
import com.example.commissionquote.adapter.outbound.http.UpstreamApiException
import com.example.commissionquote.application.ConfigurationException
import com.example.commissionquote.domain.model.QuoteResult
import com.example.commissionquote.domain.port.inbound.QuoteUseCase
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.math.BigDecimal

@WebMvcTest(QuoteController::class)
// Import SecurityConfig so the @WebMvcTest slice uses the real security rules
// (CSRF disabled, stateless sessions) rather than the default test security config.
// @MockBean JwtDecoder prevents the slice from trying to contact Keycloak at startup.
// @WithMockUser provides a fake authenticated principal for the tests that expect 2xx/4xx.
@Import(com.example.commissionquote.config.SecurityConfig::class)
@WithMockUser
class QuoteControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var jwtDecoder: JwtDecoder

    @MockkBean
    private lateinit var quoteUseCase: QuoteUseCase

    private val validRequestBody = """{"loanAmount": 50000.00, "loanTermMonths": 36, "riskBand": "medium"}"""

    @Test
    fun `POST commission-quote - valid request returns 200 with QuoteResponse`() {
        every { quoteUseCase.generateQuote(any()) } returns
            QuoteResult("q-001", BigDecimal("5250.00"), BigDecimal("55250.00"))

        mockMvc
            .post("/api/commission-quote") {
                contentType = MediaType.APPLICATION_JSON
                content = validRequestBody
            }.andExpect {
                status { isOk() }
                jsonPath("$.quoteId") { value("q-001") }
                jsonPath("$.commission") { value("5250.00") }
                jsonPath("$.totalRepayable") { value("55250.00") }
            }
    }

    @Test
    fun `POST commission-quote - missing loanAmount returns 400`() {
        mockMvc
            .post("/api/commission-quote") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"loanTermMonths": 36, "riskBand": "medium"}"""
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `POST commission-quote - negative loanAmount returns 400`() {
        mockMvc
            .post("/api/commission-quote") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"loanAmount": -100.00, "loanTermMonths": 36, "riskBand": "medium"}"""
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `POST commission-quote - loanAmount exceeds maximum returns 400`() {
        mockMvc
            .post("/api/commission-quote") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"loanAmount": 3000000.01, "loanTermMonths": 36, "riskBand": "medium"}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error") { value("loanAmount: loanAmount must not exceed \$3,000,000") }
            }
    }

    @Test
    fun `POST commission-quote - missing loanTermMonths returns 400`() {
        mockMvc
            .post("/api/commission-quote") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"loanAmount": 50000.00, "riskBand": "medium"}"""
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `POST commission-quote - loanTermMonths exceeds maximum returns 400`() {
        mockMvc
            .post("/api/commission-quote") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"loanAmount": 50000.00, "loanTermMonths": 361, "riskBand": "medium"}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error") { value("loanTermMonths: loanTermMonths must not exceed 360 (30 years)") }
            }
    }

    @Test
    fun `POST commission-quote - invalid riskBand returns 400`() {
        every { quoteUseCase.generateQuote(any()) } throws IllegalArgumentException("Invalid riskBand: unknown")

        mockMvc
            .post("/api/commission-quote") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"loanAmount": 50000.00, "loanTermMonths": 36, "riskBand": "unknown"}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error") { isNotEmpty() }
            }
    }

    @Test
    fun `POST commission-quote - ConfigurationException returns 500`() {
        every { quoteUseCase.generateQuote(any()) } throws ConfigurationException("Server configuration error")

        mockMvc
            .post("/api/commission-quote") {
                contentType = MediaType.APPLICATION_JSON
                content = validRequestBody
            }.andExpect {
                status { isInternalServerError() }
                jsonPath("$.error") { value("Server configuration error") }
            }
    }

    @Test
    fun `POST commission-quote - UnauthorisedException returns 401 with safe message`() {
        every { quoteUseCase.generateQuote(any()) } throws UnauthorisedException("internal detail that should not leak")

        mockMvc
            .post("/api/commission-quote") {
                contentType = MediaType.APPLICATION_JSON
                content = validRequestBody
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.error") { value("Unauthorised — check API key") }
            }
    }

    @Test
    fun `POST commission-quote - UpstreamApiException returns 502 with safe message`() {
        every { quoteUseCase.generateQuote(any()) } throws UpstreamApiException("internal hostname: api.internal.example.com")

        mockMvc
            .post("/api/commission-quote") {
                contentType = MediaType.APPLICATION_JSON
                content = validRequestBody
            }.andExpect {
                status { isBadGateway() }
                jsonPath("$.error") { value("Commission API error") }
            }
    }

    @Test
    fun `POST commission-quote - CommissionApiTimeoutException returns 504 with safe message`() {
        every { quoteUseCase.generateQuote(any()) } throws CommissionApiTimeoutException("internal timeout detail")

        mockMvc
            .post("/api/commission-quote") {
                contentType = MediaType.APPLICATION_JSON
                content = validRequestBody
            }.andExpect {
                status { isGatewayTimeout() }
                jsonPath("$.error") { value("Commission API request timed out") }
            }
    }

    @Test
    fun `POST commission-quote - InvalidResponseException returns 502 with safe message`() {
        every { quoteUseCase.generateQuote(any()) } throws InvalidResponseException("internal parse detail")

        mockMvc
            .post("/api/commission-quote") {
                contentType = MediaType.APPLICATION_JSON
                content = validRequestBody
            }.andExpect {
                status { isBadGateway() }
                jsonPath("$.error") { value("Received an invalid response from the Commission API") }
            }
    }
}
