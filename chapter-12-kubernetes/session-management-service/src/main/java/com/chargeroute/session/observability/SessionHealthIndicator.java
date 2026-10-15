package com.chargeroute.session.observability;

import com.chargeroute.session.repository.ChargingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Custom health indicator for Session Management Service.
 * Provides detailed health information about session service capabilities.
 */
@Component("sessionServiceHealth")
@RequiredArgsConstructor
@Slf4j
public class SessionHealthIndicator implements ReactiveHealthIndicator {

    private final ChargingSessionRepository sessionRepository;
    private final SessionObservabilityMetrics sessionMetrics;

    @Override
    public Mono<Health> health() {
        return checkSessionServiceHealth()
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(ex -> {
                    log.error("Health check failed", ex);
                    return Mono.just(Health.down()
                            .withDetail("error", ex.getMessage())
                            .withDetail("errorType", ex.getClass().getSimpleName())
                            .build());
                });
    }

    private Mono<Health> checkSessionServiceHealth() {
        return sessionRepository.count()
                .map(count -> {
                    int activeSessions = sessionMetrics.getActiveSessions();

                    Health.Builder builder = Health.up()
                            .withDetail("database", "UP")
                            .withDetail("totalSessions", count)
                            .withDetail("activeSessions", activeSessions);

                    // Add warning if there are too many active sessions
                    if (activeSessions > 1000) {
                        builder.withDetail("warning", "High number of active sessions");
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
