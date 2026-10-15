package com.chargeroute.notification.consumer;

import com.chargeroute.notification.dto.Notification;
import com.chargeroute.notification.entity.NotificationEntity;
import com.chargeroute.notification.event.UserRegisteredEvent;
import com.chargeroute.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.function.Consumer;

/**
 * Event consumer that listens for user-related events from RabbitMQ.
 * <p>
 * This demonstrates event-driven integration between services:
 * - User service publishes UserRegisteredEvent to RabbitMQ
 * - Notification service consumes the event asynchronously
 * - Services are loosely coupled - no direct HTTP calls
 * <p>
 * Key patterns demonstrated:
 * - Functional programming model (Consumer<T>)
 * - Idempotent event processing (check eventId to prevent duplicates)
 * - Error handling without blocking the event stream
 * - Asynchronous notification sending
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final NotificationRepository notificationRepository;

    /**
     * Consume UserRegisteredEvent to send welcome notification.
     * <p>
     * Spring Cloud Stream automatically deserializes the event and invokes this consumer.
     * The function name must match the binding configuration in application.yml.
     * <p>
     * Pattern: Idempotent consumer
     * - Check if event already processed (by eventId)
     * - Process only once even if event is delivered multiple times
     */
    @Bean
    public Consumer<UserRegisteredEvent> handleUserRegistered() {
        return event -> {
            log.info("Received UserRegisteredEvent for user: {} (eventId: {}, correlationId: {})",
                    event.getUserId(), event.getEventId(), event.getCorrelationId());

            try {
                // Check if we've already processed this event (idempotency)
                // Using notification ID derived from event ID
                String notificationId = "notif-" + event.getEventId();
                boolean alreadyProcessed = notificationRepository.existsById(notificationId);

                if (alreadyProcessed) {
                    log.warn("Event {} already processed (notification {} exists), skipping",
                            event.getEventId(), notificationId);
                    return;
                }

                // Create welcome notification
                NotificationEntity notification = new NotificationEntity();
                notification.setNotificationId("notif-" + event.getEventId());  // Use eventId as part of ID for idempotency
                notification.setUserId(event.getUserId());
                notification.setSubject("Welcome to ChargeRoute!");
                notification.setMessage(buildWelcomeMessage(event.getFullName(), event.getMembershipTier()));
                notification.setChannel(NotificationEntity.NotificationChannel.EMAIL);
                notification.setType(NotificationEntity.NotificationType.ACCOUNT_UPDATE);
                notification.setStatus(NotificationEntity.NotificationStatus.PENDING);

                // Save notification (will be sent by NotificationService asynchronously)
                NotificationEntity saved = notificationRepository.save(notification);

                log.info("Created welcome notification {} for user: {}",
                        saved.getNotificationId(), event.getUserId());

                // In real implementation, NotificationService would pick this up and send it
                // For now, just log success
                log.info("Welcome notification queued for user: {} (correlation: {})",
                        event.getUserId(), event.getCorrelationId());

            } catch (Exception e) {
                // Log error but don't throw - prevents message from going to DLQ immediately
                // Spring Cloud Stream will retry based on configuration
                log.error("Error processing UserRegisteredEvent for user {}: {}",
                        event.getUserId(), e.getMessage(), e);
                throw e;  // Rethrow to trigger retry
            }
        };
    }

    /**
     * Build personalized welcome message based on membership tier.
     */
    private String buildWelcomeMessage(String fullName, String membershipTier) {
        return String.format("""
                Hello %s,

                Welcome to ChargeRoute! We're excited to have you join our community of EV drivers.

                Your %s membership gives you access to our growing network of charging stations.

                Get started:
                1. Download the ChargeRoute mobile app
                2. Find nearby charging stations
                3. Start your first charging session

                Need help? Contact our support team anytime.

                Happy charging!
                The ChargeRoute Team
                """, fullName, membershipTier);
    }
}
