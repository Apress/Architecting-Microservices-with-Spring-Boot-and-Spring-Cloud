package com.chargeroute.user.service;

import com.chargeroute.user.dto.User;
import com.chargeroute.user.dto.UserPreferences;
import com.chargeroute.user.dto.UserRegistrationRequest;
import com.chargeroute.user.dto.UserValidationResponse;
import com.chargeroute.user.entity.UserEntity;
import com.chargeroute.user.entity.UserPreferencesEntity;
import com.chargeroute.user.event.UserCreatedEvent;
import com.chargeroute.user.event.UserRegisteredEvent;
import com.chargeroute.user.event.UserUpdatedEvent;
import com.chargeroute.user.mapper.UserMapper;
import com.chargeroute.user.observability.UserObservabilityMetrics;
import com.chargeroute.user.repository.UserPreferencesRepository;
import com.chargeroute.user.repository.UserRepository;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.annotation.NewSpan;
import io.micrometer.tracing.annotation.SpanTag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Reactive User Service using R2DBC.
 * <p>
 * Key Reactive Patterns:
 * - Returns Mono<T> for single results (replaces Optional<T>)
 * - Returns Flux<T> for multiple results (replaces List<T>)
 * - No @Transactional - R2DBC uses reactive transactions
 * - Chains operations using map, flatMap, switchIfEmpty
 * - Uses doOnNext for side effects (logging)
 * - Handles errors with onErrorMap, onErrorReturn
 */
