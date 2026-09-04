package com.example.commissionquote.application

import com.example.commissionquote.domain.model.LoanDetails
import com.example.commissionquote.domain.model.QuoteResult
import com.example.commissionquote.domain.port.inbound.QuoteUseCase
import com.example.commissionquote.domain.port.outbound.CommissionApiPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

class ConfigurationException(
    message: String,
) : RuntimeException(message)

@Service
class QuoteApplicationService(
    private val commissionApiPort: CommissionApiPort,
    @Value("\${COMMISSION_API_KEY:}") private val apiKey: String,
) : QuoteUseCase {
    override fun generateQuote(details: LoanDetails): QuoteResult {
        if (apiKey.isBlank()) {
            throw ConfigurationException("Server configuration error")
        }
        return commissionApiPort.fetchQuote(details)
    }
}
