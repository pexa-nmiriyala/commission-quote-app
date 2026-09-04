package com.example.commissionquote.adapter.inbound.web

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

const val CORRELATION_ID_HEADER = "X-Correlation-ID"
const val CORRELATION_ID_MDC_KEY = "correlationId"

/**
 * Servlet filter that ensures every request has a correlation ID for distributed tracing.
 *
 * - Reads the X-Correlation-ID request header if present; otherwise generates a new UUID.
 * - Puts the value into SLF4J MDC so all log statements in the request thread automatically
 *   include it (via the logging pattern).
 * - Echoes the correlation ID back in the X-Correlation-ID response header so callers can
 *   correlate their own logs with server-side logs.
 * - Clears the MDC entry after the request completes to prevent leakage in thread pool reuse.
 */
@Component
@Order(1)
class CorrelationIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val correlationId =
            request.getHeader(CORRELATION_ID_HEADER)?.takeIf { it.isNotBlank() }
                ?: UUID.randomUUID().toString()

        MDC.put(CORRELATION_ID_MDC_KEY, correlationId)
        response.setHeader(CORRELATION_ID_HEADER, correlationId)

        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY)
        }
    }
}
