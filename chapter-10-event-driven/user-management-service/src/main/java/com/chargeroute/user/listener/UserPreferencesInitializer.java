package com.chargeroute.user.listener;

import com.chargeroute.user.entity.UserPreferencesEntity;
import com.chargeroute.user.event.UserCreatedEvent;
import com.chargeroute.user.repository.UserPreferencesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Internal event listener that handles UserCreatedEvent.
 * <p>
 * This demonstrates Spring's @EventListener pattern for coordination WITHIN a service.
 * When a user is created, this listener automatically initializes default preferences.
 * <p>
 * Key differences from external event consumers:
 * - Uses @EventListener (not Spring Cloud Stream)
 * - Only receives events from THIS service (same JVM)
 * - Synchronous by default, but @Async makes it non-blocking
 * - No message broker involved
 * <p>
 * Use internal events for:
 * - Coordinating actions within the same bounded context
 * - Keeping service logic decoupled internally
 * - Avoiding tight coupling between components
 * <p>
 * Use external events (Spring Cloud Stream) for:
 * - Cross-service integration
 * - Eventual consistency across bounded contexts
 * - Pub/sub patterns with multiple consumers
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserPreferencesInitializer {

    private final UserPreferencesRepository preferencesRepository;

    /**
     * Handle internal UserCreatedEvent to initialize default preferences.
     * <p>
     * @Async makes this non-blocking - the user creation doesn't wait for this to complete.
     * If preference initialization fails, user creation still succeeds.
     */
    @Async
    @EventListener
    public void onUserCreated(UserCreatedEvent event) {
        log.info("Internal event received: Initializing default preferences for user: {}", event.getUserId());

        // Create default preferences
        UserPreferencesEntity preferences = new UserPreferencesEntity();
        preferences.setUserId(event.getUserId());
        preferences.setNotificationsEnabled(true);
        preferences.setNotificationChannel("EMAIL");
        preferences.setAutoChargeEnabled(false);
        preferences.setPreferredConnectorType("CCS");
        preferences.setPreferredPaymentMethod("CREDIT_CARD");

        // Save preferences (reactive)
        preferencesRepository.save(preferences)
                .doOnSuccess(saved ->
                    log.info("Default preferences initialized for user: {}", event.getUserId()))
                .doOnError(error ->
                    log.error("Failed to initialize preferences for user {}: {}",
                            event.getUserId(), error.getMessage()))
                .subscribe(); // Subscribe to trigger execution
    }
}
