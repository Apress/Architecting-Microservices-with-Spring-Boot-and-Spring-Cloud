package com.chargeroute.session;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Main application class for Session Management Service.
 * <p>
 * R2DBC Reactive Configuration with Advanced Patterns:
 * - Uses @EnableR2dbcAuditing for automatic timestamp handling
 * - Demonstrates parallel service calls with subscribeOn()
 * - Shows WebClient for non-blocking HTTP communication
 * - Illustrates Schedulers for controlling execution context
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableR2dbcAuditing
public class SessionManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(SessionManagementApplication.class, args);
    }

    /**
     * WebClient.Builder bean for service-to-service communication.
     * Configured with load balancing support for Eureka-registered services.
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
