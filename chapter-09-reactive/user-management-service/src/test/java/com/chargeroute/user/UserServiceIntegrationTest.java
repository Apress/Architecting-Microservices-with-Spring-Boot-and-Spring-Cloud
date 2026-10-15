package com.chargeroute.user;

import com.chargeroute.user.dto.User;
import com.chargeroute.user.dto.UserPreferences;
import com.chargeroute.user.dto.UserRegistrationRequest;
import com.chargeroute.user.repository.UserRepository;
import com.chargeroute.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Import(TestSecurityConfig.class)
@org.springframework.test.context.ActiveProfiles("test")
class UserServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("userdb_test")
            .withUsername("test")
            .withPassword("test");

    /**
     * Expose the Testcontainer as a Spring bean so it can be injected
     * into TestSecurityConfig for creating the Flyway DataSource
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
        // R2DBC configuration for reactive tests
        String r2dbcUrl = String.format("r2dbc:postgresql://%s:%d/%s",
                postgres.getHost(),
                postgres.getFirstMappedPort(),
                postgres.getDatabaseName());

        registry.add("spring.r2dbc.url", () -> r2dbcUrl);
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);

        // JDBC config for Flyway migrations
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");

        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // R2DBC deleteAll() returns Mono<Void>, must block to ensure completion before tests
        userRepository.deleteAll().block();
    }

    @Test
    void testCreateUser_Success() {
        // Given
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .phoneNumber("+1234567890")
                .password("password123")
                .build();

        // When & Then - Use StepVerifier for reactive testing
        StepVerifier.create(userService.createUser(request))
                .assertNext(createdUser -> {
                    assertThat(createdUser).isNotNull();
                    assertThat(createdUser.getUserId()).isNotNull();
                    assertThat(createdUser.getEmail()).isEqualTo("test@example.com");
                    assertThat(createdUser.getFirstName()).isEqualTo("Test");
                    assertThat(createdUser.getLastName()).isEqualTo("User");
                    assertThat(createdUser.getStatus()).isEqualTo(User.UserStatus.PENDING_VERIFICATION);
                    assertThat(createdUser.getMembershipType()).isEqualTo("BASIC");
                })
                .verifyComplete();
    }

    @Test
    void testCreateUser_DuplicateEmail_ThrowsException() {
        // Given
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .email("duplicate@example.com")
                .firstName("Test")
                .lastName("User")
                .phoneNumber("+1234567890")
                .password("password123")
                .build();

        // Create first user
        userService.createUser(request).block();

        // When/Then - Expect error on duplicate
        StepVerifier.create(userService.createUser(request))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                                throwable.getMessage().contains("User with this email already exists"))
                .verify();
    }

    @Test
    void testGetUser_Found() {
        // Given
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .email("get@example.com")
                .firstName("Get")
                .lastName("User")
                .phoneNumber("+1234567890")
                .password("password123")
                .build();

        User createdUser = userService.createUser(request).block();

        // When & Then
        StepVerifier.create(userService.getUser(createdUser.getUserId()))
                .assertNext(foundUser -> {
                    assertThat(foundUser).isNotNull();
                    assertThat(foundUser.getEmail()).isEqualTo("get@example.com");
                })
                .verifyComplete();
    }

    @Test
    void testGetUser_NotFound() {
        // When & Then - Mono should complete empty
        StepVerifier.create(userService.getUser("non-existent-id"))
                .verifyComplete();
    }

    @Test
    void testUpdateUser_Success() {
        // Given
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .email("update@example.com")
                .firstName("Old")
                .lastName("Name")
                .phoneNumber("+1234567890")
                .password("password123")
                .build();

        User createdUser = userService.createUser(request).block();

        User updateDto = User.builder()
                .firstName("New")
                .lastName("UpdatedName")
                .build();

        // When & Then
        StepVerifier.create(userService.updateUser(createdUser.getUserId(), updateDto))
                .assertNext(updatedUser -> {
                    assertThat(updatedUser.getFirstName()).isEqualTo("New");
                    assertThat(updatedUser.getLastName()).isEqualTo("UpdatedName");
                    assertThat(updatedUser.getEmail()).isEqualTo("update@example.com");
                })
                .verifyComplete();
    }

    @Test
    void testDeleteUser_Success() {
        // Given
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .email("delete@example.com")
                .firstName("Delete")
                .lastName("User")
                .phoneNumber("+1234567890")
                .password("password123")
                .build();

        User createdUser = userService.createUser(request).block();

        // When - Delete the user
        userService.deleteUser(createdUser.getUserId()).block();

        // Then - Verify user is gone
        StepVerifier.create(userService.getUser(createdUser.getUserId()))
                .verifyComplete();
    }

    @Test
    void testGetAllUsers() {
        // Given
        userService.createUser(createTestRegistrationRequest("user1@example.com", "User", "One")).block();
        userService.createUser(createTestRegistrationRequest("user2@example.com", "User", "Two")).block();
        userService.createUser(createTestRegistrationRequest("user3@example.com", "User", "Three")).block();

        // When & Then - Use StepVerifier to count Flux emissions
        StepVerifier.create(userService.getAllUsers())
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    void testUpdateUserPreferences_Success() {
        // Given
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .email("prefs@example.com")
                .firstName("Prefs")
                .lastName("User")
                .phoneNumber("+1234567890")
                .password("password123")
                .build();

        User createdUser = userService.createUser(request).block();

        UserPreferences preferences = UserPreferences.builder()
                .preferredConnectorType("CCS")
                .notificationsEnabled(true)
                .notificationChannel("EMAIL")
                .autoChargeEnabled(true)
                .maxChargeAmount(new BigDecimal("100.00"))
                .preferredPaymentMethod("CREDIT_CARD")
                .build();

        // When & Then
        StepVerifier.create(userService.updateUserPreferences(createdUser.getUserId(), preferences))
                .assertNext(savedPreferences -> {
                    assertThat(savedPreferences).isNotNull();
                    assertThat(savedPreferences.getPreferredConnectorType()).isEqualTo("CCS");
                    assertThat(savedPreferences.getNotificationsEnabled()).isTrue();
                    assertThat(savedPreferences.getMaxChargeAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
                })
                .verifyComplete();
    }

    @Test
    void testGetUserPreferences_NotFound() {
        // When & Then - Mono completes empty for non-existent user
        StepVerifier.create(userService.getUserPreferences("non-existent-id"))
                .verifyComplete();
    }

    private UserRegistrationRequest createTestRegistrationRequest(String email, String firstName, String lastName) {
        return UserRegistrationRequest.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber("+1234567890")
                .password("password123")
                .build();
    }
}
