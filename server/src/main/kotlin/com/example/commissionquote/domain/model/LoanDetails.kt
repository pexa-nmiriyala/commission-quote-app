package com.example.commissionquote.domain.model

data class LoanDetails(
    val loanAmount: Double,
    val loanTermMonths: Int,
    val riskBand: RiskBand,
) {
    init {
        require(loanAmount > 0) { "loanAmount must be positive" }
        require(loanTermMonths > 0) { "loanTermMonths must be a positive integer" }
    }
}
