package com.example.commissionquote.domain.model

import java.math.BigDecimal

enum class RiskBand(
    val multiplier: BigDecimal,
) {
    LOW(BigDecimal("0.02")),
    MEDIUM(BigDecimal("0.035")),
    HIGH(BigDecimal("0.05")),
    ;

    companion object {
        fun from(value: String): RiskBand =
            entries.find { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid riskBand: $value")
    }
}
