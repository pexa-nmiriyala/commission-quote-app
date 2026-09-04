package com.example.commissionquote.domain.model

enum class RiskBand(
    val multiplier: Double,
) {
    LOW(0.02),
    MEDIUM(0.035),
    HIGH(0.05),
    ;

    companion object {
        fun from(value: String): RiskBand =
            entries.find { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid riskBand: $value")
    }
}
