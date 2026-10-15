package com.chargeroute.chargeunit.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom business metrics for charge units.
 * Provides ChargeRoute-specific observability for charge unit operations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChargeUnitObservabilityMetrics {

    private final MeterRegistry meterRegistry;

    // Gauges - current values
    private final AtomicInteger availableChargeUnits = new AtomicInteger(0);
    private final AtomicInteger utilizationPercentage = new AtomicInteger(0);

    // Counters - cumulative values
    private Counter reservationsCounter;
    private Counter failuresCounter;

    @PostConstruct
    public void init() {
        log.info("Initializing custom charge unit metrics");

        // Register gauge for available charge units
        Gauge.builder("chargeroute.chargeunits.available", availableChargeUnits, AtomicInteger::get)
                .description("Number of available charge units")
                .tag("service", "charge-unit-management")
                .register(meterRegistry);

        // Register gauge for utilization percentage
        Gauge.builder("chargeroute.chargeunits.utilization", utilizationPercentage, AtomicInteger::get)
                .description("Charge unit utilization percentage")
                .tag("service", "charge-unit-management")
                .register(meterRegistry);

        // Register counters
        reservationsCounter = Counter.builder("chargeroute.chargeunits.reservations")
                .description("Total number of charge unit reservation requests")
                .tag("service", "charge-unit-management")
                .register(meterRegistry);

        failuresCounter = Counter.builder("chargeroute.chargeunits.failures")
                .description("Total number of charge unit failures and errors")
                .tag("service", "charge-unit-management")
                .register(meterRegistry);

        log.info("Custom charge unit metrics initialized successfully");
    }

    /**
     * Update the available charge units count
     *
     * @param count the current count of available charge units
     */
    public void setAvailableChargeUnits(int count) {
        availableChargeUnits.set(count);
        log.debug("Available charge units updated to: {}", count);
    }

    /**
     * Update the utilization percentage
     *
     * @param percentage the current utilization percentage (0-100)
     */
    public void setUtilizationPercentage(int percentage) {
        utilizationPercentage.set(percentage);
        log.debug("Charge unit utilization updated to: {}%", percentage);
    }

    /**
     * Record a charge unit reservation request
     */
    public void recordReservation() {
        reservationsCounter.increment();
        log.info("Charge unit reservation recorded. Total reservations: {}",
                (long) reservationsCounter.count());
    }

    /**
     * Record a charge unit failure or error
     *
     * @param reason the reason for the failure
     */
    public void recordFailure(String reason) {
        failuresCounter.increment();
        log.warn("Charge unit failure recorded. Reason: {}. Total failures: {}",
                reason, (long) failuresCounter.count());
    }

    /**
     * Get current count of available charge units
     *
     * @return current available charge unit count
     */
    public int getAvailableChargeUnits() {
        return availableChargeUnits.get();
    }

    /**
     * Get current utilization percentage
     *
     * @return current utilization percentage
     */
    public int getUtilizationPercentage() {
        return utilizationPercentage.get();
    }
}
