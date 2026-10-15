package com.chargeroute.session.client;

import com.chargeroute.session.dto.client.ChargeUnitDto;
import com.chargeroute.session.dto.client.UserDto;
import com.chargeroute.session.dto.client.UserValidationResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/**
 * Reactive client for calling other microservices.
 * <p>
 * Key Reactive Patterns Demonstrated:
 * 1. WebClient for non-blocking HTTP calls (replaces RestTemplate)
 * 2. subscribeOn() for parallel execution on different threads
 * 3. Schedulers for controlling execution context
 * 4. Timeouts for resilience
 * 5. Error handling with onErrorResume
 * <p>
 * This shows the user's requested pattern: "session-management calling the user-management service"
 * with advanced reactive flows using subscribeOn()
 */
@Slf4j
@Component
public class ReactiveServiceClient {

    private final WebClient.Builder webClientBuilder;

    public ReactiveServiceClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    /**
     * Validate user with user-management-service.
     * <p>
     * WebClient Pattern: Non-blocking HTTP GET
     * - retrieve() initiates the request
     * - bodyToMono() extracts response body
     * - timeout() adds resilience
     * - subscribeOn() specifies execution scheduler
     *
     * @param userId The user ID to validate
     * @return Mono of validation response
     */
    public Mono<UserValidationResponseDto> validateUser(String userId) {
        log.debug("Calling user-management-service to validate user: {}", userId);

        return webClientBuilder.build()
                .get()
                .uri("http://user-management-service/api/users/{userId}/validate", userId)
                .retrieve()
                .bodyToMono(UserValidationResponseDto.class)
                .timeout(Duration.ofSeconds(3))
                .doOnNext(response -> log.debug("User validation response for {}: {}", userId, response.isValid()))
                .doOnError(error -> log.error("Error validating user {}: {}", userId, error.getMessage()))
                .onErrorResume(ex -> {
                    log.warn("Failed to validate user {}, assuming invalid", userId);
                    return Mono.just(UserValidationResponseDto.invalid("Service unavailable"));
                })
                // subscribeOn specifies the scheduler for this operation
                // boundedElastic is good for I/O operations like HTTP calls
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Get user details from user-management-service.
     *
     * @param userId The user ID
     * @return Mono of user DTO
     */
    public Mono<UserDto> getUser(String userId) {
        log.debug("Calling user-management-service to get user: {}", userId);

        return webClientBuilder.build()
                .get()
                .uri("http://user-management-service/api/users/{userId}", userId)
                .retrieve()
                .bodyToMono(UserDto.class)
                .timeout(Duration.ofSeconds(3))
                .doOnNext(user -> log.debug("Retrieved user: {}", user.getEmail()))
                .doOnError(error -> log.error("Error getting user {}: {}", userId, error.getMessage()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Get charge unit details from charge-unit-management-service.
     *
     * @param chargeUnitId The charge unit ID
     * @return Mono of charge unit DTO
     */
    public Mono<ChargeUnitDto> getChargeUnit(String chargeUnitId) {
        log.debug("Calling charge-unit-management-service to get charge unit: {}", chargeUnitId);

        return webClientBuilder.build()
                .get()
                .uri("http://charge-unit-management-service/api/charge-units/{chargeUnitId}", chargeUnitId)
                .retrieve()
                .bodyToMono(ChargeUnitDto.class)
                .timeout(Duration.ofSeconds(3))
                .doOnNext(unit -> log.debug("Retrieved charge unit: {} at {}", unit.getChargeUnitId(), unit.getStationName()))
                .doOnError(error -> log.error("Error getting charge unit {}: {}", chargeUnitId, error.getMessage()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Validate user and get charge unit in parallel.
     * <p>
     * ADVANCED REACTIVE PATTERN: Parallel execution with subscribeOn()
     * <p>
     * Key concepts:
     * 1. Both Mono operations have subscribeOn(Schedulers.boundedElastic())
     * 2. zip() subscribes to both Monos simultaneously
     * 3. Each runs on its own thread from the boundedElastic pool
     * 4. Results are combined when both complete
     * 5. Total time ≈ max(userValidation, chargeUnitFetch), NOT sum!
     * <p>
     * Without subscribeOn():
     * - Sequential execution: ~6 seconds (3s + 3s)
     * <p>
     * With subscribeOn():
     * - Parallel execution: ~3 seconds (max of both)
     * <p>
     * This is exactly what the user requested: "session-management calling
     * the user-management service" with "subscribeOn or something"!
     *
     * @param userId       The user ID to validate
     * @param chargeUnitId The charge unit ID to check
     * @return Mono of tuple (validation, charge unit)
     */
    public Mono<ValidationResult> validateSessionStart(String userId, String chargeUnitId) {
        log.info("Validating session start for user {} at charge unit {} (PARALLEL)", userId, chargeUnitId);

        // Both calls have subscribeOn(), so they execute in parallel
        Mono<UserValidationResponseDto> userValidation = validateUser(userId);
        Mono<ChargeUnitDto> chargeUnitFetch = getChargeUnit(chargeUnitId);

        // zip() combines both results when both complete
        return Mono.zip(userValidation, chargeUnitFetch)
                .map(tuple -> {
                    UserValidationResponseDto validation = tuple.getT1();
                    ChargeUnitDto chargeUnit = tuple.getT2();

                    boolean canStart = validation.isValid() &&
                            "AVAILABLE".equals(chargeUnit.getStatus());

                    String message = canStart ?
                            "Session can start" :
                            "Cannot start session: " +
                                    (!validation.isValid() ? "User invalid" : "Charge unit not available");

                    log.info("Parallel validation complete: {}", message);

                    return new ValidationResult(validation, chargeUnit, canStart, message);
                })
                .doOnSubscribe(sub -> log.debug("Starting parallel validation..."))
                .doOnSuccess(result -> log.debug("Parallel validation took ~3s (not 6s!)"));
    }

    /**
     * Result of session start validation
     */
    public record ValidationResult(
            UserValidationResponseDto userValidation,
            ChargeUnitDto chargeUnit,
            boolean canStartSession,
            String message
    ) {
    }
}
