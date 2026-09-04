package com.example.commissionquote.domain.service

import com.example.commissionquote.domain.model.LoanDetails
import org.springframework.stereotype.Service

@Service
class CommissionCalculator {
    fun calculate(details: LoanDetails): Pair<Double, Double> {
        val commission = details.loanAmount * details.riskBand.multiplier * (details.loanTermMonths / 12.0)
        val totalRepayable = details.loanAmount + commission
        return Pair(commission, totalRepayable)
    }
}
