package com.example.commissionquote.adapter.inbound.web

import io.swagger.v3.oas.annotations.media.Schema

data class QuoteResponse(
    @Schema(description = "Unique quote identifier")
    val quoteId: String,

    @Schema(description = "Commission amount in dollars", example = "583.33")
    val commission: Double,

    @Schema(description = "Total repayable amount in dollars", example = "50583.33")
    val totalRepayable: Double
)
