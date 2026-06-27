package com.example.gateway.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http, CorsConfigurationSource corsConfigurationSource) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(
                "/api/auth/register",
                "/api/auth/verify-email",
                "/api/auth/resend-verification",
                "/api/auth/login",
                                "/actuator/health",
                                "/actuator/prometheus",
                                "/api/exam/circuit-breakers"
                        ).permitAll()
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyExchange().authenticated())
                .cors(corsSpec -> corsSpec.configurationSource(corsConfigurationSource))
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint((exchange, ex) -> writeError(exchange, HttpStatus.UNAUTHORIZED,
                "Unauthorized"))
            .accessDeniedHandler((exchange, denied) -> writeError(exchange, HttpStatus.FORBIDDEN,
                "Forbidden")))
                .oauth2ResourceServer(oauth2 -> oauth2
                    .authenticationEntryPoint((exchange, ex) ->
                        writeError(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized"))
                    .accessDeniedHandler((exchange, denied) ->
                        writeError(exchange, HttpStatus.FORBIDDEN, "Forbidden"))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.allowed-origins}") String allowedOriginsRaw) {
        List<String> configuredPatterns = Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        return exchange -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOriginPatterns(new ArrayList<>(configuredPatterns));
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(List.of(
                    "Authorization",
                    "X-Skip-Auth-Redirect",
                    "Content-Type",
                    "Idempotency-Key",
                    "Accept"
            ));
            config.setExposedHeaders(List.of("Content-Type"));
            config.setAllowCredentials(false);

            String origin = exchange.getRequest().getHeaders().getFirst(HttpHeaders.ORIGIN);
            if (origin != null && !origin.isBlank() && originHostMatchesRequestHost(origin, exchange)) {
                config.addAllowedOriginPattern(origin);
            }

            return config;
        };
    }

    /**
     * Allow browser Origin when its host matches the request Host / X-Forwarded-Host (one public domain → Nginx → / + /api).
     */
    private static boolean originHostMatchesRequestHost(String origin, ServerWebExchange exchange) {
        String requestHost = resolveRequestHost(exchange);
        if (requestHost == null || requestHost.isBlank()) {
            return false;
        }
        try {
            URI u = URI.create(origin);
            String originHost = u.getHost();
            if (originHost == null) {
                return false;
            }
            return originHost.equalsIgnoreCase(stripHostPort(requestHost));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String resolveRequestHost(ServerWebExchange exchange) {
        var headers = exchange.getRequest().getHeaders();
        String forwarded = headers.getFirst("X-Forwarded-Host");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma >= 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String host = headers.getFirst(HttpHeaders.HOST);
        return host != null ? host.trim() : null;
    }

    private static String stripHostPort(String host) {
        if (host.startsWith("[")) {
            int end = host.indexOf(']');
            if (end > 0) {
                return host.substring(1, end);
            }
        }
        int colon = host.lastIndexOf(':');
        if (colon > 0 && !host.substring(0, colon).contains(":")) {
            return host.substring(0, colon);
        }
        return host;
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(@Value("${jwt.secret}") String jwtSecret) {
        SecretKeySpec signingKey = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(signingKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    private Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            if (role == null || role.isBlank()) {
                return Collections.emptyList();
            }
            return Collections.singletonList(() -> "ROLE_" + role);
        });
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("message", message);
        body.put("data", null);

        byte[] bytes;
        try {
            bytes = OBJECT_MAPPER.writeValueAsBytes(body);
        } catch (JsonProcessingException ex) {
            bytes = "{\"status\":500,\"message\":\"Internal server error\",\"data\":null}".getBytes(StandardCharsets.UTF_8);
        }

        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }
}
