package com.chargeroute.user;

import com.chargeroute.user.dto.User;
import com.chargeroute.user.dto.UserRegistrationRequest;
import com.chargeroute.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebTestClient integration tests for User Management Service.
 * <p>
 * Demonstrates WebTestClient testing patterns for WebFlux functional endpoints:
 * - .exchange() pattern with chained status and body assertions
 * - .returnResult() for inspecting Flux elements with StepVerifier
 * - Full HTTP request/response cycle testing against a running server
 * <p>
 * Uses @SpringBootTest with RANDOM_PORT to start the full reactive server,
 * and @AutoConfigureWebTestClient to inject a pre-configured WebTestClient.
 * Testcontainers provides an ephemeral PostgreSQL instance for R2DBC.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class UserControllerWebTestClientTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("userdb_test")
            .withUsername("test")
            .withPassword("test");

    /**
     * Expose the Testcontainer as a Spring bean so TestSecurityConfig
     * can inject it for creating the Flyway JDBC DataSource.
     */
    @TestConfiguration
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

        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll().block();
    }

    // ──────────────────────────────────────────────
    // POST /api/users
    // ──────────────────────────────────────────────

    @Test
    void createUser_ReturnsCreatedStatusAndUserBody() {
        UserRegistrationRequest request = buildRegistrationRequest(
                "create@example.com", "Alice", "Johnson");

        webTestClient.post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(User.class)
                .value(user -> {
                    assertThat(user.getUserId()).isNotNull();
                    assertThat(user.getEmail()).isEqualTo("create@example.com");
                    assertThat(user.getFirstName()).isEqualTo("Alice");
                    assertThat(user.getLastName()).isEqualTo("Johnson");
                    assertThat(user.getStatus()).isEqualTo(User.UserStatus.PENDING_VERIFICATION);
                    assertThat(user.getMembershipType()).isEqualTo("BASIC");
                });
    }

    @Test
    void createUser_DuplicateEmail_ReturnsBadRequest() {
        UserRegistrationRequest request = buildRegistrationRequest(
                "dup@example.com", "Dup", "User");

        // Create the first user
        webTestClient.post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();

        // Attempt to create a second user with the same email
        webTestClient.post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ──────────────────────────────────────────────
    // GET /api/users/{userId}
    // ──────────────────────────────────────────────

    @Test
    void getUser_ExistingId_ReturnsUser() {
        User created = createUserViaApi("get@example.com", "Bob", "Smith");

        webTestClient.get()
                .uri("/api/users/{userId}", created.getUserId())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(User.class)
                .value(user -> {
                    assertThat(user.getUserId()).isEqualTo(created.getUserId());
                    assertThat(user.getEmail()).isEqualTo("get@example.com");
                    assertThat(user.getFirstName()).isEqualTo("Bob");
                });
    }

    @Test
    void getUser_NonExistentId_ReturnsNotFound() {
        webTestClient.get()
                .uri("/api/users/{userId}", "non-existent-id")
                .exchange()
                .expectStatus().isNotFound();
    }

    // ──────────────────────────────────────────────
    // GET /api/users
    // ──────────────────────────────────────────────

    @Test
    void getAllUsers_ReturnsUserList() {
        createUserViaApi("user1@example.com", "User", "One");
        createUserViaApi("user2@example.com", "User", "Two");
        createUserViaApi("user3@example.com", "User", "Three");

        webTestClient.get()
                .uri("/api/users")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(User.class)
                .hasSize(3)
                .value(users -> {
                    List<String> emails = users.stream()
                            .map(User::getEmail)
                            .toList();
                    assertThat(emails).containsExactlyInAnyOrder(
                            "user1@example.com",
                            "user2@example.com",
                            "user3@example.com");
                });
    }

    /**
     * Demonstrates returnResult() for inspecting Flux elements with StepVerifier.
     * <p>
     * Unlike expectBodyList(), returnResult() gives access to the raw
     * Flux<User>, enabling fine-grained element-by-element assertions
     * using StepVerifier -- the standard tool for testing reactive streams.
     */
    @Test
    void getAllUsers_ReturnResult_InspectFluxElements() {
        createUserViaApi("flux1@example.com", "Flux", "One");
        createUserViaApi("flux2@example.com", "Flux", "Two");

        FluxExchangeResult<User> result = webTestClient.get()
                .uri("/api/users")
                .exchange()
                .expectStatus().isOk()
                .returnResult(User.class);

        StepVerifier.create(result.getResponseBody())
                .expectNextMatches(user -> user.getEmail().equals("flux1@example.com")
                        || user.getEmail().equals("flux2@example.com"))
                .expectNextMatches(user -> user.getEmail().equals("flux1@example.com")
                        || user.getEmail().equals("flux2@example.com"))
                .verifyComplete();
    }

    // ──────────────────────────────────────────────
    // PUT /api/users/{userId}
    // ──────────────────────────────────────────────

    @Test
    void updateUser_ExistingId_ReturnsUpdatedUser() {
        User created = createUserViaApi("update@example.com", "Old", "Name");

        User updateDto = User.builder()
                .firstName("New")
                .lastName("UpdatedName")
                .build();

        webTestClient.put()
                .uri("/api/users/{userId}", created.getUserId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(User.class)
                .value(user -> {
                    assertThat(user.getFirstName()).isEqualTo("New");
                    assertThat(user.getLastName()).isEqualTo("UpdatedName");
                    assertThat(user.getEmail()).isEqualTo("update@example.com");
                });
    }

    // ──────────────────────────────────────────────
    // DELETE /api/users/{userId}
    // ──────────────────────────────────────────────

    @Test
    void deleteUser_ExistingId_ReturnsNoContent() {
        User created = createUserViaApi("delete@example.com", "Delete", "Me");

        webTestClient.delete()
                .uri("/api/users/{userId}", created.getUserId())
                .exchange()
                .expectStatus().isNoContent();

        // Verify the user is gone
        webTestClient.get()
                .uri("/api/users/{userId}", created.getUserId())
                .exchange()
                .expectStatus().isNotFound();
    }

    // ──────────────────────────────────────────────
    // Helper methods
    // ──────────────────────────────────────────────

    /**
     * Creates a user through the API and returns the response body.
     */
    private User createUserViaApi(String email, String firstName, String lastName) {
        UserRegistrationRequest request = buildRegistrationRequest(email, firstName, lastName);

        return webTestClient.post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(User.class)
                .returnResult()
                .getResponseBody();
    }

    private UserRegistrationRequest buildRegistrationRequest(
            String email, String firstName, String lastName) {
        return UserRegistrationRequest.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber("+1234567890")
                .password("password123")
                .build();
    }
}
