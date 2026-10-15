package com.chargeroute.user.service;

import com.chargeroute.user.config.UserProperties;
import com.chargeroute.user.dto.User;
import com.chargeroute.user.dto.UserPreferences;
import com.chargeroute.user.dto.UserRegistrationRequest;
import com.chargeroute.user.dto.UserValidationResponse;
import com.chargeroute.user.entity.UserEntity;
import com.chargeroute.user.entity.UserPreferencesEntity;
import com.chargeroute.user.mapper.UserMapper;
import com.chargeroute.user.repository.UserPreferencesRepository;
import com.chargeroute.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final UserMapper userMapper;
    private final UserProperties userProperties;

    public UserService(
            UserRepository userRepository,
            UserPreferencesRepository preferencesRepository,
            UserMapper userMapper,
            UserProperties userProperties) {
        this.userRepository = userRepository;
        this.preferencesRepository = preferencesRepository;
        this.userMapper = userMapper;
        this.userProperties = userProperties;
    }

    /**
     * Create a new user from registration request
     */
    @Transactional
    public User createUser(UserRegistrationRequest request) {
        log.info("Creating new user with email: {}", request.getEmail());

        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("User with email {} already exists", request.getEmail());
            throw new IllegalArgumentException("User with this email already exists");
        }

        // Map request to entity
        UserEntity userEntity = userMapper.toEntity(request);
        userEntity.setUserId(generateUserId());
        userEntity.setStatus(UserEntity.UserStatus.PENDING_VERIFICATION);
        userEntity.setMembershipType("BASIC");

        // Save user
        UserEntity savedUser = userRepository.save(userEntity);
        log.info("Created user with ID: {}", savedUser.getUserId());

        return userMapper.toDto(savedUser);
    }

    /**
     * Get user by ID
     */
    public Optional<User> getUser(String userId) {
        log.debug("Fetching user: {}", userId);
        return userRepository.findById(userId)
                .map(userMapper::toDto);
    }

    /**
     * Get all users
     */
    public List<User> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Update user information
     */
    @Transactional
    public User updateUser(String userId, User userDto) {
        log.info("Updating user: {}", userId);

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Update only non-null fields
        userMapper.updateEntityFromDto(userDto, userEntity);

        UserEntity updatedUser = userRepository.save(userEntity);
        log.info("Updated user: {}", userId);

        return userMapper.toDto(updatedUser);
    }

    /**
     * Delete user by ID
     */
    @Transactional
    public void deleteUser(String userId) {
        log.info("Deleting user: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        userRepository.deleteById(userId);
        log.info("Deleted user: {}", userId);
    }

    /**
     * Validate user for charging session
     */
    public UserValidationResponse validateUser(String userId) {
        log.info("Validating user: {}", userId);

        Optional<UserEntity> userEntity = userRepository.findById(userId);
        if (userEntity.isEmpty()) {
            log.warn("User not found: {}", userId);
            return UserValidationResponse.invalid("User not found");
        }

        UserEntity user = userEntity.get();
        if (user.getStatus() != UserEntity.UserStatus.ACTIVE) {
            log.warn("User {} is not active. Status: {}", userId, user.getStatus());
            return UserValidationResponse.invalid("User account is " + user.getStatus().toString().toLowerCase());
        }

        log.info("User {} is valid with membership: {}", userId, user.getMembershipType());
        return UserValidationResponse.valid(userId, user.getMembershipType());
    }

    /**
     * Update user's last login time
     */
    @Transactional
    public void updateLastLogin(String userId) {
        log.debug("Updating last login for user: {}", userId);

        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    /**
     * Get user preferences
     */
    public Optional<UserPreferences> getUserPreferences(String userId) {
        log.debug("Fetching preferences for user: {}", userId);
        return preferencesRepository.findById(userId)
                .map(userMapper::toPreferencesDto);
    }

    /**
     * Update user preferences
     */
    @Transactional
    public UserPreferences updateUserPreferences(String userId, UserPreferences preferencesDto) {
        log.info("Updating preferences for user: {}", userId);

        // Verify user exists
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Get or create preferences
        UserPreferencesEntity preferencesEntity = preferencesRepository.findById(userId)
                .orElseGet(() -> {
                    UserPreferencesEntity newPrefs = new UserPreferencesEntity();
                    newPrefs.setUserId(userId);
                    newPrefs.setUser(user);
                    return newPrefs;
                });

        // Update preferences
        userMapper.updatePreferencesEntityFromDto(preferencesDto, preferencesEntity);

        UserPreferencesEntity savedPreferences = preferencesRepository.save(preferencesEntity);
        log.info("Updated preferences for user: {}", userId);

        return userMapper.toPreferencesDto(savedPreferences);
    }

    /**
     * Find users by status
     */
    public List<User> getUsersByStatus(UserEntity.UserStatus status) {
        log.debug("Fetching users with status: {}", status);
        return userRepository.findByStatus(status).stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Find users by membership type
     */
    public List<User> getUsersByMembershipType(String membershipType) {
        log.debug("Fetching users with membership type: {}", membershipType);
        return userRepository.findByMembershipType(membershipType).stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get configuration properties
     */
    public Map<String, Object> getConfiguration() {
        return Map.of(
                "sessionTimeoutMinutes", userProperties.getSessionTimeoutMinutes(),
                "maxLoginAttempts", userProperties.getMaxLoginAttempts(),
                "accountLockoutMinutes", userProperties.getAccountLockoutMinutes(),
                "membership", userProperties.getMembership(),
                "notification", userProperties.getNotification(),
                "validation", userProperties.getValidation()
        );
    }

    /**
     * Generate a unique user ID
     */
    private String generateUserId() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
