package com.chargeroute.user.config;

import com.chargeroute.user.handler.UserHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;

/**
 * Functional Router Configuration for WebFlux.
 * <p>
 * Replaces @RequestMapping annotations with functional routing.
 * <p>
 * Advantages of Functional Routing:
 * - Type-safe route definitions
 * - Centralized routing configuration (easy to see all routes)
 * - Better composition and nesting
 * - More explicit request matching
 * - Easier to test (routes are just beans)
 * <p>
 * Route Structure:
 * - nest() groups routes under a common path
 * - accept() filters by Content-Type
 * - contentType() sets response Content-Type
 * - GET/POST/PUT/DELETE map HTTP methods to handlers
 * <p>
 * Note: Security is still handled by SecurityWebFilterChain
 * (see SecurityConfig for @PreAuthorize equivalent configuration)
 */
@Configuration
public class RouterConfig {

    /**
     * Define all user management routes.
     * <p>
     * Reactive Routing Pattern:
     * - Routes are immutable and composable
     * - Handlers return Mono<ServerResponse>
     * - Filters can be added at any level
     * - Easy to version APIs with nested paths
     */
    @Bean
    public RouterFunction<ServerResponse> userRoutes(UserHandler handler) {
        return RouterFunctions.nest(path("/api/users"),
                RouterFunctions.route()
                        // POST /api/users - Create user
                        .POST("", accept(MediaType.APPLICATION_JSON), handler::createUser)

                        // GET /api/users - Get all users
                        .GET("", handler::getAllUsers)

                        // GET /api/users/{userId} - Get single user
                        .GET("/{userId}", handler::getUser)

                        // PUT /api/users/{userId} - Update user
                        .PUT("/{userId}", accept(MediaType.APPLICATION_JSON), handler::updateUser)

                        // DELETE /api/users/{userId} - Delete user
                        .DELETE("/{userId}", handler::deleteUser)

                        // GET /api/users/{userId}/validate - Validate user for session
                        .GET("/{userId}/validate", handler::validateUser)

                        // GET /api/users/{userId}/profile - Get user profile
                        .GET("/{userId}/profile", handler::getUserProfile)

                        // GET /api/users/{userId}/preferences - Get preferences
                        .GET("/{userId}/preferences", handler::getUserPreferences)

                        // PUT /api/users/{userId}/preferences - Update preferences
                        .PUT("/{userId}/preferences", accept(MediaType.APPLICATION_JSON),
                                handler::updateUserPreferences)

                        // GET /api/users/{userId}/balance - Get user balance
                        .GET("/{userId}/balance", handler::getUserBalance)

                        // GET /api/users/status/{status} - Get users by status
                        .GET("/status/{status}", handler::getUsersByStatus)

                        // GET /api/users/membership/{membershipType} - Get users by membership
                        .GET("/membership/{membershipType}", handler::getUsersByMembershipType)

                        // Admin routes
                        .GET("/admin/config", handler::getConfiguration)
                        .GET("/admin/health", handler::healthCheck)

                        .build()
        );
    }

}
