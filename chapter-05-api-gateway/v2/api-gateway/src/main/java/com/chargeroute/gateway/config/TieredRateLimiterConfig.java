package com.chargeroute.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Tiered rate limiting configuration for ChargeRoute's subscription model.
 * Provides different rate limits based on user subscription tier.
 */
@Slf4j
@Configuration
public class TieredRateLimiterConfig {

    /**
     * Gold tier rate limiter for fleet operators.
     * 100 requests per second with burst capacity of 200.
     */
    @Bean
    public RedisRateLimiter goldRateLimiter() {
        return new RedisRateLimiter(100, 200);
    }

    /**
     * Silver tier rate limiter for individual drivers.
     * 50 requests per second with burst capacity of 100.
     */
    @Bean
    public RedisRateLimiter silverRateLimiter() {
        return new RedisRateLimiter(50, 100);
    }

    /**
     * Basic tier rate limiter for free users.
     * 10 requests per second with burst capacity of 20.
     */
    @Bean
    public RedisRateLimiter basicRateLimiter() {
        return new RedisRateLimiter(10, 20);
    }

    /**
     * Tiered key resolver that incorporates user tier into the rate limit key.
     * Creates separate buckets for different tiers: "GOLD:user123", "SILVER:user456", etc.
     */
    @Bean
    public KeyResolver tieredKeyResolver() {
        return exchange -> {
            // Extract user ID and tier from headers (typically from JWT)
            String userId = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-User-Id");

            String tier = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-User-Tier");

            // Prefix with tier for separate buckets
            return Mono.just(tier + ":" + userId);
        };
    }

    /**
     * Geographic key resolver for location-based rate limiting.
     * Useful for charging station operations in different regions.
     */
    @Bean
    public KeyResolver geoKeyResolver() {
        return exchange -> {
            String region = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-Region");

             return Mono.just("region:" + region);
        };
    }

    /**
     * Simple IP-to-region mapping for demonstration.
     * In production, use a proper geolocation service.
     */
    private String getRegionFromIp(String ip) {
        // Simplified mapping - in production use MaxMind GeoIP or similar
        if (ip.startsWith("127.") || ip.startsWith("192.168.") || ip.startsWith("10.")) {
            return "local";
        }
        // Default region
        return "us-east";
    }
}