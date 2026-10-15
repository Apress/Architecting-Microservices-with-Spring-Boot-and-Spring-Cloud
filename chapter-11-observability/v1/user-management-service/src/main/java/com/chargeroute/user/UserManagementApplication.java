package com.chargeroute.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

/**
 * Main application class for User Management Service.
 * <p>
 * R2DBC Reactive Configuration:
 * - Uses @EnableR2dbcAuditing for automatic @CreatedDate and @LastModifiedDate handling
 * - Replaces JPA's @EnableJpaAuditing with R2DBC equivalent
 * - Auditing works with Mono/Flux reactive types
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableR2dbcAuditing
public class UserManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserManagementApplication.class, args);
    }
}
