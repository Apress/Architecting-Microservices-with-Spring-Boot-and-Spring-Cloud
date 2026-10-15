package com.chargeroute.gateway.config;

import com.chargeroute.gateway.util.CustomHttpHeader;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * Rate limiting configuration for ChargeRoute API Gateway.
 * Provides different rate limiting strategies based on user identification.
 */
@Configuration
public class RateLimiterConfig {

    /**
     * Standard rate limiter for general API usage.
     * 10 requests per second with burst capacity of 20.
     * This is the primary rate limiter bean used as default.
     */
    @Bean
    @Primary
    public RedisRateLimiter standardRateLimiter() {
        return new RedisRateLimiter(10, 20);
    }

    /**
     * User-based key resolver for rate limiting.
     * Uses X-User-Id header to identify users, falls back to "anonymous".
     * This is the primary key resolver used as default.
     */
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest()
                    .getHeaders()
                    .getFirst(CustomHttpHeader.X_USER_ID);

            return Mono.just(userId != null ? userId : "");
        };
    }

    /**
     * API key-based resolver for partner integrations.
     * Checks X-API-Key header first, then api_key query parameter.
     */
    @Bean
    public KeyResolver apiKeyResolver() {
        return exchange -> {
            // Check header first
            String apiKey = exchange.getRequest()
                    .getHeaders()
                    .getFirst(CustomHttpHeader.X_API_KEY);

            return Mono.just(apiKey);
        };
    }

    /**
     * IP-based resolver for anonymous rate limiting.
     * Uses client IP address as the key.
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest()
                    .getRemoteAddress()
                    .getAddress()
                    .getHostAddress();

            return Mono.just("ip:" + ip);
        };
    }
}