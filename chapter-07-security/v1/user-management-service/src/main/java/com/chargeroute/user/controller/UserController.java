package com.chargeroute.user.controller;

import com.chargeroute.user.dto.User;
import com.chargeroute.user.dto.UserPreferences;
import com.chargeroute.user.dto.UserProfileResponse;
import com.chargeroute.user.dto.UserValidationResponse;
import com.chargeroute.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('SCOPE_admin.all') or hasAuthority('SCOPE_user.profile') or hasAuthority('SCOPE_service.internal')")
    public ResponseEntity<User> getUser(@PathVariable String userId) {
        log.info("Getting user: {}", userId);

        User user = userService.getUser(userId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(user);
    }

    @GetMapping("/{userId}/validate")
    @PreAuthorize("hasAuthority('SCOPE_admin.all') or hasAuthority('SCOPE_service.internal')")
    public ResponseEntity<UserValidationResponse> validateUser(@PathVariable String userId) {
        log.info("Validating user: {}", userId);

        UserValidationResponse response = userService.validateUser(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/profile")
    @PreAuthorize("hasAuthority('SCOPE_admin.all') or hasAuthority('SCOPE_user.profile')")
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
    @PreAuthorize("hasAuthority('SCOPE_user.profile')")
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

    @GetMapping("/admin/config")
    @PreAuthorize("hasAuthority('SCOPE_admin.all')")
    public ResponseEntity<Map<String, Object>> getConfiguration() {
        log.info("Getting current configuration");
        return ResponseEntity.ok(userService.getConfiguration());
    }

    @GetMapping("/admin/health")
    @PreAuthorize("hasAuthority('SCOPE_admin.all')")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "service", "User Management Service",
                "status", "UP",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}