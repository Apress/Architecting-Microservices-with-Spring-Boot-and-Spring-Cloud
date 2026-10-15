package com.chargeroute.notification.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Custom business metrics for notifications.
 * Provides ChargeRoute-specific observability for notification operations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationObservabilityMetrics {

    private final MeterRegistry meterRegistry;

    // Counters - cumulative values
    private Counter notificationsSentCounter;
    private Counter notificationsFailedCounter;

    // Timers - duration tracking
    private Timer notificationDurationTimer;

    @PostConstruct
    public void init() {
        log.info("Initializing custom notification metrics");

        // Register counters
        notificationsSentCounter = Counter.builder("chargeroute.notifications.sent")
                .description("Total number of notifications sent")
                .tag("service", "notification-management")
                .register(meterRegistry);

        notificationsFailedCounter = Counter.builder("chargeroute.notifications.failed")
                .description("Total number of failed notifications")
                .tag("service", "notification-management")
                .register(meterRegistry);

        // Register timer for notification delivery duration
        notificationDurationTimer = Timer.builder("chargeroute.notifications.duration")
                .description("Duration of notification delivery from creation to send")
                .tag("service", "notification-management")
                .register(meterRegistry);

        log.info("Custom notification metrics initialized successfully");
    }

    /**
     * Record a notification sent event
     */
    public void recordNotificationSent() {
        notificationsSentCounter.increment();
        log.info("Notification sent recorded. Total sent: {}", (long) notificationsSentCounter.count());
    }

    /**
     * Record a notification sent event with channel tag
     *
     * @param channel the notification channel (EMAIL, SMS, PUSH)
     */
    public void recordNotificationSent(String channel) {
        Counter.builder("chargeroute.notifications.sent")
                .description("Total number of notifications sent")
                .tag("service", "notification-management")
                .tag("channel", channel)
                .register(meterRegistry)
                .increment();
        log.info("Notification sent recorded for channel: {}. Total sent: {}",
                channel, (long) notificationsSentCounter.count());
    }

    /**
     * Record a notification failure event
     */
    public void recordNotificationFailed() {
        notificationsFailedCounter.increment();
        log.warn("Notification failed recorded. Total failed: {}", (long) notificationsFailedCounter.count());
    }

    /**
     * Record a notification failure event with channel tag
     *
     * @param channel the notification channel (EMAIL, SMS, PUSH)
     */
    public void recordNotificationFailed(String channel) {
        Counter.builder("chargeroute.notifications.failed")
                .description("Total number of failed notifications")
                .tag("service", "notification-management")
                .tag("channel", channel)
                .register(meterRegistry)
                .increment();
        log.warn("Notification failed recorded for channel: {}. Total failed: {}",
                channel, (long) notificationsFailedCounter.count());
    }

    /**
     * Record notification delivery time
     *
     * @param runnable the notification delivery operation to time
     */
    public void recordNotificationDuration(Runnable runnable) {
        notificationDurationTimer.record(runnable);
        log.debug("Notification duration recorded");
    }

    /**
     * Get total notifications sent count
     *
     * @return total notifications sent
     */
    public long getNotificationsSentCount() {
        return (long) notificationsSentCounter.count();
    }

    /**
     * Get total notifications failed count
     *
     * @return total notifications failed
     */
    public long getNotificationsFailedCount() {
        return (long) notificationsFailedCounter.count();
    }
}
