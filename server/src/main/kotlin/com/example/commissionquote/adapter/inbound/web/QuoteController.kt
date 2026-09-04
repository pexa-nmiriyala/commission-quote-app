package com.example.commissionquote.adapter.inbound.web

import com.example.commissionquote.adapter.outbound.http.CommissionApiTimeoutException
import com.example.commissionquote.adapter.outbound.http.InvalidResponseException
import com.example.commissionquote.adapter.outbound.http.UnauthorisedException
import com.example.commissionquote.adapter.outbound.mock.UpstreamApiException
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
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.*

@Tag(name = "Quote", description = "Commission quote generation")
@RestController
class QuoteController(private val quoteUseCase: QuoteUseCase) {

    @Operation(
        summary = "Generate a commission quote",
        description = "Accepts loan details and returns a commission quote via the backend proxy"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Quote generated successfully",
            content = [Content(schema = Schema(implementation = QuoteResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid input",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "401", description = "Unauthorised — check API key",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = "Server configuration error",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "502", description = "Commission API error or invalid response",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "504", description = "Commission API timed out",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    )
    @PostMapping("/api/commission-quote")
    fun generateQuote(@RequestBody @Valid request: QuoteRequest): ResponseEntity<QuoteResponse> {
        val loanDetails = LoanDetails(
            loanAmount = request.loanAmount!!,
            loanTermMonths = request.loanTermMonths!!,
            riskBand = RiskBand.from(request.riskBand!!)
        )
        val result = quoteUseCase.generateQuote(loanDetails)
        return ResponseEntity.ok(
            QuoteResponse(
                quoteId = result.quoteId,
                commission = result.commission,
                totalRepayable = result.totalRepayable
            )
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message ?: "Invalid input"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = e.bindingResult.fieldErrors.joinToString("; ") {
            "${it.field}: ${it.defaultMessage}"
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(message))
    }

    @ExceptionHandler(ConfigurationException::class)
    fun handleConfiguration(e: ConfigurationException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse(e.message ?: "Server configuration error"))

    @ExceptionHandler(UnauthorisedException::class)
    fun handleUnauthorised(e: UnauthorisedException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse(e.message ?: "Unauthorised — check API key"))

    @ExceptionHandler(UpstreamApiException::class)
    fun handleUpstream(e: UpstreamApiException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ErrorResponse(e.message ?: "Commission API error"))

    @ExceptionHandler(CommissionApiTimeoutException::class)
    fun handleTimeout(e: CommissionApiTimeoutException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(ErrorResponse(e.message ?: "Request timed out"))

    @ExceptionHandler(InvalidResponseException::class)
    fun handleInvalidResponse(e: InvalidResponseException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ErrorResponse(e.message ?: "Invalid response from API"))
}
