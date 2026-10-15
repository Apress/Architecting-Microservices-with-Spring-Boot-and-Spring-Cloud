package com.chargeroute.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway routing configuration.
 * Defines explicit routes for all business services with load balancing.
 * <p>
 * Note: Routes are defined programmatically here rather than in YAML because
 * Spring Cloud Gateway in reactive mode doesn't reliably load route definitions
 * from Config Server at bootstrap time due to reactive context initialization timing.
 */
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // User Management Service
                .route("user-management-service", r -> r
                        .path("/user-management-service/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://USER-MANAGEMENT-SERVICE"))

                // Charge Unit Management Service
                .route("charge-unit-management-service", r -> r
                        .path("/charge-unit-management-service/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://CHARGE-UNIT-MANAGEMENT-SERVICE"))

                // Session Management Service
                .route("session-management-service", r -> r
                        .path("/session-management-service/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://SESSION-MANAGEMENT-SERVICE"))

                // Notification Management Service
                .route("notification-management-service", r -> r
                        .path("/notification-management-service/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://NOTIFICATION-MANAGEMENT-SERVICE"))

                .build();
    }
}
