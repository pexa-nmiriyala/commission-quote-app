package com.example.commissionquote.adapter.inbound.web

import io.swagger.v3.oas.annotations.media.Schema

data class ErrorResponse(
    @Schema(description = "Human-readable error message")
    val error: String,
)
