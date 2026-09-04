package com.example.commissionquote.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtIssuerValidator
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig(
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}") private val issuerUri: String,
    @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") private val jwkSetUri: String,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // Stateless — no session needed, each request carries a JWT
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            // Disable CSRF — not needed for stateless JWT APIs
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                // Public endpoints — Swagger UI, OpenAPI spec, health
                auth
                    .requestMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml",
                        "/actuator/health",
                    ).permitAll()

                // All other endpoints require a valid JWT
                auth.anyRequest().authenticated()
            }
            // Validate Bearer tokens as JWTs issued by Keycloak
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { }
            }

        return http.build()
    }

    /**
     * Custom JwtDecoder that supports a split configuration:
     * - When KEYCLOAK_JWK_SET_URI is set (Docker mode): fetch JWKS from the
     *   internal service URL (keycloak:8080) but validate the iss claim against
     *   the public issuer URI (localhost:9090). This allows the server container
     *   to reach Keycloak's JWKS endpoint via the Docker internal network while
     *   still accepting tokens whose iss was set by --hostname-url.
     * - When KEYCLOAK_JWK_SET_URI is blank (local dev): fall back to standard
     *   Spring Boot auto-configuration via issuer-uri.
     */
    @Bean
    fun jwtDecoder(): JwtDecoder {
        val effectiveJwkSetUri =
            if (jwkSetUri.isNotBlank()) {
                jwkSetUri
            } else {
                // Derive JWK set URI from issuer URI (standard OpenID Connect convention)
                "$issuerUri/protocol/openid-connect/certs"
            }

        val decoder = NimbusJwtDecoder.withJwkSetUri(effectiveJwkSetUri).build()

        // Validate both the issuer claim and token timestamps
        decoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(
                JwtTimestampValidator(),
                JwtIssuerValidator(issuerUri),
            ),
        )

        return decoder
    }
}