@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final UserMapper userMapper;
    private final StreamBridge streamBridge;
    private final ApplicationEventPublisher eventPublisher;
    private final UserObservabilityMetrics observabilityMetrics;

    public UserService(
            UserRepository userRepository,
            UserPreferencesRepository preferencesRepository,
            UserMapper userMapper,
            StreamBridge streamBridge,
            ApplicationEventPublisher eventPublisher,
            UserObservabilityMetrics observabilityMetrics) {
        this.userRepository = userRepository;
        this.preferencesRepository = preferencesRepository;
        this.userMapper = userMapper;
        this.streamBridge = streamBridge;
        this.eventPublisher = eventPublisher;
        this.observabilityMetrics = observabilityMetrics;
    }

    /**
     * Create a new user from registration request.
     * <p>
     * Reactive Pattern: flatMap for dependent async operations
     * - existsByEmail returns Mono<Boolean>
     * - flatMap chains the save operation only if email doesn't exist
     * - doOnNext for side effects (logging, event publishing)
     * - map for synchronous transformation (entity to DTO)
     * <p>
     * Event-Driven Pattern: Dual event publishing
     * - Internal event (ApplicationEventPublisher) for same-service coordination
     * - External event (StreamBridge) for cross-service integration
     */
    @NewSpan("user-register")
    @Observed(name = "user.register", contextualName = "register-user")
    public Mono<User> createUser(@SpanTag("user.email") UserRegistrationRequest request) {
        log.info("Creating new user with email: {}", request.getEmail());
        String correlationId = UUID.randomUUID().toString();

        return userRepository.existsByEmail(request.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        log.warn("User with email {} already exists", request.getEmail());
                        return Mono.error(new IllegalArgumentException("User with this email already exists"));
                    }

                    // Map request to entity
                    UserEntity userEntity = userMapper.toEntity(request);
                    userEntity.setUserId(generateUserId());
                    userEntity.setStatus(UserEntity.UserStatus.PENDING_VERIFICATION);
                    userEntity.setMembershipType("BASIC");

                    // Save user
                    return userRepository.save(userEntity);
                })
                .doOnNext(savedUser -> {
                    log.info("Created user with ID: {}", savedUser.getUserId());

                    // Record user registration metric
                    observabilityMetrics.recordUserRegistered();

                    // Publish internal Spring event for same-service coordination
                    // This triggers UserPreferencesInitializer to create default preferences
                    eventPublisher.publishEvent(
                            new UserCreatedEvent(this, savedUser.getUserId(), savedUser.getEmail())
                    );
                    log.debug("Published internal UserCreatedEvent for user: {}", savedUser.getUserId());

                    // Publish external event to RabbitMQ for other services
                    // This notifies notification service, analytics, etc.
                    String fullName = buildFullName(savedUser.getFirstName(), savedUser.getLastName());
                    UserRegisteredEvent externalEvent = new UserRegisteredEvent(
                            correlationId,
                            savedUser.getUserId(),
                            savedUser.getEmail(),
                            fullName,
                            savedUser.getMembershipType()
                    );
                    streamBridge.send("userRegistered-out-0", externalEvent);
                    log.info("Published external UserRegisteredEvent to message broker for user: {}", savedUser.getUserId());
                })
                .map(userMapper::toDto);
    }

    /**
     * Get user by ID.
     * <p>
     * Reactive Pattern: Simple Mono transformation
     * - findById returns Mono<UserEntity>
     * - map transforms entity to DTO
     * - Empty Mono if user not found (replaces Optional.empty())
     */
    @NewSpan("user-get")
    @Observed(name = "user.get", contextualName = "get-user")
    public Mono<User> getUser(@SpanTag("user.id") String userId) {
        log.debug("Fetching user: {}", userId);
        return userRepository.findById(userId)
                .map(userMapper::toDto);
    }

    /**
     * Get all users.
     * <p>
     * Reactive Pattern: Flux transformation
     * - findAll returns Flux<UserEntity>
     * - map operator applies to each element
     * - No collect() needed - returns Flux directly
     */
    public Flux<User> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll()
                .map(userMapper::toDto);
    }

    /**
     * Update user information.
     * <p>
     * Reactive Pattern: flatMap for read-modify-write
     * - findById returns Mono<UserEntity>
     * - flatMap chains the save operation
     * - switchIfEmpty handles not found case
     * <p>
     * Event-Driven Pattern: Publishes UserUpdatedEvent for eventual consistency
     */
    public Mono<User> updateUser(String userId, User userDto) {
        log.info("Updating user: {}", userId);
        String correlationId = UUID.randomUUID().toString();

        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found: " + userId)))
                .flatMap(userEntity -> {
                    // Update only non-null fields
                    userMapper.updateEntityFromDto(userDto, userEntity);
                    return userRepository.save(userEntity);
                })
                .doOnNext(updated -> {
                    log.info("Updated user: {}", userId);

                    // Publish external event for other services to maintain consistency
                    String fullName = buildFullName(updated.getFirstName(), updated.getLastName());
                    UserUpdatedEvent event = new UserUpdatedEvent(
                            correlationId,
                            updated.getUserId(),
                            updated.getEmail(),
                            fullName,
                            updated.getMembershipType()
                    );
                    streamBridge.send("userUpdated-out-0", event);
                    log.info("Published UserUpdatedEvent for user: {}", userId);
                })
                .map(userMapper::toDto);
    }

    /**
     * Delete user by ID.
     * <p>
     * Reactive Pattern: flatMap with existsById check
     * - existsById returns Mono<Boolean>
     * - flatMap chains deleteById operation
     * - Returns Mono<Void> (completes when delete is done)
     */
    public Mono<Void> deleteUser(String userId) {
        log.info("Deleting user: {}", userId);

        return userRepository.existsById(userId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new IllegalArgumentException("User not found: " + userId));
                    }
                    return userRepository.deleteById(userId);
                })
                .doOnSuccess(v -> log.info("Deleted user: {}", userId));
    }

    /**
     * Validate user for charging session.
     * <p>
     * Reactive Pattern: map with default value
     * - findById returns Mono<UserEntity>
     * - map transforms entity to validation response
     * - defaultIfEmpty provides fallback for not found case
     */
    @NewSpan("user-validate")
    @Observed(name = "user.validate", contextualName = "validate-user")
    public Mono<UserValidationResponse> validateUser(@SpanTag("user.id") String userId) {
        log.info("Validating user: {}", userId);

        return userRepository.findById(userId)
                .map(user -> {
                    if (user.getStatus() != UserEntity.UserStatus.ACTIVE) {
                        log.warn("User {} is not active. Status: {}", userId, user.getStatus());
                        return UserValidationResponse.invalid("User account is " + user.getStatus().toString().toLowerCase());
                    }

                    log.info("User {} is valid with membership: {}", userId, user.getMembershipType());
                    return UserValidationResponse.valid(userId, user.getMembershipType());
                })
                .defaultIfEmpty(UserValidationResponse.invalid("User not found"))
                .doOnNext(response -> {
                    if (!response.isValid()) {
                        log.warn("User validation failed: {}", response.getMessage());
                    }
                });
    }

    /**
     * Update user's last login time.
     * <p>
     * Reactive Pattern: flatMap for update operation
     * - findById returns Mono<UserEntity>
     * - flatMap chains the save operation
     * - then() returns Mono<Void> (fire and forget)
     */
    @NewSpan("user-login")
    @Observed(name = "user.login", contextualName = "user-login")
    public Mono<Void> updateLastLogin(@SpanTag("user.id") String userId) {
        log.debug("Updating last login for user: {}", userId);

        return userRepository.findById(userId)
                .flatMap(user -> {
                    user.setLastLoginAt(LocalDateTime.now());
                    return userRepository.save(user);
                })
                .doOnNext(user -> {
                    // Record user login metric
                    observabilityMetrics.recordUserLogin();
                    log.info("User login recorded for: {}", userId);
                })
                .then();
    }

    /**
     * Get user preferences.
     * <p>
     * Reactive Pattern: Simple Mono transformation
     * - findById returns Mono<UserPreferencesEntity>
     * - map transforms entity to DTO
     */
    public Mono<UserPreferences> getUserPreferences(String userId) {
        log.debug("Fetching preferences for user: {}", userId);
        return preferencesRepository.findById(userId)
                .map(userMapper::toPreferencesDto);
    }

    /**
     * Update user preferences.
     * <p>
     * Reactive Pattern: Parallel Mono operations with zip
     * - Verifies user exists and gets/creates preferences in parallel
     * - Combines results using zipWith
     * - flatMap chains the save operation
     * <p>
     * This demonstrates advanced reactive composition!
     */
    public Mono<UserPreferences> updateUserPreferences(String userId, UserPreferences preferencesDto) {
        log.info("Updating preferences for user: {}", userId);

        // Verify user exists
        Mono<UserEntity> userMono = userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found: " + userId)));

        // Get or create preferences
        Mono<UserPreferencesEntity> preferencesMono = preferencesRepository.findById(userId)
                .switchIfEmpty(Mono.defer(() -> {
                    UserPreferencesEntity newPrefs = new UserPreferencesEntity();
                    newPrefs.setUserId(userId);
                    return Mono.just(newPrefs);
                }));

        // Combine both operations and save
        return userMono.zipWith(preferencesMono)
                .flatMap(tuple -> {
                    UserPreferencesEntity preferencesEntity = tuple.getT2();
                    userMapper.updatePreferencesEntityFromDto(preferencesDto, preferencesEntity);
                    return preferencesRepository.save(preferencesEntity);
                })
                .doOnNext(saved -> log.info("Updated preferences for user: {}", userId))
                .map(userMapper::toPreferencesDto);
    }

    /**
     * Find users by status.
     * <p>
     * Reactive Pattern: Flux transformation
     * - findByStatus returns Flux<UserEntity>
     * - map applies to each element
     */
    public Flux<User> getUsersByStatus(UserEntity.UserStatus status) {
        log.debug("Fetching users with status: {}", status);
        return userRepository.findByStatus(status)
                .map(userMapper::toDto);
    }

    /**
     * Find users by membership type.
     * <p>
     * Reactive Pattern: Flux transformation
     * - findByMembershipType returns Flux<UserEntity>
     * - map applies to each element
     */
    public Flux<User> getUsersByMembershipType(String membershipType) {
        log.debug("Fetching users with membership type: {}", membershipType);
        return userRepository.findByMembershipType(membershipType)
                .map(userMapper::toDto);
    }


    /**
     * Generate a unique user ID.
     * Synchronous helper method.
     */
    private String generateUserId() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Build full name from first and last name.
     * Handles null values gracefully.
     */
    private String buildFullName(String firstName, String lastName) {
        if (firstName == null && lastName == null) {
            return "Unknown User";
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }
}
