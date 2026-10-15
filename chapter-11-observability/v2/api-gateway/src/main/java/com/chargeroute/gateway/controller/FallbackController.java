package com.chargeroute.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Fallback Controller for Circuit Breaker Integration
 * <p>
 * Provides fallback responses when backend services are unavailable or slow.
 * This ensures the API Gateway can gracefully handle service failures without
 * completely breaking the client experience.
 */
@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        log.warn("User service fallback triggered - service may be unavailable");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "User service temporarily unavailable",
                        "message", "Please try again in a few moments",
                        "fallback", true,
                        "timestamp", LocalDateTime.now(),
                        "service", "user-management-service",
                        "suggestion", "Check your connection or contact support if the issue persists"
                ));
    }

    @GetMapping("/charge-units")
    public ResponseEntity<Map<String, Object>> chargeUnitServiceFallback() {
        log.warn("Charge unit service fallback triggered - service may be unavailable");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Charge unit service temporarily unavailable",
                        "message", "Unable to retrieve charging station information",
                        "fallback", true,
                        "timestamp", LocalDateTime.now(),
                        "service", "charge-unit-management-service",
                        "suggestion", "Please refresh the app or try viewing nearby stations on the map"
                ));
    }

    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> sessionServiceFallback() {
        log.warn("Session service fallback triggered - service may be unavailable");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Session service temporarily unavailable",
                        "message", "Cannot process charging session requests at this time",
                        "fallback", true,
                        "timestamp", LocalDateTime.now(),
                        "service", "session-management-service",
                        "suggestion", "If you have an active session, it will continue safely. New sessions may be delayed."
                ));
    }

    @GetMapping("/notifications")
    public ResponseEntity<Map<String, Object>> notificationServiceFallback() {
        log.warn("Notification service fallback triggered - service may be unavailable");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Notification service temporarily unavailable",
                        "message", "Notifications may be delayed",
                        "fallback", true,
                        "timestamp", LocalDateTime.now(),
                        "service", "notification-management-service",
                        "suggestion", "Check your notification preferences in settings"
                ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> gatewayHealth() {
        return ResponseEntity.ok(Map.of(
                "gateway", "UP",
                "timestamp", LocalDateTime.now(),
                "message", "ChargeRoute API Gateway is operational",
                "version", "1.0.0"
        ));
    }
}