package com.chargeroute.user.service;

import com.chargeroute.user.config.UserProperties;
import com.chargeroute.user.dto.UserValidationResponse;
import com.chargeroute.user.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class UserService {

    private final UserProperties userProperties;
    private final Map<String, User> users = new ConcurrentHashMap<>();

    public UserService(UserProperties userProperties) {
        this.userProperties = userProperties;
        // Initialize with sample users
        initializeSampleUsers();
    }

    private void initializeSampleUsers() {
        users.put("user123", new User(
            "user123",
            "john.doe@example.com",
            "John",
            "Doe",
            "+1-555-0123",
            User.UserStatus.ACTIVE,
            "PREMIUM",
            LocalDateTime.now().minusDays(30),
            LocalDateTime.now().minusHours(2)
        ));

        users.put("user456", new User(
            "user456",
            "jane.smith@example.com",
            "Jane",
            "Smith",
            "+1-555-0456",
            User.UserStatus.ACTIVE,
            "BASIC",
            LocalDateTime.now().minusDays(15),
            LocalDateTime.now().minusHours(6)
        ));

        users.put("user789", new User(
            "user789",
            "bob.wilson@example.com",
            "Bob",
            "Wilson",
            "+1-555-0789",
            User.UserStatus.SUSPENDED,
            "BASIC",
            LocalDateTime.now().minusDays(60),
            LocalDateTime.now().minusDays(10)
        ));

        log.info("Initialized {} sample users", users.size());
    }

    public UserValidationResponse validateUser(String userId) {
        log.info("Validating user: {}", userId);

        User user = users.get(userId);
        if (user == null) {
            log.warn("User not found: {}", userId);
            return UserValidationResponse.invalid("User not found");
        }

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            log.warn("User {} is not active. Status: {}", userId, user.getStatus());
            return UserValidationResponse.invalid("User account is " + user.getStatus().toString().toLowerCase());
        }

        log.info("User {} is valid with membership: {}", userId, user.getMembershipType());
        return UserValidationResponse.valid(userId, user.getMembershipType());
    }

    public User getUser(String userId) {
        return users.get(userId);
    }

    public Map<String, Object> getConfiguration() {
        return Map.of(
            "sessionTimeoutMinutes", userProperties.getSessionTimeoutMinutes(),
            "maxLoginAttempts", userProperties.getMaxLoginAttempts(),
            "accountLockoutMinutes", userProperties.getAccountLockoutMinutes(),
            "membership", userProperties.getMembership(),
            "notification", userProperties.getNotification()
        );
    }
}