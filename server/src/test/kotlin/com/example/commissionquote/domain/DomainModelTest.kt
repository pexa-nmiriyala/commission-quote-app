package com.example.commissionquote.domain

import com.example.commissionquote.domain.model.LoanDetails
import com.example.commissionquote.domain.model.RiskBand
import com.example.commissionquote.domain.service.CommissionCalculator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
        assertEquals(0.02, RiskBand.LOW.multiplier)
        assertEquals(0.035, RiskBand.MEDIUM.multiplier)
        assertEquals(0.05, RiskBand.HIGH.multiplier)
    }

    // ── LoanDetails ───────────────────────────────────────────────────────

    @Test
    fun `LoanDetails - valid construction succeeds`() {
        val details = LoanDetails(50000.0, 36, RiskBand.MEDIUM)
        assertEquals(50000.0, details.loanAmount)
        assertEquals(36, details.loanTermMonths)
        assertEquals(RiskBand.MEDIUM, details.riskBand)
    }

    @Test
    fun `LoanDetails - throws when loanAmount is zero`() {
        assertThrows<IllegalArgumentException> {
            LoanDetails(0.0, 36, RiskBand.LOW)
        }
    }

    @Test
    fun `LoanDetails - throws when loanAmount is negative`() {
        assertThrows<IllegalArgumentException> {
            LoanDetails(-100.0, 36, RiskBand.LOW)
        }
    }

    @Test
    fun `LoanDetails - throws when loanTermMonths is zero`() {
        assertThrows<IllegalArgumentException> {
            LoanDetails(50000.0, 0, RiskBand.LOW)
        }
    }

    @Test
    fun `LoanDetails - throws when loanTermMonths is negative`() {
        assertThrows<IllegalArgumentException> {
            LoanDetails(50000.0, -12, RiskBand.LOW)
        }
    }

    // ── CommissionCalculator ──────────────────────────────────────────────

    private val calculator = CommissionCalculator()

    @Test
    fun `CommissionCalculator - medium risk 50000 over 36 months`() {
        val details = LoanDetails(50000.0, 36, RiskBand.MEDIUM)
        val (commission, totalRepayable) = calculator.calculate(details)
        // 50000 * 0.035 * (36/12.0) = 50000 * 0.035 * 3 = 5250.0
        assertEquals(5250.0, commission, 0.001)
        assertEquals(55250.0, totalRepayable, 0.001)
    }

    @Test
    fun `CommissionCalculator - low risk 10000 over 12 months`() {
        val details = LoanDetails(10000.0, 12, RiskBand.LOW)
        val (commission, totalRepayable) = calculator.calculate(details)
        // 10000 * 0.02 * 1 = 200.0
        assertEquals(200.0, commission, 0.001)
        assertEquals(10200.0, totalRepayable, 0.001)
    }

    @Test
    fun `CommissionCalculator - high risk 100000 over 24 months`() {
        val details = LoanDetails(100000.0, 24, RiskBand.HIGH)
        val (commission, totalRepayable) = calculator.calculate(details)
        // 100000 * 0.05 * 2 = 10000.0
        assertEquals(10000.0, commission, 0.001)
        assertEquals(110000.0, totalRepayable, 0.001)
    }

    @Test
    fun `CommissionCalculator - commission is positive`() {
        val details = LoanDetails(1000.0, 1, RiskBand.LOW)
        val (commission, _) = calculator.calculate(details)
        assertTrue(commission > 0)
    }

    @Test
    fun `CommissionCalculator - totalRepayable is greater than loanAmount`() {
        val details = LoanDetails(50000.0, 36, RiskBand.MEDIUM)
        val (_, totalRepayable) = calculator.calculate(details)
        assertTrue(totalRepayable > details.loanAmount)
    }

    @Test
    fun `CommissionCalculator - same inputs produce same result`() {
        val details = LoanDetails(75000.0, 48, RiskBand.HIGH)
        val result1 = calculator.calculate(details)
        val result2 = calculator.calculate(details)
        assertEquals(result1, result2)
    }
}
