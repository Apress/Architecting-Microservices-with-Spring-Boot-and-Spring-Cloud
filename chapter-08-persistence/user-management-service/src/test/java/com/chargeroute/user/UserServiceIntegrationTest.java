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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class UserServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("userdb_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
    }

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
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

        // When
        User createdUser = userService.createUser(request);

        // Then
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getUserId()).isNotNull();
        assertThat(createdUser.getEmail()).isEqualTo("test@example.com");
        assertThat(createdUser.getFirstName()).isEqualTo("Test");
        assertThat(createdUser.getLastName()).isEqualTo("User");
        assertThat(createdUser.getStatus()).isEqualTo(User.UserStatus.PENDING_VERIFICATION);
        assertThat(createdUser.getMembershipType()).isEqualTo("BASIC");
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

        userService.createUser(request);

        // When/Then
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User with this email already exists");
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

        User createdUser = userService.createUser(request);

        // When
        Optional<User> foundUser = userService.getUser(createdUser.getUserId());

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("get@example.com");
    }

    @Test
    void testGetUser_NotFound() {
        // When
        Optional<User> foundUser = userService.getUser("non-existent-id");

        // Then
        assertThat(foundUser).isEmpty();
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

        User createdUser = userService.createUser(request);

        User updateDto = User.builder()
                .firstName("New")
                .lastName("UpdatedName")
                .build();

        // When
        User updatedUser = userService.updateUser(createdUser.getUserId(), updateDto);

        // Then
        assertThat(updatedUser.getFirstName()).isEqualTo("New");
        assertThat(updatedUser.getLastName()).isEqualTo("UpdatedName");
        assertThat(updatedUser.getEmail()).isEqualTo("update@example.com"); // Should remain unchanged
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

        User createdUser = userService.createUser(request);

        // When
        userService.deleteUser(createdUser.getUserId());

        // Then
        Optional<User> deletedUser = userService.getUser(createdUser.getUserId());
        assertThat(deletedUser).isEmpty();
    }

    @Test
    void testGetAllUsers() {
        // Given
        userService.createUser(createTestRegistrationRequest("user1@example.com", "User", "One"));
        userService.createUser(createTestRegistrationRequest("user2@example.com", "User", "Two"));
        userService.createUser(createTestRegistrationRequest("user3@example.com", "User", "Three"));

        // When
        List<User> allUsers = userService.getAllUsers();

        // Then
        assertThat(allUsers).hasSize(3);
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

        User createdUser = userService.createUser(request);

        UserPreferences preferences = UserPreferences.builder()
                .preferredConnectorType("CCS")
                .notificationsEnabled(true)
                .notificationChannel("EMAIL")
                .autoChargeEnabled(true)
                .maxChargeAmount(new BigDecimal("100.00"))
                .preferredPaymentMethod("CREDIT_CARD")
                .build();

        // When
        UserPreferences savedPreferences = userService.updateUserPreferences(createdUser.getUserId(), preferences);

        // Then
        assertThat(savedPreferences).isNotNull();
        assertThat(savedPreferences.getPreferredConnectorType()).isEqualTo("CCS");
        assertThat(savedPreferences.getNotificationsEnabled()).isTrue();
        assertThat(savedPreferences.getMaxChargeAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void testGetUserPreferences_NotFound() {
        // When
        Optional<UserPreferences> preferences = userService.getUserPreferences("non-existent-id");

        // Then
        assertThat(preferences).isEmpty();
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
