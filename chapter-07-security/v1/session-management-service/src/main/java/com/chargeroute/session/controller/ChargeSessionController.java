package com.chargeroute.session.controller;

import com.chargeroute.session.dto.ChargeSession;
import com.chargeroute.session.dto.SessionMetrics;
import com.chargeroute.session.dto.SessionSummary;
import com.chargeroute.session.dto.StartSessionRequest;
import com.chargeroute.session.dto.StopSessionRequest;
import com.chargeroute.session.dto.client.ChargeUnit;
import com.chargeroute.session.dto.client.User;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/sessions")
@Slf4j
public class ChargeSessionController {

    private final WebClient webClient;

    // Service names for Eureka discovery - no more hardcoded URLs!
    private static final String USER_SERVICE_URL = "http://USER-MANAGEMENT-SERVICE";
    private static final String CHARGE_UNIT_SERVICE_URL = "http://CHARGE-UNIT-MANAGEMENT-SERVICE";

    public ChargeSessionController(WebClient webClient) {
        this.webClient = webClient;
    }

    // Simple in-memory storage for demo purposes
    private final Map<String, ChargeSession> activeSessions = new ConcurrentHashMap<>();

    @GetMapping("/admin/health")
    @PreAuthorize("hasAuthority('SCOPE_admin.all')")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "service", "Charge Session Service",
                "status", "UP",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @PostMapping("/start")
    @PreAuthorize("hasAuthority('SCOPE_charging.start')")
    public ResponseEntity<ChargeSession> startSession(@Valid @RequestBody StartSessionRequest request) {
        // First, verify the user exists by calling User Management Service
        // Using WebClient with OAuth2 client credentials for service-to-service auth
        try {
            // Validate user using service discovery with OAuth2 token
            String userUrl = USER_SERVICE_URL + "/api/users/" + request.getUserId();
            log.info("Calling User Service via Eureka at: {}", userUrl);
            User user = this.webClient.get()
                    .uri(userUrl)
                    .retrieve()
                    .bodyToMono(User.class)
                    .block();

            if (user == null) {
                log.warn("User not found: {}", request.getUserId());
                return ResponseEntity.badRequest().build();
            }

            // Check charge unit availability using service discovery with OAuth2 token
            String unitUrl = CHARGE_UNIT_SERVICE_URL + "/api/charge-units/" + request.getChargeUnitId();
            log.info("Calling Charge Unit Service via Eureka at: {}", unitUrl);
            ChargeUnit chargeUnit = this.webClient.get()
                    .uri(unitUrl)
                    .retrieve()
                    .bodyToMono(ChargeUnit.class)
                    .block();

            if (chargeUnit == null || !"AVAILABLE".equals(chargeUnit.getStatus())) {
                log.warn("Charge unit not available: {}", request.getChargeUnitId());
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            // User exists and unit is available, create the charging session
            ChargeSession session = ChargeSession.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(user.getId())
                    .userEmail(user.getEmail())  // Retrieved from User Service
                    .chargeUnitId(chargeUnit.getId())
                    .locationId(chargeUnit.getLocationId())
                    .status("ACTIVE")
                    .startTime(LocalDateTime.now())
                    .paymentMethodId(request.getPaymentMethodId())
                    .metrics(SessionMetrics.builder()
                            .averagePower(0.0)
                            .maxPower(0.0)
                            .chargingDurationMinutes(0)
                            .efficiency(95.0)
                            .additionalMetrics(new HashMap<>())
                            .build())
                    .build();

            // Store the session
            this.activeSessions.put(session.getId(), session);

            log.info("Starting charging session {} for user: {} ({}) at unit: {}",
                    session.getId(), user.getId(), user.getEmail(), chargeUnit.getId());

            return ResponseEntity.ok(session);
        } catch (WebClientResponseException e) {
            log.error("Failed to start session due to service communication error: {} {}",
                    e.getStatusCode(), e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            log.error("Failed to start session due to unexpected error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @PostMapping("/stop")
    @PreAuthorize("hasAuthority('SCOPE_charging.start')")
    public ResponseEntity<SessionSummary> stopSession(@Valid @RequestBody StopSessionRequest request) {
        log.info("Stopping session {} for user: {}", request.getSessionId(), request.getUserId());

        ChargeSession session = this.activeSessions.get(request.getSessionId());
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        // Verify user owns this session
        if (!session.getUserId().equals(request.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Calculate session summary
        LocalDateTime endTime = LocalDateTime.now();
        long durationMinutes = ChronoUnit.MINUTES.between(session.getStartTime(), endTime);
        Double energyDelivered = calculateEnergyDelivered(durationMinutes);
        BigDecimal totalCost = calculateTotalCost(energyDelivered);

        // Update session
        session.setEndTime(endTime);
        session.setStatus("COMPLETED");
        session.setEnergyDelivered(energyDelivered);
        session.setTotalCost(totalCost);

        // Remove from active sessions
        this.activeSessions.remove(request.getSessionId());

        SessionSummary summary = SessionSummary.builder()
                .sessionId(request.getSessionId())
                .startTime(session.getStartTime())
                .endTime(endTime)
                .energyDelivered(energyDelivered)
                .totalCost(totalCost)
                .chargeUnitId(session.getChargeUnitId())
                .locationName("Main Street Station")
                .build();

        log.info("Session {} stopped. Duration: {} minutes, Energy: {} kWh, Cost: ${}",
                request.getSessionId(), durationMinutes, energyDelivered, totalCost);

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{sessionId}")
    @PreAuthorize("hasAuthority('SCOPE_admin.all') or hasAuthority('SCOPE_charging.read')")
    public ResponseEntity<ChargeSession> getSession(@PathVariable String sessionId) {
        ChargeSession session = this.activeSessions.get(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        // Update real-time metrics for active sessions
        if ("ACTIVE".equals(session.getStatus())) {
            updateSessionMetrics(session);
        }

        return ResponseEntity.ok(session);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('SCOPE_admin.all')")
    public ResponseEntity<Map<String, Object>> getActiveSessions() {
        Map<String, Object> response = Map.of(
                "totalActive", this.activeSessions.size(),
                "sessions", this.activeSessions.values(),
                "timestamp", LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }

    // Helper methods for demo calculations
    private Double calculateEnergyDelivered(long durationMinutes) {
        // Mock calculation: assume 50kW average charging rate
        return (50.0 * durationMinutes) / 60.0;
    }

    private BigDecimal calculateTotalCost(Double energyDelivered) {
        // Mock calculation: $0.45 per kWh
        return BigDecimal.valueOf(energyDelivered).multiply(new BigDecimal("0.45"));
    }

    private void updateSessionMetrics(ChargeSession session) {
        long durationMinutes = ChronoUnit.MINUTES.between(session.getStartTime(), LocalDateTime.now());
        SessionMetrics metrics = session.getMetrics();
        metrics.setChargingDurationMinutes((int) durationMinutes);
        metrics.setAveragePower(47.5); // Mock value
        metrics.setMaxPower(52.0); // Mock value
        session.setEnergyDelivered(calculateEnergyDelivered(durationMinutes));
    }
}
