package com.example.commissionquote.adapter.inbound.web

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class QuoteRequest(
    @field:NotNull
    @field:DecimalMin(value = "0.01", message = "loanAmount must be at least \$0.01")
    @field:DecimalMax(value = "3000000.00", message = "loanAmount must not exceed \$3,000,000")
    @Schema(
        description = "Loan amount in dollars (retail lending: \$0.01–\$3,000,000)",
        example = "50000.00",
        minimum = "0.01",
        maximum = "3000000.00",
    )
    val loanAmount: BigDecimal?,
    @field:NotNull
    @field:Min(value = 1, message = "loanTermMonths must be at least 1")
    @field:Max(value = 360, message = "loanTermMonths must not exceed 360 (30 years)")
    @Schema(
        description = "Loan term in months (retail lending: 1–360 months)",
        example = "36",
        minimum = "1",
        maximum = "360",
    )
    val loanTermMonths: Int?,
    @field:NotBlank
    @Schema(description = "Risk band", example = "medium", allowableValues = ["low", "medium", "high"])
    val riskBand: String?,
)
