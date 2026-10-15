package com.chargeroute.gateway.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom business metrics for API Gateway.
 * Provides ChargeRoute-specific observability for gateway routing operations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GatewayObservabilityMetrics {

    private final MeterRegistry meterRegistry;

    // Store counters and timers per route
    private final Map<String, Counter> requestCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> errorCounters = new ConcurrentHashMap<>();
    private final Map<String, Timer> latencyTimers = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Initializing custom gateway metrics");

        // Pre-register metrics for known routes
        registerRouteMetrics("user-management-service");
        registerRouteMetrics("charge-unit-management-service");
        registerRouteMetrics("session-management-service");
        registerRouteMetrics("notification-management-service");

        log.info("Custom gateway metrics initialized successfully");
    }

    /**
     * Register metrics for a specific route
     *
     * @param routeId the route identifier
     */
    private void registerRouteMetrics(String routeId) {
        // Request counter
        Counter requestCounter = Counter.builder("chargeroute.gateway.requests")
                .description("Total number of requests routed through the gateway")
                .tag("service", "api-gateway")
                .tag("route", routeId)
                .register(meterRegistry);
        requestCounters.put(routeId, requestCounter);

        // Error counter
        Counter errorCounter = Counter.builder("chargeroute.gateway.errors")
                .description("Total number of error responses from the gateway")
                .tag("service", "api-gateway")
                .tag("route", routeId)
                .register(meterRegistry);
        errorCounters.put(routeId, errorCounter);

        // Latency timer
        Timer latencyTimer = Timer.builder("chargeroute.gateway.latency")
                .description("Request latency through the gateway by route")
                .tag("service", "api-gateway")
                .tag("route", routeId)
                .register(meterRegistry);
        latencyTimers.put(routeId, latencyTimer);

        log.debug("Registered metrics for route: {}", routeId);
    }

    /**
     * Record a request for a specific route
     *
     * @param routeId the route identifier
     */
    public void recordRequest(String routeId) {
        Counter counter = requestCounters.computeIfAbsent(routeId, this::createRequestCounter);
        counter.increment();
        log.debug("Request recorded for route: {}", routeId);
    }

    /**
     * Record an error for a specific route
     *
     * @param routeId the route identifier
     */
    public void recordError(String routeId) {
        Counter counter = errorCounters.computeIfAbsent(routeId, this::createErrorCounter);
        counter.increment();
        log.warn("Error recorded for route: {}", routeId);
    }

    /**
     * Record latency for a specific route
     *
     * @param routeId  the route identifier
     * @param duration the duration of the request
     */
    public void recordLatency(String routeId, Duration duration) {
        Timer timer = latencyTimers.computeIfAbsent(routeId, this::createLatencyTimer);
        timer.record(duration);
        log.debug("Latency recorded for route {}: {} ms", routeId, duration.toMillis());
    }

    /**
     * Create a request counter for a new route
     *
     * @param routeId the route identifier
     * @return the created counter
     */
    private Counter createRequestCounter(String routeId) {
        log.info("Creating request counter for new route: {}", routeId);
        return Counter.builder("chargeroute.gateway.requests")
                .description("Total number of requests routed through the gateway")
                .tag("service", "api-gateway")
                .tag("route", routeId)
                .register(meterRegistry);
    }

    /**
     * Create an error counter for a new route
     *
     * @param routeId the route identifier
     * @return the created counter
     */
    private Counter createErrorCounter(String routeId) {
        log.info("Creating error counter for new route: {}", routeId);
        return Counter.builder("chargeroute.gateway.errors")
                .description("Total number of error responses from the gateway")
                .tag("service", "api-gateway")
                .tag("route", routeId)
                .register(meterRegistry);
    }

    /**
     * Create a latency timer for a new route
     *
     * @param routeId the route identifier
     * @return the created timer
     */
    private Timer createLatencyTimer(String routeId) {
        log.info("Creating latency timer for new route: {}", routeId);
        return Timer.builder("chargeroute.gateway.latency")
                .description("Request latency through the gateway by route")
                .tag("service", "api-gateway")
                .tag("route", routeId)
                .register(meterRegistry);
    }
}
