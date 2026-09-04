package com.example.commissionquote.domain

import com.example.commissionquote.domain.model.LoanDetails
import com.example.commissionquote.domain.model.RiskBand
import com.example.commissionquote.domain.service.CommissionCalculator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class DomainModelTest {
    // ── RiskBand ──────────────────────────────────────────────────────────

    @Test
    fun `RiskBand from - low is case-insensitive`() {
        assertEquals(RiskBand.LOW, RiskBand.from("low"))
        assertEquals(RiskBand.LOW, RiskBand.from("LOW"))
        assertEquals(RiskBand.LOW, RiskBand.from("Low"))
    }

    @Test
    fun `RiskBand from - medium is case-insensitive`() {
        assertEquals(RiskBand.MEDIUM, RiskBand.from("medium"))
        assertEquals(RiskBand.MEDIUM, RiskBand.from("MEDIUM"))
    }

    @Test
    fun `RiskBand from - high is case-insensitive`() {
        assertEquals(RiskBand.HIGH, RiskBand.from("high"))
        assertEquals(RiskBand.HIGH, RiskBand.from("HIGH"))
    }

    @Test
    fun `RiskBand from - throws for invalid value`() {
        assertThrows<IllegalArgumentException> { RiskBand.from("unknown") }
        assertThrows<IllegalArgumentException> { RiskBand.from("") }
        assertThrows<IllegalArgumentException> { RiskBand.from("extreme") }
    }

    @Test
    fun `RiskBand multipliers are correct`() {
        assertEquals(BigDecimal("0.02"), RiskBand.LOW.multiplier)
        assertEquals(BigDecimal("0.035"), RiskBand.MEDIUM.multiplier)
        assertEquals(BigDecimal("0.05"), RiskBand.HIGH.multiplier)
    }

    // ── LoanDetails ───────────────────────────────────────────────────────

    @Test
    fun `LoanDetails - valid construction succeeds`() {
        val details = LoanDetails(BigDecimal("50000.00"), 36, RiskBand.MEDIUM)
        assertEquals(BigDecimal("50000.00"), details.loanAmount)
        assertEquals(36, details.loanTermMonths)
        assertEquals(RiskBand.MEDIUM, details.riskBand)
    }

    @Test
    fun `LoanDetails - throws when loanAmount is zero`() {
        assertThrows<IllegalArgumentException> {
            LoanDetails(BigDecimal.ZERO, 36, RiskBand.LOW)
        }
    }

    @Test
    fun `LoanDetails - throws when loanAmount is negative`() {
        assertThrows<IllegalArgumentException> {
            LoanDetails(BigDecimal("-100.00"), 36, RiskBand.LOW)
        }
    }

    @Test
    fun `LoanDetails - throws when loanTermMonths is zero`() {
        assertThrows<IllegalArgumentException> {
            LoanDetails(BigDecimal("50000.00"), 0, RiskBand.LOW)
        }
    }

    @Test
    fun `LoanDetails - throws when loanTermMonths is negative`() {
        assertThrows<IllegalArgumentException> {
            LoanDetails(BigDecimal("50000.00"), -12, RiskBand.LOW)
        }
    }

    // ── CommissionCalculator ──────────────────────────────────────────────

    private val calculator = CommissionCalculator()

    @Test
    fun `CommissionCalculator - medium risk 50000 over 36 months`() {
        val details = LoanDetails(BigDecimal("50000.00"), 36, RiskBand.MEDIUM)
        val (commission, totalRepayable) = calculator.calculate(details)
        // 50000 * 0.035 * (36/12) = 50000 * 0.035 * 3 = 5250.00
        assertEquals(BigDecimal("5250.00"), commission)
        assertEquals(BigDecimal("55250.00"), totalRepayable)
    }

    @Test
    fun `CommissionCalculator - low risk 10000 over 12 months`() {
        val details = LoanDetails(BigDecimal("10000.00"), 12, RiskBand.LOW)
        val (commission, totalRepayable) = calculator.calculate(details)
        // 10000 * 0.02 * 1 = 200.00
        assertEquals(BigDecimal("200.00"), commission)
        assertEquals(BigDecimal("10200.00"), totalRepayable)
    }

    @Test
    fun `CommissionCalculator - high risk 100000 over 24 months`() {
        val details = LoanDetails(BigDecimal("100000.00"), 24, RiskBand.HIGH)
        val (commission, totalRepayable) = calculator.calculate(details)
        // 100000 * 0.05 * 2 = 10000.00
        assertEquals(BigDecimal("10000.00"), commission)
        assertEquals(BigDecimal("110000.00"), totalRepayable)
    }

    @Test
    fun `CommissionCalculator - commission is positive`() {
        val details = LoanDetails(BigDecimal("1000.00"), 1, RiskBand.LOW)
        val (commission, _) = calculator.calculate(details)
        assertTrue(commission > BigDecimal.ZERO)
    }

    @Test
    fun `CommissionCalculator - totalRepayable is greater than loanAmount`() {
        val details = LoanDetails(BigDecimal("50000.00"), 36, RiskBand.MEDIUM)
        val (_, totalRepayable) = calculator.calculate(details)
        assertTrue(totalRepayable > details.loanAmount)
    }

    @Test
    fun `CommissionCalculator - same inputs produce same result`() {
        val details = LoanDetails(BigDecimal("75000.00"), 48, RiskBand.HIGH)
        val result1 = calculator.calculate(details)
        val result2 = calculator.calculate(details)
        assertEquals(result1, result2)
    }
}
