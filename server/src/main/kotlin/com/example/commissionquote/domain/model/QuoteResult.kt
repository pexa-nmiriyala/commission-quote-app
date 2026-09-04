package com.example.commissionquote.domain.model

import java.math.BigDecimal

data class QuoteResult(
    val quoteId: String,
    val commission: BigDecimal,
    val totalRepayable: BigDecimal,
)
