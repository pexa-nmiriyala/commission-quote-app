package com.example.commissionquote.domain.model

data class QuoteResult(
    val quoteId: String,
    val commission: Double,
    val totalRepayable: Double
)
