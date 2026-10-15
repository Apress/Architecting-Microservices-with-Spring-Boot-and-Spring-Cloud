package com.chargeroute.gateway.filter;

import lombok.Getter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter that adds rate limit headers to all responses.
 * Provides clients with information about their current rate limit status.
 */
@Component
public class RateLimitHeaderFilter implements GlobalFilter, Ordered {

    private final RedisTemplate<String, String> redisTemplate;

    public RateLimitHeaderFilter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            try {
                addRateLimitHeaders(exchange);
            } catch (Exception e) {
                // If we can't get rate limit info, continue without headers
                // This prevents Redis connectivity issues from breaking requests
            }
        }));
    }

    /**
     * Add rate limit headers to the response.
     */
    private void addRateLimitHeaders(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();

        String userId = exchange.getRequest()
                .getHeaders()
                .getFirst("X-User-Id");

        RateLimitInfo info = getRateLimitInfo(userId);

        response.getHeaders().add("X-RateLimit-Limit",
                String.valueOf(info.getLimit()));
        response.getHeaders().add("X-RateLimit-Remaining",
                String.valueOf(info.getRemaining()));
        response.getHeaders().add("X-RateLimit-Reset",
                String.valueOf(info.getResetTime()));

        String tier = exchange.getRequest()
                .getHeaders()
                .getFirst("X-User-Tier");
        response.getHeaders().add("X-RateLimit-Tier", tier);
    }

    /**
     * Get rate limit information for a user.
     * In a real implementation, this would query Redis for current usage.
     */
    private RateLimitInfo getRateLimitInfo(String userId) {
        try {

            String tier = getCurrentUserTier(userId);
            int limit = getLimitForTier(tier);

            return new RateLimitInfo(limit, limit - 5, System.currentTimeMillis() + 3600000);

        } catch (Exception e) {
            // Return default values if we can't get the info
            return new RateLimitInfo(10, 5, System.currentTimeMillis() + 3600000);
        }
    }

    /**
     * Get the current user's tier.
     * In production, this would come from a user service or JWT token.
     */
    private String getCurrentUserTier(String userId) {
        // Mock implementation - in practice, query user service or decode JWT
        return "BASIC";
    }

    /**
     * Get rate limit for a given tier.
     */
    private int getLimitForTier(String tier) {
        return switch (tier) {
            case "GOLD" -> 100;
            case "SILVER" -> 50;
            case "BASIC" -> 10;
            default -> 5;
        };
    }

    @Override
    public int getOrder() {
        return -100; // Run early to capture all responses
    }

    /**
     * Rate limit information container.
     */
    @Getter
    public static class RateLimitInfo {
        private final int limit;
        private final int remaining;
        private final long resetTime;

        public RateLimitInfo(int limit, int remaining, long resetTime) {
            this.limit = limit;
            this.remaining = remaining;
            this.resetTime = resetTime;
        }
    }
}