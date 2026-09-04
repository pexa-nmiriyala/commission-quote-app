package com.example.commissionquote.domain.model

import java.math.BigDecimal

data class LoanDetails(
    val loanAmount: BigDecimal,
    val loanTermMonths: Int,
    val riskBand: RiskBand,
) {
    init {
        require(loanAmount > BigDecimal.ZERO) { "loanAmount must be positive" }
        require(loanTermMonths > 0) { "loanTermMonths must be a positive integer" }
    }
}
