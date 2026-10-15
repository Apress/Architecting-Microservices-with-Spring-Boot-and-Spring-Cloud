package com.chargeroute.chargeunit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

/**
 * Main application class for Charge Unit Management Service.
 * <p>
 * R2DBC Reactive Configuration with SSE Streaming:
 * - Uses @EnableR2dbcAuditing for automatic timestamp handling
 * - Supports Server-Sent Events (SSE) for real-time charge unit status streaming
 * - Demonstrates hot Flux streams using Sinks
 * - Kubernetes-native deployment without service discovery
 */
@SpringBootApplication
@EnableR2dbcAuditing
public class ChargeUnitManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChargeUnitManagementApplication.class, args);
    }
}
