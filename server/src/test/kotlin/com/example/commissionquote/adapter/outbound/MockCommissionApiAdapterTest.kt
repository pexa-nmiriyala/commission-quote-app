package com.example.commissionquote.adapter.outbound

import com.example.commissionquote.adapter.outbound.http.UpstreamApiException
import com.example.commissionquote.adapter.outbound.mock.MockCommissionApiAdapter
import com.example.commissionquote.domain.model.LoanDetails
import com.example.commissionquote.domain.model.RiskBand
import com.example.commissionquote.domain.service.CommissionCalculator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class MockCommissionApiAdapterTest {
    private val calculator = CommissionCalculator()
    private val details = LoanDetails(BigDecimal("50000.00"), 36, RiskBand.MEDIUM)

    @Test
    fun `fetchQuote - returns QuoteResult with non-blank quoteId`() {
        // Run several times to get a non-failure result (20% failure rate)
        var result: com.example.commissionquote.domain.model.QuoteResult? = null
        repeat(20) {
            try {
                result = MockCommissionApiAdapter(calculator).fetchQuote(details)
                return@repeat
            } catch (_: UpstreamApiException) {
            }
        }
        assertNotNull(result)
        assertTrue(result!!.quoteId.isNotBlank())
    }

    @Test
    fun `fetchQuote - commission is positive on success`() {
        var commission: BigDecimal? = null
        repeat(20) {
            try {
                commission = MockCommissionApiAdapter(calculator).fetchQuote(details).commission
                return@repeat
            } catch (_: UpstreamApiException) {
            }
        }
        assertNotNull(commission)
        assertTrue(commission!! > BigDecimal.ZERO)
    }

    @Test
    fun `fetchQuote - totalRepayable is greater than loanAmount on success`() {
        var totalRepayable: BigDecimal? = null
        repeat(20) {
            try {
                totalRepayable = MockCommissionApiAdapter(calculator).fetchQuote(details).totalRepayable
                return@repeat
            } catch (_: UpstreamApiException) {
            }
        }
        assertNotNull(totalRepayable)
        assertTrue(totalRepayable!! > details.loanAmount)
    }

    @Test
    fun `fetchQuote - formula matches CommissionCalculator`() {
        val (expectedCommission, expectedTotal) = calculator.calculate(details)
        var result: com.example.commissionquote.domain.model.QuoteResult? = null
        repeat(20) {
            try {
                result = MockCommissionApiAdapter(calculator).fetchQuote(details)
                return@repeat
            } catch (_: UpstreamApiException) {
            }
        }
        assertNotNull(result)
        assertEquals(expectedCommission, result!!.commission)
        assertEquals(expectedTotal, result!!.totalRepayable)
    }

    @Test
    fun `fetchQuote - each successful call returns unique quoteId`() {
        val ids = mutableSetOf<String>()
        var collected = 0
        repeat(100) {
            try {
                ids.add(MockCommissionApiAdapter(calculator).fetchQuote(details).quoteId)
                collected++
            } catch (_: UpstreamApiException) {
            }
        }
        // All collected IDs should be unique
        if (collected > 1) {
            assertEquals(collected, ids.size)
        }
    }

    @Test
    fun `fetchQuote - failure rate is approximately 20 percent`() {
        var failures = 0
        val total = 200
        repeat(total) {
            try {
                MockCommissionApiAdapter(calculator).fetchQuote(details)
            } catch (_: UpstreamApiException) {
                failures++
            }
        }
        val rate = failures.toDouble() / total
        // Allow 5%-40% range (wide tolerance for randomness)
        assertTrue(rate in 0.05..0.40, "Failure rate $rate is outside expected range 5%-40%")
    }
}
