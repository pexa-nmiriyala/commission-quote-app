package com.example.commissionquote.application

import com.example.commissionquote.domain.model.LoanDetails
import com.example.commissionquote.domain.model.QuoteResult
import com.example.commissionquote.domain.model.RiskBand
import com.example.commissionquote.domain.port.outbound.CommissionApiPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class QuoteApplicationServiceTest {
    private val commissionApiPort: CommissionApiPort = mockk()

    private val loanDetails = LoanDetails(50000.0, 36, RiskBand.MEDIUM)
    private val expectedResult = QuoteResult("quote-123", 5250.0, 55250.0)

    @Test
    fun `generateQuote - throws ConfigurationException when apiKey is blank`() {
        val service = QuoteApplicationService(commissionApiPort, "")
        assertThrows<ConfigurationException> {
            service.generateQuote(loanDetails)
        }
    }

    @Test
    fun `generateQuote - throws ConfigurationException when apiKey is whitespace`() {
        val service = QuoteApplicationService(commissionApiPort, "   ")
        assertThrows<ConfigurationException> {
            service.generateQuote(loanDetails)
        }
    }

    @Test
    fun `generateQuote - delegates to commissionApiPort when apiKey is set`() {
        val service = QuoteApplicationService(commissionApiPort, "valid-key")
        every { commissionApiPort.fetchQuote(loanDetails) } returns expectedResult

        val result = service.generateQuote(loanDetails)

        assertEquals(expectedResult, result)
        verify(exactly = 1) { commissionApiPort.fetchQuote(loanDetails) }
    }

    @Test
    fun `generateQuote - propagates exception from commissionApiPort`() {
        val service = QuoteApplicationService(commissionApiPort, "valid-key")
        every { commissionApiPort.fetchQuote(loanDetails) } throws RuntimeException("upstream error")

        assertThrows<RuntimeException> {
            service.generateQuote(loanDetails)
        }
    }

    @Test
    fun `generateQuote - returns exact QuoteResult from port`() {
        val service = QuoteApplicationService(commissionApiPort, "my-key")
        val customResult = QuoteResult("abc-999", 123.45, 10123.45)
        every { commissionApiPort.fetchQuote(loanDetails) } returns customResult

        val result = service.generateQuote(loanDetails)
        assertEquals("abc-999", result.quoteId)
        assertEquals(123.45, result.commission)
        assertEquals(10123.45, result.totalRepayable)
    }
}
