package com.chargeroute.session.controller;

import com.chargeroute.session.dto.ChargeSession;
import com.chargeroute.session.dto.SessionMetrics;
import com.chargeroute.session.dto.SessionSummary;
import com.chargeroute.session.dto.StartSessionRequest;
import com.chargeroute.session.dto.StopSessionRequest;
import com.chargeroute.session.dto.client.ChargeUnit;
import com.chargeroute.session.dto.client.User;
import com.chargeroute.session.service.SessionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
@Slf4j
public class ChargeSessionController {

    private final RestTemplate restTemplate;
    private final SessionService sessionService;

    // Service names for Eureka discovery - no more hardcoded URLs!
    private static final String USER_SERVICE_NAME = "user-management-service";
    private static final String CHARGE_UNIT_SERVICE_NAME = "charge-unit-management-service";

    public ChargeSessionController(RestTemplate restTemplate, SessionService sessionService) {
        this.restTemplate = restTemplate;
        this.sessionService = sessionService;
    }

    @GetMapping("/admin/health")
    @PreAuthorize("hasAuthority('SCOPE_admin.all')")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "service", "Charge Session Service",
                "status", "UP",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Create a new charging session
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_charging.start')")
    public ResponseEntity<ChargeSession> createSession(@Valid @RequestBody ChargeSession sessionDto) {
        log.info("Creating new charging session for user: {}", sessionDto.getUserId());

        // Generate session ID if not provided
        if (sessionDto.getId() == null || sessionDto.getId().isEmpty()) {
            sessionDto.setId(UUID.randomUUID().toString());
        }

        ChargeSession createdSession = sessionService.createSession(sessionDto);

        // Build location header
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdSession.getId())
                .toUri();

        return ResponseEntity.created(location).body(createdSession);
    }

    @PostMapping("/start")
    @PreAuthorize("hasAuthority('SCOPE_charging.start')")
    public ResponseEntity<ChargeSession> startSession(@Valid @RequestBody StartSessionRequest request) {
        // First, verify the user exists by calling User Management Service
        // NOW USING EUREKA SERVICE DISCOVERY - no more hardcoded URLs!
        // Service names are resolved dynamically through Eureka registry
        try {
            // Validate user using service discovery - no more hardcoded URLs!
            String userUrl = "http://" + USER_SERVICE_NAME + "/api/users/" + request.getUserId();
            log.info("Calling User Service via Eureka at: {}", userUrl);
            User user = this.restTemplate.getForObject(userUrl, User.class);

            if (user == null) {
                log.warn("User not found: {}", request.getUserId());
                return ResponseEntity.badRequest().build();
            }

            // Check charge unit availability using service discovery!
            String unitUrl = "http://" + CHARGE_UNIT_SERVICE_NAME + "/api/charge-units/" + request.getChargeUnitId();
            log.info("Calling Charge Unit Service via Eureka at: {}", unitUrl);
            ChargeUnit chargeUnit = this.restTemplate.getForObject(unitUrl, ChargeUnit.class);

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

            // Persist the session using service layer
            ChargeSession createdSession = sessionService.createSession(session);

            log.info("Starting charging session {} for user: {} ({}) at unit: {}",
                    createdSession.getId(), user.getId(), user.getEmail(), chargeUnit.getId());

            return ResponseEntity.ok(createdSession);
        } catch (RestClientException e) {
            log.error("Failed to start session due to service communication error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @PostMapping("/stop")
    @PreAuthorize("hasAuthority('SCOPE_charging.start')")
    public ResponseEntity<SessionSummary> stopSession(@Valid @RequestBody StopSessionRequest request) {
        log.info("Stopping session {} for user: {}", request.getSessionId(), request.getUserId());

        return sessionService.getSession(request.getSessionId())
                .map(session -> {
                    // Verify user owns this session
                    if (!session.getUserId().equals(request.getUserId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<SessionSummary>build();
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

                    // Persist the update
                    sessionService.updateSession(request.getSessionId(), session);

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
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get all sessions
     */
    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_admin.all') or hasAuthority('SCOPE_charging.read')")
    public ResponseEntity<List<ChargeSession>> getAllSessions() {
        log.debug("Fetching all sessions");
        List<ChargeSession> sessions = sessionService.getAllSessions();
        return ResponseEntity.ok(sessions);
    }

    /**
     * Get session by ID
     */
    @GetMapping("/{sessionId}")
    @PreAuthorize("hasAuthority('SCOPE_admin.all') or hasAuthority('SCOPE_charging.read')")
    public ResponseEntity<ChargeSession> getSession(@PathVariable String sessionId) {
        log.debug("Fetching session: {}", sessionId);
        return sessionService.getSession(sessionId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get sessions by user ID
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('SCOPE_admin.all') or hasAuthority('SCOPE_charging.read')")
    public ResponseEntity<List<ChargeSession>> getSessionsByUserId(@PathVariable String userId) {
        log.debug("Fetching sessions for user: {}", userId);
        List<ChargeSession> sessions = sessionService.getSessionsByUserId(userId);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Get all active sessions
     */
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('SCOPE_admin.all')")
    public ResponseEntity<Map<String, Object>> getActiveSessions() {
        List<ChargeSession> activeSessions = sessionService.getActiveSessions();
        Map<String, Object> response = Map.of(
                "totalActive", activeSessions.size(),
                "sessions", activeSessions,
                "timestamp", LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing session
     */
    @PutMapping("/{sessionId}")
    @PreAuthorize("hasAuthority('SCOPE_charging.start')")
    public ResponseEntity<ChargeSession> updateSession(
            @PathVariable String sessionId,
            @Valid @RequestBody ChargeSession sessionDto) {
        log.info("Updating session: {}", sessionId);

        return sessionService.updateSession(sessionId, sessionDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Delete a session
     */
    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('SCOPE_admin.all')")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        log.info("Deleting session: {}", sessionId);

        if (sessionService.deleteSession(sessionId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
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
}