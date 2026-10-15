package com.chargeroute.chargeunit.service;

import com.chargeroute.chargeunit.dto.ChargeUnit;
import com.chargeroute.chargeunit.dto.ChargeUnitRegistrationRequest;
import com.chargeroute.chargeunit.entity.ChargeUnitEntity;
import com.chargeroute.chargeunit.mapper.ChargeUnitMapper;
import com.chargeroute.chargeunit.repository.ChargeUnitRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Reactive Charge Unit Service using R2DBC.
 * Converted from blocking JPA to reactive R2DBC patterns.
 */
@Slf4j
@Service
public class ChargeUnitService {

    private final ChargeUnitRepository chargeUnitRepository;
    private final ChargeUnitMapper chargeUnitMapper;

    public ChargeUnitService(ChargeUnitRepository chargeUnitRepository, ChargeUnitMapper chargeUnitMapper) {
        this.chargeUnitRepository = chargeUnitRepository;
        this.chargeUnitMapper = chargeUnitMapper;
    }

    /**
     * Create a new charge unit from registration request
     */
    public Mono<ChargeUnit> createChargeUnit(ChargeUnitRegistrationRequest request) {
        log.info("Creating new charge unit at station: {}", request.getStationName());

        ChargeUnitEntity chargeUnitEntity = chargeUnitMapper.toEntity(request);
        chargeUnitEntity.setChargeUnitId(generateChargeUnitId());
        chargeUnitEntity.setStatus(ChargeUnitEntity.ChargeUnitStatus.OFFLINE);
        chargeUnitEntity.setCurrentPowerKw(0);

        return chargeUnitRepository.save(chargeUnitEntity)
                .doOnNext(saved -> log.info("Created charge unit with ID: {}", saved.getChargeUnitId()))
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
    public Flux<ChargeUnit> getAvailableChargeUnits() {
        log.debug("Fetching available charge units");
        return getChargeUnitsByStatus(ChargeUnitEntity.ChargeUnitStatus.AVAILABLE);
    }

    /**
     * Find charge units near a location
     */
    public Flux<ChargeUnit> findNearLocation(BigDecimal latitude, BigDecimal longitude, double radiusKm) {
        log.debug("Finding charge units near location ({}, {}) within {} km", latitude, longitude, radiusKm);
        return chargeUnitRepository.findNearLocation(latitude, longitude, radiusKm)
                .map(chargeUnitMapper::toDto);
    }

    /**
     * Generate a unique charge unit ID
     */
    private String generateChargeUnitId() {
        return "cu-" + UUID.randomUUID().toString().substring(0, 8);
    }
}

