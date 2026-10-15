package com.chargeroute.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * ChargeRoute API Gateway Application
 * <p>
 * This gateway serves as the single entry point for all client applications
 * accessing the ChargeRoute microservices ecosystem. It provides:
 * <p>
 * - Unified routing to all backend services
 * - Service discovery integration with Eureka
 * - Cross-cutting concerns (logging, security, rate limiting)
 * - Protocol translation and request/response transformation
 * - Load balancing and resilience patterns
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}