package com.chargeroute.chargeunit.service;

import com.chargeroute.chargeunit.dto.ChargeUnit;
import com.chargeroute.chargeunit.dto.ChargeUnitRegistrationRequest;
import com.chargeroute.chargeunit.entity.ChargeUnitEntity;
import com.chargeroute.chargeunit.event.ChargeUnitStatusChangedEvent;
import com.chargeroute.chargeunit.mapper.ChargeUnitMapper;
import com.chargeroute.chargeunit.observability.ChargeUnitObservabilityMetrics;
import com.chargeroute.chargeunit.repository.ChargeUnitRepository;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.annotation.NewSpan;
import io.micrometer.tracing.annotation.SpanTag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Reactive Charge Unit Service using R2DBC.
 * Converted from blocking JPA to reactive R2DBC patterns.
 * <p>
 * Event-Driven Pattern: Publishes ChargeUnitStatusChangedEvent when status changes.
 * This is a BROADCAST event - multiple services can subscribe independently.
 */
@Slf4j
@Service
public class ChargeUnitService {

    private final ChargeUnitRepository chargeUnitRepository;
    private final ChargeUnitMapper chargeUnitMapper;
    private final StreamBridge streamBridge;
    private final ChargeUnitObservabilityMetrics observabilityMetrics;

    public ChargeUnitService(ChargeUnitRepository chargeUnitRepository,
                             ChargeUnitMapper chargeUnitMapper,
                             StreamBridge streamBridge,
                             ChargeUnitObservabilityMetrics observabilityMetrics) {
        this.chargeUnitRepository = chargeUnitRepository;
        this.chargeUnitMapper = chargeUnitMapper;
        this.streamBridge = streamBridge;
        this.observabilityMetrics = observabilityMetrics;
    }

    /**
     * Create a new charge unit from registration request
     */
    @NewSpan("chargeunit-create")
    @Observed(name = "chargeunit.create", contextualName = "create-charge-unit")
    public Mono<ChargeUnit> createChargeUnit(ChargeUnitRegistrationRequest request) {
        log.info("Creating new charge unit at station: {}", request.getStationName());

        ChargeUnitEntity chargeUnitEntity = chargeUnitMapper.toEntity(request);
        chargeUnitEntity.setChargeUnitId(generateChargeUnitId());
        chargeUnitEntity.setStatus(ChargeUnitEntity.ChargeUnitStatus.OFFLINE);
        chargeUnitEntity.setCurrentPowerKw(0);

        return chargeUnitRepository.save(chargeUnitEntity)
                .doOnNext(saved -> log.info("Created charge unit with ID: {}", saved.getChargeUnitId()))
                .doOnError(error -> {
                    log.error("Failed to create charge unit", error);
                    observabilityMetrics.recordFailure("Charge unit creation failed: " + error.getMessage());
                })
                .map(chargeUnitMapper::toDto);
    }

    /**
     * Get charge unit by ID
     */
    public Mono<ChargeUnit> getChargeUnit(String chargeUnitId) {
        log.debug("Fetching charge unit: {}", chargeUnitId);
        return chargeUnitRepository.findById(chargeUnitId)
                .map(chargeUnitMapper::toDto);
    }

    /**
     * Get all charge units
     */
    public Flux<ChargeUnit> getAllChargeUnits() {
        log.debug("Fetching all charge units");
        return chargeUnitRepository.findAll()
                .map(chargeUnitMapper::toDto);
    }

