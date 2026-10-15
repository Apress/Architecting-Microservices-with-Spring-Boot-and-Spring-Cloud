package com.chargeroute.gateway.filter;

import com.chargeroute.gateway.util.CustomHttpHeader;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Custom rate limiter for charging session operations.
 * Limits users to a maximum number of charging sessions per hour
 * to prevent abuse and ensure fair usage.
 */
@Component
public class ChargingSessionRateLimiterFilter implements GatewayFilter {

    private final RedisTemplate<String, String> redisTemplate;
    private final int maxSessionsPerHour;

    public ChargingSessionRateLimiterFilter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.maxSessionsPerHour = 5; // Default limit
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        if (!isChargingSessionRequest(exchange)) {
            return chain.filter(exchange);
        }

        String userId = extractUserId(exchange);

        String key = "charging_session_limit:" + userId;

        return checkSessionLimit(key)
                .flatMap(allowed -> {
                    if (allowed) {
                        return chain.filter(exchange);
                    } else {
                        return handleRateLimitExceeded(exchange);
                    }
                });
    }

    /**
     * Check if this is a charging session creation request.
     */
    private boolean isChargingSessionRequest(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();

        // Check for POST requests to session endpoints
        return "POST".equals(method) &&
                (path.contains("/api/sessions") || path.contains("/sessions/start"));
    }

    /**
     * Extract user ID from request headers.
     */
    private String extractUserId(ServerWebExchange exchange) {
        return exchange.getRequest()
                .getHeaders()
                .getFirst(CustomHttpHeader.X_USER_ID);
    }

    /**
     * Check if user has exceeded session limit using Redis.
     */
    private Mono<Boolean> checkSessionLimit(String key) {
        return Mono.fromSupplier(() -> {
            try {
                long count = redisTemplate.opsForValue().increment(key);

                if (count == 1) {
                    // First request, set expiry to 1 hour
                    redisTemplate.expire(key, Duration.ofHours(1));
                }
                return count <= maxSessionsPerHour;

            } catch (Exception e) {
                // If Redis is down, allow the request through
                return true;
            }
        });
    }

    /**
     * Handle rate limit exceeded scenario.
     */
    private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        response.getHeaders().add(CustomHttpHeader.X_RATELIMIT_RETRY_AFTER, "3600");

        String body = """
                {
                    "error": "CHARGING_SESSION_LIMIT_EXCEEDED",
                    "message": "Maximum charging sessions per hour exceeded",
                    "limit": %d,
                    "retryAfter": 3600,
                    "upgradeUrl": "https://chargeroute.com/pricing"
                }
                """.formatted(maxSessionsPerHour);

        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}