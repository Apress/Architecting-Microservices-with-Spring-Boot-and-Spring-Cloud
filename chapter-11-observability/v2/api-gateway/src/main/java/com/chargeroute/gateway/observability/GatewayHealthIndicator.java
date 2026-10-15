package com.chargeroute.gateway.observability;

import com.netflix.discovery.EurekaClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Custom health indicator for API Gateway.
 * Provides detailed health information about gateway capabilities and connectivity.
 */
@Component("gatewayServiceHealth")
@Slf4j
public class GatewayHealthIndicator implements ReactiveHealthIndicator {

    private final EurekaClient eurekaClient;

    public GatewayHealthIndicator(@Autowired(required = false) EurekaClient eurekaClient) {
        this.eurekaClient = eurekaClient;
    }

    @Override
    public Mono<Health> health() {
        return checkGatewayHealth()
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(ex -> {
                    log.error("Health check failed", ex);
                    return Mono.just(Health.down()
                            .withDetail("error", ex.getMessage())
                            .withDetail("errorType", ex.getClass().getSimpleName())
                            .build());
                });
    }

    private Mono<Health> checkGatewayHealth() {
        return Mono.fromCallable(() -> {
            Health.Builder builder = Health.up()
                    .withDetail("gateway", "UP");

            // Check Eureka connectivity if available
            if (eurekaClient != null) {
                try {
                    var applications = eurekaClient.getApplications();
                    int registeredServices = applications.getRegisteredApplications().size();

                    builder.withDetail("eureka", "UP")
                            .withDetail("registeredServices", registeredServices);

                    // Add warning if no services are registered
                    if (registeredServices == 0) {
                        builder.withDetail("warning", "No services registered with Eureka");
                    }

                    // List registered service names
                    var serviceNames = applications.getRegisteredApplications().stream()
                            .map(app -> app.getName().toLowerCase())
                            .toList();
                    builder.withDetail("availableServices", serviceNames);

                } catch (Exception ex) {
                    log.error("Eureka connectivity check failed", ex);
                    builder.withDetail("eureka", "DOWN")
                            .withDetail("eurekaError", ex.getMessage());
                    // Don't fail overall health just because Eureka is down
                    // Gateway can still route to hardcoded endpoints if needed
                }
            } else {
                builder.withDetail("eureka", "NOT_CONFIGURED")
                        .withDetail("note", "EurekaClient not available (test mode or disabled)");
            }

            return builder.build();
        });
    }
}
