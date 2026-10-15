package com.chargeroute.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main application class for Notification Management Service.
 * <p>
 * Configuration:
 * - Uses @EnableJpaAuditing for automatic timestamp handling
 * - Event-driven notification processing with RabbitMQ
 * - Kubernetes-native deployment without service discovery
 */
@SpringBootApplication
@EnableJpaAuditing
public class NotificationManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationManagementApplication.class, args);
    }
}
