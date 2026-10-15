package com.chargeroute.user.observability;

import com.chargeroute.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Custom health indicator for User Management Service.
 * Provides detailed health information about user service capabilities.
 */
@Component("userServiceHealth")
@RequiredArgsConstructor
@Slf4j
public class UserHealthIndicator implements ReactiveHealthIndicator {

    private final UserRepository userRepository;
    private final UserObservabilityMetrics userMetrics;

    @Override
    public Mono<Health> health() {
        return checkUserServiceHealth()
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(ex -> {
                    log.error("Health check failed", ex);
                    return Mono.just(Health.down()
                            .withDetail("error", ex.getMessage())
                            .withDetail("errorType", ex.getClass().getSimpleName())
                            .build());
                });
    }

    private Mono<Health> checkUserServiceHealth() {
        return userRepository.count()
                .map(count -> {
                    int activeUsers = userMetrics.getActiveUsers();

                    Health.Builder builder = Health.up()
                            .withDetail("database", "UP")
                            .withDetail("totalUsers", count)
                            .withDetail("activeUsers", activeUsers);

                    // Add warning if there are too many active users
                    if (activeUsers > 5000) {
                        builder.withDetail("warning", "High number of active users");
                    }

                    return builder.build();
                })
                .onErrorResume(ex -> {
                    log.error("Database health check failed", ex);
                    return Mono.just(Health.down()
                            .withDetail("database", "DOWN")
                            .withDetail("error", ex.getMessage())
                            .build());
                });
    }
}
