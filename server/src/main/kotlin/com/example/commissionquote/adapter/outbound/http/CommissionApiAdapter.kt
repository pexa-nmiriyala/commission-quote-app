package com.example.commissionquote.adapter.outbound.http

import com.example.commissionquote.domain.model.LoanDetails
import com.example.commissionquote.domain.model.QuoteResult
import com.example.commissionquote.domain.port.outbound.CommissionApiPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.math.BigDecimal

data class CommissionApiRequest(
    val loanAmount: BigDecimal,
    val loanTermMonths: Int,
    val riskBand: String,
)

data class CommissionApiResponse(
    val quoteId: String?,
    val commission: BigDecimal?,
    val totalRepayable: BigDecimal?,
)

@ConditionalOnMissingBean(CommissionApiPort::class)
@Service
class CommissionApiAdapter(
    private val webClient: WebClient,
    @Value("\${commission.api.url}") private val apiUrl: String,
    @Value("\${COMMISSION_API_KEY:}") private val apiKey: String,
) : CommissionApiPort {
    override fun fetchQuote(details: LoanDetails): QuoteResult {
        val requestBody =
            CommissionApiRequest(
                loanAmount = details.loanAmount,
                loanTermMonths = details.loanTermMonths,
                riskBand = details.riskBand.name.lowercase(),
            )

        return try {
            val response =
                webClient
                    .post()
                    .uri(apiUrl)
                    .header("api-key", apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus({ it.value() == 401 }) {
                        Mono.error(UnauthorisedException("Unauthorised — check API key"))
                    }.onStatus({ it.is5xxServerError }) {
                        Mono.error(UpstreamApiException("Commission API error"))
                    }.bodyToMono(CommissionApiResponse::class.java)
                    .block()
            // Note: response timeout is configured on the WebClient's HttpClient (AppConfig),
            // which surfaces as io.netty.handler.timeout.ReadTimeoutException.
            // A redundant .timeout() operator has been removed — it caused double-wrapping
            // that bypassed the TimeoutException catch block and produced 500 instead of 504.

            if (response == null || response.quoteId == null || response.commission == null || response.totalRepayable == null) {
                throw InvalidResponseException("Invalid response from API")
            }

            QuoteResult(
                quoteId = response.quoteId,
                commission = response.commission,
                totalRepayable = response.totalRepayable,
            )
        } catch (e: UnauthorisedException) {
            throw e
        } catch (e: UpstreamApiException) {
            throw e
        } catch (e: InvalidResponseException) {
            throw e
        } catch (e: WebClientResponseException) {
            if (e.statusCode.value() == 401) throw UnauthorisedException("Unauthorised — check API key")
            throw UpstreamApiException("Commission API error")
        } catch (e: java.util.concurrent.TimeoutException) {
            throw CommissionApiTimeoutException("Request timed out")
        } catch (e: io.netty.handler.timeout.ReadTimeoutException) {
            throw CommissionApiTimeoutException("Request timed out")
        }
    }
}
