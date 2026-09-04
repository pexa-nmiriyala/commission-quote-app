package com.example.commissionquote.domain.port.outbound

import com.example.commissionquote.domain.model.LoanDetails
import com.example.commissionquote.domain.model.QuoteResult

interface CommissionApiPort {
    fun fetchQuote(details: LoanDetails): QuoteResult
}
