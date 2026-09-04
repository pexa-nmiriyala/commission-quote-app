package com.example.commissionquote.domain.port.inbound

import com.example.commissionquote.domain.model.LoanDetails
import com.example.commissionquote.domain.model.QuoteResult

interface QuoteUseCase {
    fun generateQuote(details: LoanDetails): QuoteResult
}
