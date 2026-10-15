package com.chargeroute.chargeunit.service;

import com.chargeroute.chargeunit.dto.ChargeUnitStatusUpdate;
import com.chargeroute.chargeunit.entity.ChargeUnitEntity;
import com.chargeroute.chargeunit.repository.ChargeUnitRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service for streaming real-time charge unit status updates using Server-Sent Events (SSE).
 * <p>
 * Key Reactive SSE Patterns:
 * - Sinks.Many for hot streams (multiple subscribers)
 * - Flux.interval for periodic updates
 * - multicast() for broadcasting to multiple clients
 * - onBackpressureBuffer for handling slow consumers
 * <p>
 * Hot vs Cold Streams:
 * - Cold: Each subscriber gets its own stream (like Flux.just())
 * - Hot: All subscribers share the same stream (like Sinks)
 * <p>
 * This service demonstrates:
 * 1. Real-time status updates streamed to multiple clients
 * 2. Simulated power consumption changes
 * 3. Backpressure handling for slow network connections
 */
@Slf4j
@Service
public class ChargeUnitStreamingService {

    private final ChargeUnitRepository chargeUnitRepository;

    // Hot stream - all subscribers receive the same events
    private final Sinks.Many<ChargeUnitStatusUpdate> statusSink;

    public ChargeUnitStreamingService(ChargeUnitRepository chargeUnitRepository) {
        this.chargeUnitRepository = chargeUnitRepository;

        // Create a multicast sink with backpressure buffer
        // - multicast(): Multiple subscribers can listen
        // - onBackpressureBuffer(): Buffer events if subscriber is slow
        this.statusSink = Sinks.many().multicast().onBackpressureBuffer();

        // Start background simulation of status changes
        startStatusSimulation();
    }

    /**
     * Stream real-time status updates for all charge units.
     * <p>
     * SSE Pattern: Flux<T> with text/event-stream media type
     * - Each element is sent as a separate SSE event
     * - Connection stays open until client disconnects
     * - Automatic reconnection supported by browsers
     *
     * @return Flux of status updates
     */
    public Flux<ChargeUnitStatusUpdate> streamAllStatuses() {
        log.info("Client subscribed to all charge unit status updates");

        return statusSink.asFlux()
                .doOnSubscribe(sub -> log.info("New SSE subscriber connected"))
                .doOnCancel(() -> log.info("SSE subscriber disconnected"))
                .doOnError(error -> log.error("Error in SSE stream", error));
    }

    /**
     * Stream status updates for a specific charge unit.
     * <p>
     * SSE Pattern: Filter hot stream for specific entity
     * - filter() creates a derived stream
     * - Only relevant updates are sent to client
     *
     * @param chargeUnitId The charge unit to monitor
     * @return Flux of status updates for the specific unit
     */
    public Flux<ChargeUnitStatusUpdate> streamUnitStatus(String chargeUnitId) {
        log.info("Client subscribed to status updates for charge unit: {}", chargeUnitId);

        return statusSink.asFlux()
                .filter(update -> update.getChargeUnitId().equals(chargeUnitId))
                .doOnSubscribe(sub -> log.info("New SSE subscriber for unit: {}", chargeUnitId))
                .doOnCancel(() -> log.info("SSE subscriber disconnected for unit: {}", chargeUnitId));
    }

    /**
     * Stream available charge units (status = AVAILABLE).
     * <p>
     * SSE Pattern: Filter and transform hot stream
     * - filter() for business logic
     * - map() could transform to different DTO if needed
     *
     * @return Flux of available charge unit updates
     */
    public Flux<ChargeUnitStatusUpdate> streamAvailableUnits() {
        log.info("Client subscribed to available charge unit updates");

        return statusSink.asFlux()
                .filter(update -> "AVAILABLE".equals(update.getStatus()))
                .doOnSubscribe(sub -> log.info("New SSE subscriber for available units"))
                .doOnCancel(() -> log.info("SSE subscriber for available units disconnected"));
    }

    /**
     * Emit a status update to all subscribers.
     * <p>
     * Sinks Pattern: tryEmitNext for non-blocking emission
     * - tryEmitNext() attempts to emit without blocking
     * - Returns EmitResult to check success
     * - Fails if backpressure buffer is full
     *
     * @param update The status update to broadcast
     */
    public void emitStatusUpdate(ChargeUnitStatusUpdate update) {
        Sinks.EmitResult result = statusSink.tryEmitNext(update);

        if (result.isFailure()) {
            log.warn("Failed to emit status update for {}: {}",
                    update.getChargeUnitId(), result);
        } else {
            log.debug("Emitted status update for {}: {}",
                    update.getChargeUnitId(), update.getStatus());
        }
    }

    /**
     * Start background simulation of charge unit status changes.
     * <p>
     * Reactive Pattern: Flux.interval for periodic operations
     * - interval() emits incrementing Long every N duration
     * - flatMap queries database reactively
     * - subscribe() starts the stream (hot stream)
     * <p>
     * In production, this would listen to real hardware events,
     * message queues, or database change streams.
     */
    private void startStatusSimulation() {
        log.info("Starting charge unit status simulation");

        Flux.interval(Duration.ofSeconds(5))
                .flatMap(tick -> chargeUnitRepository.findAll())
                .flatMap(this::simulateStatusChange)
                .subscribe(
                        this::emitStatusUpdate,
                        error -> log.error("Error in status simulation", error),
                        () -> log.info("Status simulation completed")
                );
    }

    /**
     * Simulate a status change for a charge unit.
     * <p>
     * In production, this would be real sensor data or events.
     *
     * @param unit The charge unit entity
     * @return Mono of status update
     */
    private reactor.core.publisher.Mono<ChargeUnitStatusUpdate> simulateStatusChange(ChargeUnitEntity unit) {
        return reactor.core.publisher.Mono.fromSupplier(() -> {
            // Simulate random power fluctuations for occupied units
            Integer currentPower = unit.getCurrentPowerKw();
            String message = null;

            if ("OCCUPIED".equals(unit.getStatus().toString())) {
                // Random fluctuation ±5 kW
                int fluctuation = ThreadLocalRandom.current().nextInt(-5, 6);
                currentPower = Math.max(0, Math.min(unit.getMaxPowerKw(),
                        (currentPower != null ? currentPower : 0) + fluctuation));
                message = "Charging in progress";
            } else if ("AVAILABLE".equals(unit.getStatus().toString())) {
                currentPower = 0;
                message = "Ready for charging";
            } else if ("MAINTENANCE".equals(unit.getStatus().toString())) {
                currentPower = 0;
                message = "Unit under maintenance";
            } else if ("OFFLINE".equals(unit.getStatus().toString())) {
                currentPower = 0;
                message = "Unit offline";
            }

            return ChargeUnitStatusUpdate.builder()
                    .chargeUnitId(unit.getChargeUnitId())
                    .stationName(unit.getStationName())
                    .status(unit.getStatus().toString())
                    .currentPowerKw(currentPower)
                    .maxPowerKw(unit.getMaxPowerKw())
                    .timestamp(LocalDateTime.now())
                    .message(message)
                    .build();
        });
    }
}
