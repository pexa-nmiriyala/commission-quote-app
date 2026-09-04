package com.example.commissionquote.adapter.inbound.web

import com.example.commissionquote.adapter.outbound.http.CommissionApiTimeoutException
import com.example.commissionquote.adapter.outbound.http.InvalidResponseException
import com.example.commissionquote.adapter.outbound.http.UnauthorisedException
import com.example.commissionquote.adapter.outbound.mock.UpstreamApiException
import com.example.commissionquote.application.ConfigurationException
import com.example.commissionquote.domain.model.QuoteResult
import com.example.commissionquote.domain.port.inbound.QuoteUseCase
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@WebMvcTest(QuoteController::class)
class QuoteControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var quoteUseCase: QuoteUseCase

    private val validRequestBody = """{"loanAmount": 50000.0, "loanTermMonths": 36, "riskBand": "medium"}"""

    @Test
    fun `POST commission-quote - valid request returns 200 with QuoteResponse`() {
        every { quoteUseCase.generateQuote(any()) } returns QuoteResult("q-001", 5250.0, 55250.0)

        mockMvc
            .post("/api/commission-quote") {
                contentType = MediaType.APPLICATION_JSON
                content = validRequestBody
            }.andExpect {
                status { isOk() }
                jsonPath("$.quoteId") { value("q-001") }
                jsonPath("$.commission") { value(5250.0) }
                jsonPath("$.totalRepayable") { value(55250.0) }
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
                content = """{"loanAmount": -100.0, "loanTermMonths": 36, "riskBand": "medium"}"""
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `POST commission-quote - missing loanTermMonths returns 400`() {
        mockMvc
            .post("/api/commission-quote") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"loanAmount": 50000.0, "riskBand": "medium"}"""
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `POST commission-quote - invalid riskBand returns 400`() {
        every { quoteUseCase.generateQuote(any()) } throws IllegalArgumentException("Invalid riskBand: unknown")

        mockMvc
            .post("/api/commission-quote") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"loanAmount": 50000.0, "loanTermMonths": 36, "riskBand": "unknown"}"""
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
    fun `POST commission-quote - UnauthorisedException returns 401`() {
        every { quoteUseCase.generateQuote(any()) } throws UnauthorisedException("Unauthorised — check API key")

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
    fun `POST commission-quote - UpstreamApiException returns 502`() {
        every { quoteUseCase.generateQuote(any()) } throws UpstreamApiException("Commission API error")

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
    fun `POST commission-quote - CommissionApiTimeoutException returns 504`() {
        every { quoteUseCase.generateQuote(any()) } throws CommissionApiTimeoutException("Request timed out")

        mockMvc
            .post("/api/commission-quote") {
                contentType = MediaType.APPLICATION_JSON
                content = validRequestBody
            }.andExpect {
                status { isGatewayTimeout() }
                jsonPath("$.error") { value("Request timed out") }
            }
    }

    @Test
    fun `POST commission-quote - InvalidResponseException returns 502`() {
        every { quoteUseCase.generateQuote(any()) } throws InvalidResponseException("Invalid response from API")

        mockMvc
            .post("/api/commission-quote") {
                contentType = MediaType.APPLICATION_JSON
                content = validRequestBody
            }.andExpect {
                status { isBadGateway() }
                jsonPath("$.error") { value("Invalid response from API") }
            }
    }
}
