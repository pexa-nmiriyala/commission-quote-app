package com.example.commissionquote

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@TestPropertySource(properties = ["COMMISSION_API_KEY=test-api-key"])
class QuoteIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Test
    fun `POST commission-quote with valid input returns 200 with quote shape`() {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }
        val requestBody = """{"loanAmount": 50000.0, "loanTermMonths": 36, "riskBand": "medium"}"""
        val request = HttpEntity(requestBody, headers)

        // MockCommissionApiAdapter has ~20% failure rate — retry up to 10 times to get a success
        var successBody: Map<*, *>? = null
        for (attempt in 1..10) {
            val response = restTemplate.postForEntity(
                "http://localhost:$port/api/commission-quote",
                request,
                Map::class.java
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
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }
        val requestBody = """{"loanAmount": -100.0, "loanTermMonths": 36, "riskBand": "medium"}"""
        val request = HttpEntity(requestBody, headers)

        val response = restTemplate.postForEntity(
            "http://localhost:$port/api/commission-quote",
            request,
            Map::class.java
        )

        assert(response.statusCode == HttpStatus.BAD_REQUEST) {
            "Expected 400, got ${response.statusCode}"
        }
    }

    @Test
    fun `POST commission-quote with invalid riskBand returns 400`() {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }
        val requestBody = """{"loanAmount": 50000.0, "loanTermMonths": 36, "riskBand": "invalid"}"""
        val request = HttpEntity(requestBody, headers)

        val response = restTemplate.postForEntity(
            "http://localhost:$port/api/commission-quote",
            request,
            Map::class.java
        )

        assert(response.statusCode == HttpStatus.BAD_REQUEST) {
            "Expected 400, got ${response.statusCode}"
        }
    }
}
