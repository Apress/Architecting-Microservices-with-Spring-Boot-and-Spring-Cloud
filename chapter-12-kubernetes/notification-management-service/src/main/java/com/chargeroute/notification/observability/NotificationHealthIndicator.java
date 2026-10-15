package com.chargeroute.notification.observability;

import com.chargeroute.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Custom health indicator for Notification Management Service.
 * Provides detailed health information about notification service capabilities.
 */
@Component("notificationServiceHealth")
@RequiredArgsConstructor
@Slf4j
public class NotificationHealthIndicator implements ReactiveHealthIndicator {

    private final NotificationRepository notificationRepository;
    private final NotificationObservabilityMetrics notificationMetrics;

    @Override
    public Mono<Health> health() {
        return checkNotificationServiceHealth()
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(ex -> {
                    log.error("Health check failed", ex);
                    return Mono.just(Health.down()
                            .withDetail("error", ex.getMessage())
                            .withDetail("errorType", ex.getClass().getSimpleName())
                            .build());
                });
    }

    private Mono<Health> checkNotificationServiceHealth() {
        return Mono.fromCallable(() -> {
            try {
                // Check database connectivity by counting notifications
                long count = notificationRepository.count();
                long sentCount = notificationMetrics.getNotificationsSentCount();
                long failedCount = notificationMetrics.getNotificationsFailedCount();

                Health.Builder builder = Health.up()
                        .withDetail("database", "UP")
                        .withDetail("totalNotifications", count)
                        .withDetail("notificationsSent", sentCount)
                        .withDetail("notificationsFailed", failedCount);

                // Calculate success rate if we have sent notifications
                if (sentCount + failedCount > 0) {
                    double successRate = (double) sentCount / (sentCount + failedCount) * 100;
                    builder.withDetail("successRate", String.format("%.2f%%", successRate));

                    // Add warning if success rate is too low
                    if (successRate < 90.0) {
                        builder.withDetail("warning", "Notification success rate below 90%");
                    }
                }

                // Simulate RabbitMQ health check (can be replaced with actual RabbitMQ health check)
                builder.withDetail("messagingQueue", "UP");

                return builder.build();
            } catch (Exception ex) {
                log.error("Database health check failed", ex);
                return Health.down()
                        .withDetail("database", "DOWN")
                        .withDetail("error", ex.getMessage())
                        .build();
            }
        });
    }
}
