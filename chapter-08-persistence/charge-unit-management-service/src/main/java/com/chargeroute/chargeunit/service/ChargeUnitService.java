package com.chargeroute.chargeunit.service;

import com.chargeroute.chargeunit.dto.ChargeUnit;
import com.chargeroute.chargeunit.dto.ChargeUnitRegistrationRequest;
import com.chargeroute.chargeunit.entity.ChargeUnitEntity;
import com.chargeroute.chargeunit.mapper.ChargeUnitMapper;
import com.chargeroute.chargeunit.repository.ChargeUnitRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
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
    @Transactional
    public ChargeUnit createChargeUnit(ChargeUnitRegistrationRequest request) {
        log.info("Creating new charge unit at station: {}", request.getStationName());

        // Map request to entity
        ChargeUnitEntity chargeUnitEntity = chargeUnitMapper.toEntity(request);
        chargeUnitEntity.setChargeUnitId(generateChargeUnitId());
        chargeUnitEntity.setStatus(ChargeUnitEntity.ChargeUnitStatus.OFFLINE);
        chargeUnitEntity.setCurrentPowerKw(0);

        // Save charge unit
        ChargeUnitEntity savedChargeUnit = chargeUnitRepository.save(chargeUnitEntity);
        log.info("Created charge unit with ID: {}", savedChargeUnit.getChargeUnitId());

        return chargeUnitMapper.toDto(savedChargeUnit);
    }

    /**
     * Get charge unit by ID
     */
    public Optional<ChargeUnit> getChargeUnit(String chargeUnitId) {
        log.debug("Fetching charge unit: {}", chargeUnitId);
        return chargeUnitRepository.findById(chargeUnitId)
                .map(chargeUnitMapper::toDto);
    }

    /**
     * Get all charge units
     */
    public List<ChargeUnit> getAllChargeUnits() {
        log.debug("Fetching all charge units");
        return chargeUnitRepository.findAll().stream()
                .map(chargeUnitMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Update charge unit information
     */
    @Transactional
    public ChargeUnit updateChargeUnit(String chargeUnitId, ChargeUnit chargeUnitDto) {
        log.info("Updating charge unit: {}", chargeUnitId);

        ChargeUnitEntity chargeUnitEntity = chargeUnitRepository.findById(chargeUnitId)
                .orElseThrow(() -> new IllegalArgumentException("Charge unit not found: " + chargeUnitId));

        // Update only non-null fields
        chargeUnitMapper.updateEntityFromDto(chargeUnitDto, chargeUnitEntity);

        ChargeUnitEntity updatedChargeUnit = chargeUnitRepository.save(chargeUnitEntity);
        log.info("Updated charge unit: {}", chargeUnitId);

        return chargeUnitMapper.toDto(updatedChargeUnit);
    }

    /**
     * Delete charge unit by ID
     */
    @Transactional
    public void deleteChargeUnit(String chargeUnitId) {
        log.info("Deleting charge unit: {}", chargeUnitId);

        if (!chargeUnitRepository.existsById(chargeUnitId)) {
            throw new IllegalArgumentException("Charge unit not found: " + chargeUnitId);
        }

        chargeUnitRepository.deleteById(chargeUnitId);
        log.info("Deleted charge unit: {}", chargeUnitId);
    }

    /**
     * Find charge units by status
     */
    public List<ChargeUnit> getChargeUnitsByStatus(ChargeUnitEntity.ChargeUnitStatus status) {
        log.debug("Fetching charge units with status: {}", status);
        return chargeUnitRepository.findByStatus(status).stream()
                .map(chargeUnitMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Find available charge units
     */
    public List<ChargeUnit> getAvailableChargeUnits() {
        log.debug("Fetching available charge units");
        return getChargeUnitsByStatus(ChargeUnitEntity.ChargeUnitStatus.AVAILABLE);
    }

    /**
     * Find charge units near a location
     */
    public List<ChargeUnit> findNearLocation(BigDecimal latitude, BigDecimal longitude, double radiusKm) {
        log.debug("Finding charge units near location ({}, {}) within {} km", latitude, longitude, radiusKm);
        return chargeUnitRepository.findNearLocation(latitude, longitude, radiusKm).stream()
                .map(chargeUnitMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Generate a unique charge unit ID
     */
    private String generateChargeUnitId() {
        return "cu-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
