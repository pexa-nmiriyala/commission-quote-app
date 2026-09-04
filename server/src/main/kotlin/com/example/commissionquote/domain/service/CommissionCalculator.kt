package com.example.commissionquote.domain.service

import com.example.commissionquote.domain.model.LoanDetails
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

@Service
class CommissionCalculator {
    fun calculate(details: LoanDetails): Pair<BigDecimal, BigDecimal> {
        val termYears = BigDecimal(details.loanTermMonths).divide(BigDecimal("12"), MathContext.DECIMAL128)
        val commission =
            details.loanAmount
                .multiply(details.riskBand.multiplier)
                .multiply(termYears)
                .setScale(2, RoundingMode.HALF_UP)
        val totalRepayable = details.loanAmount.add(commission).setScale(2, RoundingMode.HALF_UP)
        return Pair(commission, totalRepayable)
    }
}
