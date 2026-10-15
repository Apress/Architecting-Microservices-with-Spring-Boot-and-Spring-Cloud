package com.chargeroute.chargeunit.controller;

import com.chargeroute.chargeunit.dto.ChargeUnit;
import com.chargeroute.chargeunit.dto.ChargeUnitRegistrationRequest;
import com.chargeroute.chargeunit.service.ChargeUnitService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/charge-units")
@Slf4j
public class ChargeUnitController {

    private final ChargeUnitService chargeUnitService;

    public ChargeUnitController(ChargeUnitService chargeUnitService) {
        this.chargeUnitService = chargeUnitService;
    }

    @GetMapping("/admin/health")
    @PreAuthorize("hasAuthority('SCOPE_admin.all')")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "service", "Charge Unit Management Service",
                "status", "UP",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Create a new charge unit
     */
    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_admin.all')")
    public ResponseEntity<ChargeUnit> createChargeUnit(@Valid @RequestBody ChargeUnitRegistrationRequest request) {
        log.info("Creating new charge unit at station: {}", request.getStationName());
        ChargeUnit createdChargeUnit = chargeUnitService.createChargeUnit(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdChargeUnit.getChargeUnitId())
                .toUri();

        return ResponseEntity.created(location).body(createdChargeUnit);
    }

    /**
     * Get charge unit by ID
     */
    @GetMapping("/{chargeUnitId}")
    @PreAuthorize("hasAuthority('SCOPE_admin.all') or hasAuthority('SCOPE_charging.read') or hasAuthority('SCOPE_service.internal')")
    public ResponseEntity<ChargeUnit> getChargeUnit(@PathVariable String chargeUnitId) {
        log.debug("Fetching charge unit: {}", chargeUnitId);
        return chargeUnitService.getChargeUnit(chargeUnitId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all charge units
     */
    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_admin.all') or hasAuthority('SCOPE_charging.read') or hasAuthority('SCOPE_service.internal')")
    public ResponseEntity<List<ChargeUnit>> getAllChargeUnits() {
        log.debug("Fetching all charge units");
        List<ChargeUnit> chargeUnits = chargeUnitService.getAllChargeUnits();
        return ResponseEntity.ok(chargeUnits);
    }

    /**
     * Get available charge units
     */
    @GetMapping("/available")
    @PreAuthorize("hasAuthority('SCOPE_admin.all') or hasAuthority('SCOPE_charging.read') or hasAuthority('SCOPE_service.internal')")
    public ResponseEntity<List<ChargeUnit>> getAvailableChargeUnits() {
        log.debug("Fetching available charge units");
        List<ChargeUnit> availableUnits = chargeUnitService.getAvailableChargeUnits();
        return ResponseEntity.ok(availableUnits);
    }

    /**
     * Find charge units near location
     */
    @GetMapping("/near")
    @PreAuthorize("hasAuthority('SCOPE_admin.all') or hasAuthority('SCOPE_charging.read') or hasAuthority('SCOPE_service.internal')")
    public ResponseEntity<List<ChargeUnit>> findNearLocation(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam(defaultValue = "10.0") double radiusKm) {
        log.debug("Finding charge units near ({}, {}) within {} km", latitude, longitude, radiusKm);
        List<ChargeUnit> nearbyUnits = chargeUnitService.findNearLocation(latitude, longitude, radiusKm);
        return ResponseEntity.ok(nearbyUnits);
    }

    /**
     * Update charge unit
     */
    @PutMapping("/{chargeUnitId}")
    @PreAuthorize("hasAuthority('SCOPE_admin.all')")
    public ResponseEntity<ChargeUnit> updateChargeUnit(
            @PathVariable String chargeUnitId,
            @RequestBody ChargeUnit chargeUnit) {
        log.info("Updating charge unit: {}", chargeUnitId);
        try {
            ChargeUnit updatedChargeUnit = chargeUnitService.updateChargeUnit(chargeUnitId, chargeUnit);
            return ResponseEntity.ok(updatedChargeUnit);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete charge unit
     */
    @DeleteMapping("/{chargeUnitId}")
    @PreAuthorize("hasAuthority('SCOPE_admin.all')")
    public ResponseEntity<Void> deleteChargeUnit(@PathVariable String chargeUnitId) {
        log.info("Deleting charge unit: {}", chargeUnitId);
        try {
            chargeUnitService.deleteChargeUnit(chargeUnitId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
