package com.chargeroute.user.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom business metrics for user management.
 * Provides ChargeRoute-specific observability for user operations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserObservabilityMetrics {

    private final MeterRegistry meterRegistry;

    // Gauges - current values
    private final AtomicInteger activeUsers = new AtomicInteger(0);

    // Counters - cumulative values
    private Counter usersRegisteredCounter;
    private Counter userLoginsCounter;

    @PostConstruct
    public void init() {
        log.info("Initializing custom user metrics");

        // Register gauge for active users
        Gauge.builder("chargeroute.users.active", activeUsers, AtomicInteger::get)
                .description("Number of active user sessions")
                .tag("service", "user-management")
                .register(meterRegistry);

        // Register counters
        usersRegisteredCounter = Counter.builder("chargeroute.users.registered")
                .description("Total number of registered users")
                .tag("service", "user-management")
                .register(meterRegistry);

        userLoginsCounter = Counter.builder("chargeroute.users.logins")
                .description("Total number of user login attempts")
                .tag("service", "user-management")
                .register(meterRegistry);

        log.info("Custom user metrics initialized successfully");
    }

    /**
     * Increment the active users counter
     */
    public void incrementActiveUsers() {
        activeUsers.incrementAndGet();
        log.debug("Active users incremented to: {}", activeUsers.get());
    }

    /**
     * Decrement the active users counter
     */
    public void decrementActiveUsers() {
        activeUsers.decrementAndGet();
        log.debug("Active users decremented to: {}", activeUsers.get());
    }

    /**
     * Record a user registration event
     */
    public void recordUserRegistered() {
        usersRegisteredCounter.increment();
        log.info("User registration recorded. Total registered: {}",
                (long) usersRegisteredCounter.count());
    }

    /**
     * Record a user login event
     */
    public void recordUserLogin() {
        userLoginsCounter.increment();
        incrementActiveUsers();
        log.info("User login recorded. Total logins: {}, Active users: {}",
                (long) userLoginsCounter.count(), activeUsers.get());
    }

    /**
     * Record a user logout event
     */
    public void recordUserLogout() {
        decrementActiveUsers();
        log.info("User logout recorded. Active users: {}", activeUsers.get());
    }

    /**
     * Get current count of active users
     *
     * @return current active user count
     */
    public int getActiveUsers() {
        return activeUsers.get();
    }
}
