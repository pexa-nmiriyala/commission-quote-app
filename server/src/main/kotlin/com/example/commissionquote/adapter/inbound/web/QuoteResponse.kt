package com.example.commissionquote.adapter.inbound.web

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

data class QuoteResponse(
    @Schema(description = "Unique quote identifier")
    val quoteId: String,
    @Schema(description = "Commission amount in dollars", example = "583.33")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val commission: BigDecimal,
    @Schema(description = "Total repayable amount in dollars", example = "50583.33")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val totalRepayable: BigDecimal,
)
