package com.example.commissionquote.adapter.inbound.web

import com.example.commissionquote.adapter.outbound.http.CommissionApiTimeoutException
import com.example.commissionquote.adapter.outbound.http.InvalidResponseException
import com.example.commissionquote.adapter.outbound.http.UnauthorisedException
import com.example.commissionquote.adapter.outbound.http.UpstreamApiException
import com.example.commissionquote.application.ConfigurationException
import com.example.commissionquote.domain.model.LoanDetails
import com.example.commissionquote.domain.model.RiskBand
import com.example.commissionquote.domain.port.inbound.QuoteUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Quote", description = "Commission quote generation")
@RestController
class QuoteController(
    private val quoteUseCase: QuoteUseCase,
) {
    private val log = LoggerFactory.getLogger(QuoteController::class.java)

    @Operation(
        summary = "Generate a commission quote",
        description = "Accepts loan details and returns a commission quote via the backend proxy",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Quote generated successfully",
            content = [Content(schema = Schema(implementation = QuoteResponse::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorised — check API key",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "500",
            description = "Server configuration error",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "502",
            description = "Commission API error or invalid response",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "504",
            description = "Commission API timed out",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    @PostMapping("/api/commission-quote")
    fun generateQuote(
        @RequestBody @Valid request: QuoteRequest,
    ): ResponseEntity<QuoteResponse> {
        val loanDetails =
            LoanDetails(
                loanAmount = request.loanAmount!!,
                loanTermMonths = request.loanTermMonths!!,
                riskBand = RiskBand.from(request.riskBand!!),
            )
        val result = quoteUseCase.generateQuote(loanDetails)
        return ResponseEntity.ok(
            QuoteResponse(
                quoteId = result.quoteId,
                commission = result.commission,
                totalRepayable = result.totalRepayable,
            ),
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message ?: "Invalid input"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message =
            e.bindingResult.fieldErrors.joinToString("; ") {
                "${it.field}: ${it.defaultMessage}"
            }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(message))
    }

    @ExceptionHandler(ConfigurationException::class)
    fun handleConfiguration(e: ConfigurationException): ResponseEntity<ErrorResponse> {
        log.error("Server configuration error", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse("Server configuration error"))
    }

    @ExceptionHandler(UnauthorisedException::class)
    fun handleUnauthorised(e: UnauthorisedException): ResponseEntity<ErrorResponse> {
        log.error("Commission API authentication failed", e)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorised — check API key"))
    }

    @ExceptionHandler(UpstreamApiException::class)
    fun handleUpstream(e: UpstreamApiException): ResponseEntity<ErrorResponse> {
        log.error("Commission API returned an error", e)
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ErrorResponse("Commission API error"))
    }

    @ExceptionHandler(CommissionApiTimeoutException::class)
    fun handleTimeout(e: CommissionApiTimeoutException): ResponseEntity<ErrorResponse> {
        log.error("Commission API request timed out", e)
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(ErrorResponse("Commission API request timed out"))
    }

    @ExceptionHandler(InvalidResponseException::class)
    fun handleInvalidResponse(e: InvalidResponseException): ResponseEntity<ErrorResponse> {
        log.error("Commission API returned an invalid response", e)
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ErrorResponse("Received an invalid response from the Commission API"))
    }
}
