package com.example.commissionquote.adapter.outbound.mock

import com.example.commissionquote.domain.model.LoanDetails
import com.example.commissionquote.domain.model.QuoteResult
import com.example.commissionquote.domain.port.outbound.CommissionApiPort
import com.example.commissionquote.domain.service.CommissionCalculator
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.util.UUID

class UpstreamApiException(
    message: String,
) : RuntimeException(message)

@Profile("dev", "local")
@Service
class MockCommissionApiAdapter(
    private val calculator: CommissionCalculator,
) : CommissionApiPort {
    override fun fetchQuote(details: LoanDetails): QuoteResult {
        if (Math.random() < 0.2) {
            throw UpstreamApiException("Commission API error")
        }
        val (commission, totalRepayable) = calculator.calculate(details)
        return QuoteResult(
            quoteId = UUID.randomUUID().toString(),
            commission = commission,
            totalRepayable = totalRepayable,
        )
    }
}
