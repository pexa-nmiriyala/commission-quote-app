package com.example.commissionquote.adapter.outbound.http

class UnauthorisedException(
    message: String,
) : RuntimeException(message)

class InvalidResponseException(
    message: String,
) : RuntimeException(message)

class CommissionApiTimeoutException(
    message: String,
) : RuntimeException(message)
