package com.chargeroute.user.controller;

import com.chargeroute.user.dto.User;
import com.chargeroute.user.dto.UserPreferences;
import com.chargeroute.user.dto.UserProfileResponse;
import com.chargeroute.user.dto.UserRegistrationRequest;
import com.chargeroute.user.dto.UserValidationResponse;
import com.chargeroute.user.service.UserService;
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

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Create a new user (Register)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_user.write') or hasAuthority('SCOPE_service.internal')")
    public ResponseEntity<User> createUser(@Valid @RequestBody UserRegistrationRequest request) {
        log.info("Creating new user with email: {}", request.getEmail());

        User createdUser = userService.createUser(request);
        URI location = URI.create("/api/users/" + createdUser.getUserId());

        return ResponseEntity.created(location).body(createdUser);
    }

    /**
     * Get all users
     */
    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_admin.all') or hasAuthority('SCOPE_service.internal')")
    public ResponseEntity<List<User>> getAllUsers() {
        log.info("Fetching all users");

        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Get a specific user by ID
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('SCOPE_admin.all') or hasAuthority('SCOPE_user.profile') or hasAuthority('SCOPE_service.internal')")
    public ResponseEntity<User> getUser(@PathVariable String userId) {
        log.info("Getting user: {}", userId);

        return userService.getUser(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update a user
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('SCOPE_user.profile') or hasAuthority('SCOPE_service.internal')")
    public ResponseEntity<User> updateUser(
            @PathVariable String userId,
            @RequestBody User user) {
        log.info("Updating user: {}", userId);

        try {
            User updatedUser = userService.updateUser(userId, user);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            log.error("User not found: {}", userId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a user
     */
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('SCOPE_admin.all') or hasAuthority('SCOPE_service.internal')")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        log.info("Deleting user: {}", userId);

        try {
            userService.deleteUser(userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("User not found: {}", userId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Validate user for charging session
     */
    @GetMapping("/{userId}/validate")
    @PreAuthorize("hasAuthority('SCOPE_admin.all') or hasAuthority('SCOPE_service.internal')")
    public ResponseEntity<UserValidationResponse> validateUser(@PathVariable String userId) {
        log.info("Validating user: {}", userId);

        UserValidationResponse response = userService.validateUser(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user profile (aggregated view)
     */
    @GetMapping("/{userId}/profile")
    @PreAuthorize("hasAuthority('SCOPE_admin.all') or hasAuthority('SCOPE_user.profile')")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable String userId) {
        log.info("Getting user profile for ID: {}", userId);

        return userService.getUser(userId)
                .map(user -> {
                    UserProfileResponse profile = UserProfileResponse.builder()
                            .id(userId)
                            .email(user.getEmail())
                            .displayName(user.getFirstName() + " " + user.getLastName())
                            .membershipLevel(user.getMembershipType())
                            .accountBalance(new BigDecimal("250.00"))
                            .totalSessions(42)
                            .totalKwhConsumed(1234.5)
                            .memberSince(user.getCreatedAt())
                            .build();
                    return ResponseEntity.ok(profile);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get user preferences
     */
    @GetMapping("/{userId}/preferences")
    @PreAuthorize("hasAuthority('SCOPE_admin.all') or hasAuthority('SCOPE_user.profile')")
    public ResponseEntity<UserPreferences> getUserPreferences(@PathVariable String userId) {
        log.info("Getting preferences for user: {}", userId);

        return userService.getUserPreferences(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update user preferences
     */
    @PutMapping("/{userId}/preferences")
    @PreAuthorize("hasAuthority('SCOPE_user.profile')")
    public ResponseEntity<UserPreferences> updateUserPreferences(
            @PathVariable String userId,
            @RequestBody UserPreferences preferences) {
        log.info("Updating preferences for user: {}", userId);

        try {
            UserPreferences updatedPreferences = userService.updateUserPreferences(userId, preferences);
            return ResponseEntity.ok(updatedPreferences);
        } catch (IllegalArgumentException e) {
            log.error("User not found: {}", userId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get users by status
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('SCOPE_admin.all') or hasAuthority('SCOPE_service.internal')")
    public ResponseEntity<List<User>> getUsersByStatus(@PathVariable String status) {
        log.info("Getting users with status: {}", status);

        try {
            List<User> users = userService.getUsersByStatus(
                    com.chargeroute.user.entity.UserEntity.UserStatus.valueOf(status.toUpperCase())
            );
            return ResponseEntity.ok(users);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status: {}", status);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get users by membership type
     */
    @GetMapping("/membership/{membershipType}")
    @PreAuthorize("hasAuthority('SCOPE_admin.all') or hasAuthority('SCOPE_service.internal')")
    public ResponseEntity<List<User>> getUsersByMembershipType(@PathVariable String membershipType) {
        log.info("Getting users with membership type: {}", membershipType);

        List<User> users = userService.getUsersByMembershipType(membershipType);
        return ResponseEntity.ok(users);
    }

    /**
     * Get user balance (stub for now)
     */
    @GetMapping("/{userId}/balance")
    @PreAuthorize("hasAuthority('SCOPE_admin.all') or hasAuthority('SCOPE_user.profile')")
    public ResponseEntity<Map<String, Object>> getUserBalance(@PathVariable String userId) {
        log.info("Getting balance for user: {}", userId);

        // Verify user exists
        if (userService.getUser(userId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> balance = Map.of(
                "userId", userId,
                "balance", new BigDecimal("250.00"),
                "currency", "USD",
                "lastUpdated", LocalDateTime.now()
        );
        return ResponseEntity.ok(balance);
    }

    /**
     * Get service configuration
     */
    @GetMapping("/admin/config")
    @PreAuthorize("hasAuthority('SCOPE_admin.all')")
    public ResponseEntity<Map<String, Object>> getConfiguration() {
        log.info("Getting current configuration");
        return ResponseEntity.ok(userService.getConfiguration());
    }

    /**
     * Health check endpoint
     */
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
