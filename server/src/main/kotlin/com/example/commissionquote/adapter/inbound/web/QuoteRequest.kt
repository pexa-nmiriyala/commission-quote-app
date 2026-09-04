package com.example.commissionquote.adapter.inbound.web

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class QuoteRequest(
    @field:NotNull
    @field:Positive
    @Schema(description = "Loan amount in dollars", example = "50000.0", minimum = "0.01")
    val loanAmount: Double?,

    @field:NotNull
    @field:Positive
    @Schema(description = "Loan term in months", example = "36", minimum = "1")
    val loanTermMonths: Int?,

    @field:NotBlank
    @Schema(description = "Risk band", example = "medium", allowableValues = ["low", "medium", "high"])
    val riskBand: String?
)
