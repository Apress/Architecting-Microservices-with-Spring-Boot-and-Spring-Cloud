package com.chargeroute.notification.consumer;

import com.chargeroute.notification.entity.NotificationEntity;
import com.chargeroute.notification.event.UserRegisteredEvent;
import com.chargeroute.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test that verifies the UserEventConsumer correctly processes UserRegisteredEvent.
 * This tests the actual consumer logic without requiring a running RabbitMQ instance.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class UserEventConsumerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("notificationdb_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.placeholder-replacement", () -> "false");  // Disable placeholder replacement
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");  // Let Flyway handle schema
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private UserEventConsumer userEventConsumer;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void shouldCreateWelcomeNotificationWhenUserRegisteredEventReceived() {
        // Given
        UserRegisteredEvent event = new UserRegisteredEvent(
                UUID.randomUUID().toString(),  // eventId
                LocalDateTime.now(),            // occurredAt
                UUID.randomUUID().toString(),   // correlationId
                "UserRegistered",               // eventType
                "user-123",                     // userId
                "alice@example.com",            // email
                "Alice Johnson",                // fullName
                "BASIC"                         // membershipTier
        );

        // When
        Consumer<UserRegisteredEvent> handler = userEventConsumer.handleUserRegistered();
        handler.accept(event);

        // Then
        NotificationEntity notification = notificationRepository
                .findByUserId("user-123")
                .stream()
                .findFirst()
                .orElse(null);

        assertThat(notification).isNotNull();
        assertThat(notification.getNotificationId()).startsWith("notif-");
        assertThat(notification.getUserId()).isEqualTo("user-123");
        assertThat(notification.getSubject()).isEqualTo("Welcome to ChargeRoute!");
        assertThat(notification.getMessage()).contains("Alice Johnson");
        assertThat(notification.getMessage()).contains("BASIC membership");
        assertThat(notification.getChannel()).isEqualTo(NotificationEntity.NotificationChannel.EMAIL);
        assertThat(notification.getStatus()).isEqualTo(NotificationEntity.NotificationStatus.PENDING);
        assertThat(notification.getType()).isEqualTo(NotificationEntity.NotificationType.ACCOUNT_UPDATE);
    }

    @Test
    void shouldBeIdempotent_WhenSameEventProcessedTwice() {
        // Given
        String eventId = UUID.randomUUID().toString();
        UserRegisteredEvent event = new UserRegisteredEvent(
                eventId,                        // same eventId
                LocalDateTime.now(),
                UUID.randomUUID().toString(),
                "UserRegistered",
                "user-456",
                "bob@example.com",
                "Bob Smith",
                "PREMIUM"
        );

        // When - process event twice
        Consumer<UserRegisteredEvent> handler = userEventConsumer.handleUserRegistered();
        handler.accept(event);
        handler.accept(event);  // Process same event again

        // Then - only ONE notification should exist
        long count = notificationRepository.findByUserId("user-456").size();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldHandleMultipleUserRegistrations() {
        // Given
        UserRegisteredEvent event1 = new UserRegisteredEvent(
                UUID.randomUUID().toString(),
                LocalDateTime.now(),
                UUID.randomUUID().toString(),
                "UserRegistered",
                "user-001",
                "user1@example.com",
                "User One",
                "BASIC"
        );

        UserRegisteredEvent event2 = new UserRegisteredEvent(
                UUID.randomUUID().toString(),
                LocalDateTime.now(),
                UUID.randomUUID().toString(),
                "UserRegistered",
                "user-002",
                "user2@example.com",
                "User Two",
                "PREMIUM"
        );

        // When
        Consumer<UserRegisteredEvent> handler = userEventConsumer.handleUserRegistered();
        handler.accept(event1);
        handler.accept(event2);

        // Then
        assertThat(notificationRepository.findByUserId("user-001")).hasSize(1);
        assertThat(notificationRepository.findByUserId("user-002")).hasSize(1);
        assertThat(notificationRepository.count()).isEqualTo(2);
    }

    @Test
    void shouldGenerateNotificationIdFromEventId() {
        // Given
        String eventId = "evt-12345";
        UserRegisteredEvent event = new UserRegisteredEvent(
                eventId,
                LocalDateTime.now(),
                UUID.randomUUID().toString(),
                "UserRegistered",
                "user-789",
                "test@example.com",
                "Test User",
                "BASIC"
        );

        // When
        Consumer<UserRegisteredEvent> handler = userEventConsumer.handleUserRegistered();
        handler.accept(event);

        // Then
        NotificationEntity notification = notificationRepository
                .findByUserId("user-789")
                .stream()
                .findFirst()
                .orElse(null);

        assertThat(notification).isNotNull();
        assertThat(notification.getNotificationId()).isEqualTo("notif-evt-12345");
    }

    @Test
    void shouldIncludeMembershipTierInWelcomeMessage() {
        // Given - PREMIUM user
        UserRegisteredEvent event = new UserRegisteredEvent(
                UUID.randomUUID().toString(),
                LocalDateTime.now(),
                UUID.randomUUID().toString(),
                "UserRegistered",
                "user-premium",
                "premium@example.com",
                "Premium User",
                "PREMIUM"
        );

        // When
        Consumer<UserRegisteredEvent> handler = userEventConsumer.handleUserRegistered();
        handler.accept(event);

        // Then
        NotificationEntity notification = notificationRepository
                .findByUserId("user-premium")
                .stream()
                .findFirst()
                .orElse(null);

        assertThat(notification).isNotNull();
        assertThat(notification.getMessage()).contains("PREMIUM membership");
        assertThat(notification.getMessage()).contains("Premium User");
    }
}
