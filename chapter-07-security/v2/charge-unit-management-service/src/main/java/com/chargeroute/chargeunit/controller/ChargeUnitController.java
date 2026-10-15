package com.chargeroute.chargeunit.controller;

import com.chargeroute.chargeunit.dto.ChargeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/charge-units")
@Slf4j
public class ChargeUnitController {

    @GetMapping("/admin/health")
    @PreAuthorize("hasAuthority('SCOPE_admin.all')")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "service", "Charge Unit Management Service",
                "status", "UP",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/{unitId}")
    @PreAuthorize("hasAuthority('SCOPE_admin.all') or hasAuthority('SCOPE_charging.read') or hasAuthority('SCOPE_service.internal')")
    public ResponseEntity<ChargeUnit> getChargeUnit(@PathVariable String unitId) {
        // Mock implementation for now
        ChargeUnit unit = ChargeUnit.builder()
                .id(unitId)
                .locationId("LOC-001")
                .status("AVAILABLE")
                .power(150.0)
                .type("DC_FAST")
                .connector("CCS")
                .pricePerKwh(new BigDecimal("0.45"))
                .lastMaintenanceDate(LocalDateTime.now().minusDays(30))
                .build();
        return ResponseEntity.ok(unit);
    }
}