    /**
     * Update charge unit information
     */
    public Mono<ChargeUnit> updateChargeUnit(String chargeUnitId, ChargeUnit chargeUnitDto) {
        log.info("Updating charge unit: {}", chargeUnitId);

        return chargeUnitRepository.findById(chargeUnitId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Charge unit not found: " + chargeUnitId)))
                .flatMap(chargeUnitEntity -> {
                    // Update only non-null fields
                    chargeUnitMapper.updateEntityFromDto(chargeUnitDto, chargeUnitEntity);
                    return chargeUnitRepository.save(chargeUnitEntity);
                })
                .doOnNext(updated -> log.info("Updated charge unit: {}", chargeUnitId))
                .map(chargeUnitMapper::toDto);
    }

    /**
     * Delete charge unit by ID
     */
    public Mono<Void> deleteChargeUnit(String chargeUnitId) {
        log.info("Deleting charge unit: {}", chargeUnitId);

        return chargeUnitRepository.existsById(chargeUnitId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new IllegalArgumentException("Charge unit not found: " + chargeUnitId));
                    }
                    return chargeUnitRepository.deleteById(chargeUnitId);
                })
                .doOnSuccess(v -> log.info("Deleted charge unit: {}", chargeUnitId));
    }

    /**
     * Find charge units by status
     */
    public Flux<ChargeUnit> getChargeUnitsByStatus(ChargeUnitEntity.ChargeUnitStatus status) {
        log.debug("Fetching charge units with status: {}", status);
        return chargeUnitRepository.findByStatus(status)
                .map(chargeUnitMapper::toDto);
    }

    /**
     * Find available charge units
     */
    @NewSpan("chargeunit-get-available")
    @Observed(name = "chargeunit.getavailable", contextualName = "get-available-charge-units")
    public Flux<ChargeUnit> getAvailableChargeUnits() {
        log.debug("Fetching available charge units");
        return getChargeUnitsByStatus(ChargeUnitEntity.ChargeUnitStatus.AVAILABLE)
                .collectList()
                .doOnNext(units -> {
                    observabilityMetrics.setAvailableChargeUnits(units.size());
                    log.info("Available charge units count updated: {}", units.size());
                })
                .flatMapMany(Flux::fromIterable);
    }

    /**
     * Find charge units near a location
     */
    @NewSpan("chargeunit-find-near-location")
    @Observed(name = "chargeunit.findnearlocation", contextualName = "find-charge-units-near-location")
    public Flux<ChargeUnit> findNearLocation(
            @SpanTag("location.latitude") BigDecimal latitude,
            @SpanTag("location.longitude") BigDecimal longitude,
            @SpanTag("location.radius") double radiusKm) {
        log.debug("Finding charge units near location ({}, {}) within {} km", latitude, longitude, radiusKm);
        return chargeUnitRepository.findNearLocation(latitude, longitude, radiusKm)
                .map(chargeUnitMapper::toDto);
    }

    /**
     * Update charge unit status and publish status change event.
     * <p>
     * This demonstrates the BROADCAST event pattern:
     * - Event published to a topic exchange
     * - Multiple services can consume independently (each has own queue)
     * - Session Service: Tracks which units are available for new sessions
     * - Notification Service: Alerts users about nearby available charging
     * - Analytics Service: Tracks utilization patterns
     * <p>
     * Event-Driven Pattern: Status changes are broadcast to all interested services.
     * Unlike targeted events (UserRegistered), this uses a fanout/topic pattern where
     * any service can subscribe without the publisher knowing who the consumers are.
     */
    @NewSpan("chargeunit-update-status")
    @Observed(name = "chargeunit.updatestatus", contextualName = "update-charge-unit-status")
    public Mono<ChargeUnit> updateStatus(
            @SpanTag("chargeunit.id") String chargeUnitId,
            @SpanTag("chargeunit.newstatus") ChargeUnitEntity.ChargeUnitStatus newStatus) {
        log.info("Updating status for charge unit {} to {}", chargeUnitId, newStatus);
        String correlationId = UUID.randomUUID().toString();

        return chargeUnitRepository.findById(chargeUnitId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Charge unit not found: " + chargeUnitId)))
                .flatMap(chargeUnit -> {
                    ChargeUnitEntity.ChargeUnitStatus previousStatus = chargeUnit.getStatus();

                    // Only proceed if status actually changed
                    if (previousStatus == newStatus) {
                        log.debug("Status unchanged for charge unit {}, skipping update", chargeUnitId);
                        return Mono.just(chargeUnit);
                    }

                    chargeUnit.setStatus(newStatus);
                    return chargeUnitRepository.save(chargeUnit)
                            .doOnNext(updated -> {
                                log.info("Updated charge unit {} from {} to {}",
                                    chargeUnitId, previousStatus, newStatus);

                                // Record metrics based on status change
                                if (newStatus == ChargeUnitEntity.ChargeUnitStatus.OCCUPIED) {
                                    observabilityMetrics.recordReservation();
                                    log.info("Recorded charge unit reservation for unit: {}", chargeUnitId);
                                }

                                // Publish broadcast event
                                String location = updated.getStationName() + " - " + updated.getAddress();
                                BigDecimal currentPower = updated.getCurrentPowerKw() != null ?
                                        BigDecimal.valueOf(updated.getCurrentPowerKw()) : BigDecimal.ZERO;
                                ChargeUnitStatusChangedEvent event = new ChargeUnitStatusChangedEvent(
                                        correlationId,
                                        updated.getChargeUnitId(),
                                        previousStatus.name(),
                                        newStatus.name(),
                                        currentPower,
                                        location
                                );

                                streamBridge.send("chargeUnitStatusChanged-out-0", event);
                                log.info("Published ChargeUnitStatusChangedEvent for unit: {}", chargeUnitId);
                            });
                })
                .doOnError(error -> {
                    log.error("Failed to update charge unit status", error);
                    observabilityMetrics.recordFailure("Status update failed: " + error.getMessage());
                })
                .map(chargeUnitMapper::toDto);
    }

    /**
     * Generate a unique charge unit ID
     */
    private String generateChargeUnitId() {
        return "cu-" + UUID.randomUUID().toString().substring(0, 8);
    }
}

