package com.example.commissionquote.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun openApiInfo(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Commission Quote API")
                    .version("1.0.0")
                    .description("Backend proxy for generating commission quotes on loan applications"),
            )
}
