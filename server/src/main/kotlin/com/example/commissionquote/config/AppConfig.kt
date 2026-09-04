package com.example.commissionquote.config

import io.netty.channel.ChannelOption
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration

@Configuration
class AppConfig(
    @Value("\${COMMISSION_API_KEY:}") val apiKey: String
) {
    private val log = LoggerFactory.getLogger(AppConfig::class.java)

    @PostConstruct
    fun validateApiKey() {
        if (apiKey.isBlank()) {
            log.warn("COMMISSION_API_KEY is not set — all quote requests will return HTTP 500")
        }
    }

    @Bean
    fun webClient(builder: WebClient.Builder): WebClient {
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
            .responseTimeout(Duration.ofSeconds(10))
        return builder
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .build()
    }
}
