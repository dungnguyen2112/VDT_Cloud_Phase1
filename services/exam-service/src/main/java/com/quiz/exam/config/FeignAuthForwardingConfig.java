package com.quiz.exam.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

@Configuration
public class FeignAuthForwardingConfig {

    @Bean
    public RequestInterceptor authForwardingInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            String authorization = null;

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                authorization = request != null ? request.getHeader("Authorization") : null;
            }

            if (authorization != null && !authorization.isBlank()) {
                // Forward whatever client sent (some clients may use different casing, e.g. "bearer ...").
                requestTemplate.header("Authorization", authorization);
                return;
            }

            // Fallback: if RequestContextHolder doesn't contain the request, pull the token from Spring Security.
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Object principal = authentication != null ? authentication.getPrincipal() : null;
            if (principal instanceof Jwt jwt) {
                requestTemplate.header("Authorization", "Bearer " + jwt.getTokenValue());
            }
        };
    }
}
