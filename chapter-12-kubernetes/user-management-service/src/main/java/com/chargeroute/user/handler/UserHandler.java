package com.chargeroute.user.handler;

import com.chargeroute.user.dto.User;
import com.chargeroute.user.dto.UserPreferences;
import com.chargeroute.user.dto.UserProfileResponse;
import com.chargeroute.user.dto.UserRegistrationRequest;
import com.chargeroute.user.entity.UserEntity;
import com.chargeroute.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Reactive Handler for User operations using WebFlux functional endpoints.
 * <p>
 * Functional Programming Model vs Annotation Model:
 * - Uses HandlerFunction instead of @RestController methods
 * - Returns Mono<ServerResponse> for all endpoints
 * - Accesses request data reactively (body, pathVariable, queryParam)
 * - More control over response building
 * - Better for advanced routing and middleware
 * <p>
 * Key Patterns:
 * - ServerRequest::bodyToMono - Parse request body reactively
 * - ServerResponse::ok/created/notFound - Build responses
 * - flatMap - Chain service calls to response building
 * - onErrorResume - Handle errors without try-catch
 */
@Slf4j
@Component
public class UserHandler {

    private final UserService userService;

    public UserHandler(UserService userService) {
        this.userService = userService;
    }

    /**
     * Create a new user.
     * <p>
     * Reactive Pattern: Parse body, call service, build response
     * - bodyToMono extracts and validates request
     * - flatMap chains to service call
     * - flatMap chains to response building
     * - onErrorResume handles validation errors
     */
    public Mono<ServerResponse> createUser(ServerRequest request) {
        return request.bodyToMono(UserRegistrationRequest.class)
                .doOnNext(req -> log.info("Creating new user with email: {}", req.getEmail()))
                .flatMap(userService::createUser)
                .flatMap(user -> ServerResponse
                        .created(URI.create("/api/users/" + user.getUserId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(user))
                .onErrorResume(IllegalArgumentException.class, e ->
                        ServerResponse.badRequest().bodyValue(Map.of("error", e.getMessage()))
                );
    }

    /**
     * Get all users.
     * <p>
     * Reactive Pattern: Flux streaming
     * - service returns Flux<User>
     * - body() streams results to client
     */
    public Mono<ServerResponse> getAllUsers(ServerRequest request) {
        log.info("Fetching all users");
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(userService.getAllUsers(), User.class);
    }

    /**
     * Get a single user by ID.
     * <p>
     * Reactive Pattern: switchIfEmpty for 404
     * - service returns Mono<User>
     * - flatMap builds 200 response
     * - switchIfEmpty builds 404 response
     */
    public Mono<ServerResponse> getUser(ServerRequest request) {
        String userId = request.pathVariable("userId");
        log.info("Getting user: {}", userId);

        return userService.getUser(userId)
                .flatMap(user -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(user))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    /**
     * Update a user.
     * <p>
     * Reactive Pattern: Combine path variable with body
     * - pathVariable extracts userId
     * - bodyToMono extracts User DTO
     * - flatMap chains to service call
     * - onErrorResume handles not found case
     */
    public Mono<ServerResponse> updateUser(ServerRequest request) {
        String userId = request.pathVariable("userId");
        log.info("Updating user: {}", userId);

        return request.bodyToMono(User.class)
                .flatMap(user -> userService.updateUser(userId, user))
                .flatMap(updated -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(updated))
                .onErrorResume(IllegalArgumentException.class, e ->
                        ServerResponse.notFound().build()
                );
    }

    /**
     * Delete a user.
     * <p>
     * Reactive Pattern: Mono<Void> service call
     * - service returns Mono<Void>
     * - then() chains to response building
     * - onErrorResume handles not found case
     */
    public Mono<ServerResponse> deleteUser(ServerRequest request) {
        String userId = request.pathVariable("userId");
        log.info("Deleting user: {}", userId);

        return userService.deleteUser(userId)
                .then(ServerResponse.noContent().build())
                .onErrorResume(IllegalArgumentException.class, e ->
                        ServerResponse.notFound().build()
                );
    }

    /**
     * Validate user for charging session.
     * <p>
     * Reactive Pattern: Simple transformation
     * - service returns Mono<UserValidationResponse>
     * - flatMap builds response with validation result
     */
    public Mono<ServerResponse> validateUser(ServerRequest request) {
        String userId = request.pathVariable("userId");
        log.info("Validating user: {}", userId);

        return userService.validateUser(userId)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    /**
     * Get user profile (aggregated view).
     * <p>
     * Reactive Pattern: Transform DTO to different response type
     * - service returns Mono<User>
     * - map transforms to UserProfileResponse
     * - flatMap builds response
     */
    public Mono<ServerResponse> getUserProfile(ServerRequest request) {
        String userId = request.pathVariable("userId");
        log.info("Getting user profile for ID: {}", userId);

        return userService.getUser(userId)
                .map(user -> UserProfileResponse.builder()
                        .id(userId)
                        .email(user.getEmail())
                        .displayName(user.getFirstName() + " " + user.getLastName())
                        .membershipLevel(user.getMembershipType())
                        .accountBalance(new BigDecimal("250.00"))
                        .totalSessions(42)
                        .totalKwhConsumed(1234.5)
                        .memberSince(user.getCreatedAt())
                        .build())
                .flatMap(profile -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(profile))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    /**
     * Get user preferences.
     */
    public Mono<ServerResponse> getUserPreferences(ServerRequest request) {
        String userId = request.pathVariable("userId");
        log.info("Getting preferences for user: {}", userId);

        return userService.getUserPreferences(userId)
                .flatMap(prefs -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(prefs))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    /**
     * Update user preferences.
     */
    public Mono<ServerResponse> updateUserPreferences(ServerRequest request) {
        String userId = request.pathVariable("userId");
        log.info("Updating preferences for user: {}", userId);

        return request.bodyToMono(UserPreferences.class)
                .flatMap(prefs -> userService.updateUserPreferences(userId, prefs))
                .flatMap(updated -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(updated))
                .onErrorResume(IllegalArgumentException.class, e ->
                        ServerResponse.notFound().build()
                );
    }

    /**
     * Get users by status.
     * <p>
     * Reactive Pattern: Parse enum from path variable
     * - pathVariable extracts status string
     * - try-catch wraps enum parsing
     * - Returns Flux<User> streamed to client
     */
    public Mono<ServerResponse> getUsersByStatus(ServerRequest request) {
        String status = request.pathVariable("status");
        log.info("Getting users with status: {}", status);

        try {
            UserEntity.UserStatus userStatus = UserEntity.UserStatus.valueOf(status.toUpperCase());
            return ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(userService.getUsersByStatus(userStatus), User.class);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status: {}", status);
            return ServerResponse.badRequest()
                    .bodyValue(Map.of("error", "Invalid status: " + status));
        }
    }

    /**
     * Get users by membership type.
     */
    public Mono<ServerResponse> getUsersByMembershipType(ServerRequest request) {
        String membershipType = request.pathVariable("membershipType");
        log.info("Getting users with membership type: {}", membershipType);

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(userService.getUsersByMembershipType(membershipType), User.class);
    }

    /**
     * Get user balance (stub).
     * <p>
     * Reactive Pattern: Verify existence before returning data
     * - hasElement() checks if Mono contains a value
     * - flatMap builds response based on existence
     */
    public Mono<ServerResponse> getUserBalance(ServerRequest request) {
        String userId = request.pathVariable("userId");
        log.info("Getting balance for user: {}", userId);

        return userService.getUser(userId)
                .hasElement()
                .flatMap(exists -> {
                    if (!exists) {
                        return ServerResponse.notFound().build();
                    }

                    Map<String, Object> balance = Map.of(
                            "userId", userId,
                            "balance", new BigDecimal("250.00"),
                            "currency", "USD",
                            "lastUpdated", LocalDateTime.now()
                    );
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(balance);
                });
    }


    /**
     * Health check endpoint.
     */
    public Mono<ServerResponse> healthCheck(ServerRequest request) {
        Map<String, String> health = Map.of(
                "service", "User Management Service",
                "status", "UP",
                "timestamp", LocalDateTime.now().toString()
        );
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(health);
    }
}
