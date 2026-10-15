package com.chargeroute.session.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom business metrics for charging sessions.
 * Provides ChargeRoute-specific observability for session operations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionObservabilityMetrics {

    private final MeterRegistry meterRegistry;

    // Gauges - current values
    private final AtomicInteger activeSessions = new AtomicInteger(0);

    // Counters - cumulative values
    private Counter sessionsStartedCounter;
    private Counter sessionsCompletedCounter;
    private Counter sessionsFailedCounter;

    // Timers - duration tracking
    private Timer sessionDurationTimer;

    @PostConstruct
    public void init() {
        log.info("Initializing custom session metrics");

        // Register gauge for active sessions
        Gauge.builder("chargeroute.sessions.active", activeSessions, AtomicInteger::get)
                .description("Number of active charging sessions")
                .tag("service", "session-management")
                .register(meterRegistry);

        // Register counters
        sessionsStartedCounter = Counter.builder("chargeroute.sessions.started")
                .description("Total number of charging sessions started")
                .tag("service", "session-management")
                .register(meterRegistry);

        sessionsCompletedCounter = Counter.builder("chargeroute.sessions.completed")
                .description("Total number of charging sessions completed")
                .tag("service", "session-management")
                .register(meterRegistry);

        sessionsFailedCounter = Counter.builder("chargeroute.sessions.failed")
                .description("Total number of charging sessions failed")
                .tag("service", "session-management")
                .register(meterRegistry);

        // Register timer for session duration
        sessionDurationTimer = Timer.builder("chargeroute.sessions.duration")
                .description("Duration of charging sessions from start to completion")
                .tag("service", "session-management")
                .register(meterRegistry);

        log.info("Custom session metrics initialized successfully");
    }

    /**
     * Increment the active sessions counter
     */
    public void incrementActiveSessions() {
        activeSessions.incrementAndGet();
        log.debug("Active sessions incremented to: {}", activeSessions.get());
    }

    /**
     * Decrement the active sessions counter
     */
    public void decrementActiveSessions() {
        activeSessions.decrementAndGet();
        log.debug("Active sessions decremented to: {}", activeSessions.get());
    }

    /**
     * Record a session start event
     */
    public void recordSessionStarted() {
        sessionsStartedCounter.increment();
        incrementActiveSessions();
        log.info("Session started recorded. Total started: {}, Active: {}",
                (long) sessionsStartedCounter.count(), activeSessions.get());
    }

    /**
     * Record a session completion event with duration
     *
     * @param duration the duration of the session
     */
    public void recordSessionCompleted(Duration duration) {
        sessionsCompletedCounter.increment();
        decrementActiveSessions();
        sessionDurationTimer.record(duration);
        log.info("Session completed recorded. Duration: {} minutes, Total completed: {}, Active: {}",
                duration.toMinutes(), (long) sessionsCompletedCounter.count(), activeSessions.get());
    }

    /**
     * Record a session failure event
     */
    public void recordSessionFailed() {
        sessionsFailedCounter.increment();
        decrementActiveSessions();
        log.warn("Session failed recorded. Total failed: {}, Active: {}",
                (long) sessionsFailedCounter.count(), activeSessions.get());
    }

    /**
     * Get current count of active sessions
     *
     * @return current active session count
     */
    public int getActiveSessions() {
        return activeSessions.get();
    }
}
