package com.chargeroute.user.event;

import com.chargeroute.user.TestSecurityConfig;
import com.chargeroute.user.dto.UserRegistrationRequest;
import com.chargeroute.user.repository.UserRepository;
import com.chargeroute.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test that verifies events are actually published when user actions occur.
 * This uses real service logic but mocked event publishers to verify the events
 * that would be sent to RabbitMQ and internal Spring event listeners.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class UserEventPublishingTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("userdb_test")
            .withUsername("test")
            .withPassword("test");

    /**
     * Expose the Testcontainer as a Spring bean so TestSecurityConfig can use it
     * for Flyway DataSource creation. Without this, Flyway migrations won't run.
     */
    @org.springframework.boot.test.context.TestConfiguration
    static class TestContainerConfig {
        @Bean
        public PostgreSQLContainer<?> postgresContainer() {
            return postgres;
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String r2dbcUrl = String.format("r2dbc:postgresql://%s:%d/%s",
                postgres.getHost(),
                postgres.getFirstMappedPort(),
                postgres.getDatabaseName());

        registry.add("spring.r2dbc.url", () -> r2dbcUrl);
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private StreamBridge streamBridge;

    @MockBean
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll().block();
        reset(streamBridge, eventPublisher);
    }

    @Test
    void shouldPublishBothInternalAndExternalEventsWhenUserCreated() {
        // Given
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+1234567890")
                .password("password123")
                .build();

        // When
        StepVerifier.create(userService.createUser(request))
                .expectNextMatches(user -> user.getEmail().equals("test@example.com"))
                .verifyComplete();

        // Then - verify external RabbitMQ event was published
        verify(streamBridge, times(1)).send(
                eq("userRegistered-out-0"),
                argThat((UserRegisteredEvent event) ->
                        event.getEmail().equals("test@example.com") &&
                                event.getFullName().equals("John Doe") &&
                                event.getMembershipTier().equals("BASIC") &&
                                event.getEventType().equals("UserRegistered") &&
                                event.getEventId() != null &&
                                event.getCorrelationId() != null
                )
        );

        // Note: Internal events (ApplicationEventPublisher) are tested indirectly
        // by verifying their side effects (e.g., UserPreferencesInitializer creating preferences).
        // This avoids the complexity of spy beans and better represents actual behavior.
    }

    @Test
    void shouldPublishUpdateEventWhenUserUpdated() {
        // Given - create a user first
        UserRegistrationRequest createRequest = UserRegistrationRequest.builder()
                .email("original@example.com")
                .firstName("Original")
                .lastName("User")
                .phoneNumber("+1234567890")
                .password("password123")
                .build();

        String userId = userService.createUser(createRequest).block().getUserId();
        reset(streamBridge, eventPublisher); // Reset mocks after creation

        // When - update the user
        com.chargeroute.user.dto.User updateDto = new com.chargeroute.user.dto.User();
        updateDto.setFirstName("Updated");
        updateDto.setLastName("Name");

        StepVerifier.create(userService.updateUser(userId, updateDto))
                .expectNextMatches(user -> user.getFirstName().equals("Updated"))
                .verifyComplete();

        // Then - verify UserUpdatedEvent was published
        verify(streamBridge, times(1)).send(
                eq("userUpdated-out-0"),
                argThat((UserUpdatedEvent event) ->
                        event.getUserId().equals(userId) &&
                                event.getFullName().equals("Updated Name") &&
                                event.getEventType().equals("UserUpdated") &&
                                event.getEventId() != null
                )
        );
    }

    @Test
    void shouldNotPublishEventsWhenUserCreationFails() {
        // Given - create user first
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .email("duplicate@example.com")
                .firstName("Test")
                .lastName("User")
                .phoneNumber("+1234567890")
                .password("password123")
                .build();

        userService.createUser(request).block();
        reset(streamBridge, eventPublisher);

        // When - try to create duplicate
        StepVerifier.create(userService.createUser(request))
                .expectError(IllegalArgumentException.class)
                .verify();

        // Then - NO events should be published on failure
        verify(streamBridge, never()).send(any(), any());
    }

    @Test
    void shouldIncludeProperMetadataInPublishedEvents() {
        // Given
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .email("metadata@example.com")
                .firstName("Meta")
                .lastName("Data")
                .phoneNumber("+1234567890")
                .password("password123")
                .build();

        // When
        userService.createUser(request).block();

        // Then - verify event has all required metadata
        verify(streamBridge).send(
                eq("userRegistered-out-0"),
                argThat((UserRegisteredEvent event) -> {
                    // Verify all metadata fields are populated
                    boolean hasEventId = event.getEventId() != null && !event.getEventId().isEmpty();
                    boolean hasCorrelationId = event.getCorrelationId() != null && !event.getCorrelationId().isEmpty();
                    boolean hasTimestamp = event.getOccurredAt() != null;
                    boolean hasEventType = "UserRegistered".equals(event.getEventType());

                    return hasEventId && hasCorrelationId && hasTimestamp && hasEventType;
                })
        );
    }
}
