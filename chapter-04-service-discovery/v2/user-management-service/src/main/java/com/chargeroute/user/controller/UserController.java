package com.chargeroute.user.controller;

import com.chargeroute.user.dto.User;
import com.chargeroute.user.dto.UserPreferences;
import com.chargeroute.user.dto.UserProfileResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    @GetMapping("/health-check")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "service", "User Management Service",
                "status", "UP",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@PathVariable String userId) {
        log.info("Getting user with ID: {}", userId);

        User user = User.builder()
                .id(userId)
                .email("john.doe@evcharging.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+15551234567")
                .membershipLevel("GOLD")
                .accountBalance(new BigDecimal("250.00"))
                .registeredAt(LocalDateTime.now().minusMonths(6))
                .lastActiveAt(LocalDateTime.now().minusDays(1))
                .emailVerified(true)
                .preferredLocationIds(List.of("LOC-001", "LOC-002"))
                .preferences(UserPreferences.builder()
                        .preferredConnectorType("CCS")
                        .notificationsEnabled(true)
                        .notificationChannel("ALL")
                        .autoChargeEnabled(true)
                        .maxChargeAmount(new BigDecimal("50.00"))
                        .preferredPaymentMethod("CREDIT_CARD")
                        .build())
                .build();

        return ResponseEntity.ok(user);
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable String userId) {
        log.info("Getting user profile for ID: {}", userId);

        UserProfileResponse profile = UserProfileResponse.builder()
                .id(userId)
                .email("john.doe@evcharging.com")
                .displayName("John Doe")
                .membershipLevel("GOLD")
                .accountBalance(new BigDecimal("250.00"))
                .totalSessions(42)
                .totalKwhConsumed(1234.5)
                .memberSince(LocalDateTime.now().minusMonths(6))
                .build();

        return ResponseEntity.ok(profile);
    }

    @PutMapping("/{userId}/preferences")
    public ResponseEntity<UserPreferences> updateUserPreferences(
            @PathVariable String userId,
            @RequestBody UserPreferences preferences) {
        log.info("Updating preferences for user: {}", userId);

        return ResponseEntity.ok(preferences);
    }

    @GetMapping("/{userId}/balance")
    public ResponseEntity<Map<String, Object>> getUserBalance(@PathVariable String userId) {
        log.info("Getting balance for user: {}", userId);

        Map<String, Object> balance = Map.of(
                "userId", userId,
                "balance", new BigDecimal("250.00"),
                "currency", "USD",
                "lastUpdated", LocalDateTime.now()
        );
        return ResponseEntity.ok(balance);
    }
}
