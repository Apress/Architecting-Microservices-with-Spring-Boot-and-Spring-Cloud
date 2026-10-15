package com.chargeroute.notification.service;

import com.chargeroute.notification.dto.Notification;
import com.chargeroute.notification.dto.NotificationTemplate;
import com.chargeroute.notification.entity.NotificationEntity;
import com.chargeroute.notification.entity.NotificationTemplateEntity;
import com.chargeroute.notification.repository.NotificationRepository;
import com.chargeroute.notification.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for NotificationService using Testcontainers
 * Tests the complete persistence layer with a real PostgreSQL database
 */
@SpringBootTest
@Testcontainers
@Transactional
class NotificationServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private OAuth2AuthorizedClientService authorizedClientService;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationTemplateRepository templateRepository;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        notificationRepository.deleteAll();
        templateRepository.deleteAll();
    }

    @Test
    void createNotification_ShouldPersistNotification() {
        // Given
        Notification notificationDto = Notification.builder()
                .notificationId(UUID.randomUUID().toString())
                .userId("USER-001")
                .type("SESSION_STARTED")
                .channel("EMAIL")
                .subject("Session Started")
                .message("Your charging session has started")
                .status("PENDING")
                .build();

        // When
        Notification createdNotification = notificationService.createNotification(notificationDto);

        // Then
        assertThat(createdNotification).isNotNull();
        assertThat(createdNotification.getNotificationId()).isEqualTo(notificationDto.getNotificationId());
        assertThat(createdNotification.getUserId()).isEqualTo("USER-001");
        assertThat(createdNotification.getType()).isEqualTo("SESSION_STARTED");
        assertThat(createdNotification.getChannel()).isEqualTo("EMAIL");

        // Verify it's in the database
        Optional<NotificationEntity> persistedEntity =
                notificationRepository.findById(notificationDto.getNotificationId());
        assertThat(persistedEntity).isPresent();
        assertThat(persistedEntity.get().getUserId()).isEqualTo("USER-001");
    }

    @Test
    void getById_ShouldReturnNotification_WhenExists() {
        // Given
        String notificationId = UUID.randomUUID().toString();
        NotificationEntity entity = new NotificationEntity();
        entity.setNotificationId(notificationId);
        entity.setUserId("USER-002");
        entity.setType(NotificationEntity.NotificationType.SESSION_COMPLETED);
        entity.setChannel(NotificationEntity.NotificationChannel.SMS);
        entity.setMessage("Session completed");
        entity.setStatus(NotificationEntity.NotificationStatus.SENT);
        entity.setSentAt(LocalDateTime.now());
        notificationRepository.save(entity);

        // When
        Optional<Notification> result = notificationService.getById(notificationId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getNotificationId()).isEqualTo(notificationId);
        assertThat(result.get().getUserId()).isEqualTo("USER-002");
        assertThat(result.get().getType()).isEqualTo("SESSION_COMPLETED");
    }

    @Test
    void getById_ShouldReturnEmpty_WhenNotExists() {
        // When
        Optional<Notification> result = notificationService.getById("NON-EXISTENT");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getByUserId_ShouldReturnUserNotifications() {
        // Given
        String userId = "USER-003";

        NotificationEntity notif1 = new NotificationEntity();
        notif1.setNotificationId(UUID.randomUUID().toString());
        notif1.setUserId(userId);
        notif1.setType(NotificationEntity.NotificationType.SESSION_STARTED);
        notif1.setChannel(NotificationEntity.NotificationChannel.EMAIL);
        notif1.setMessage("Session started");
        notif1.setStatus(NotificationEntity.NotificationStatus.SENT);
        notificationRepository.save(notif1);

        NotificationEntity notif2 = new NotificationEntity();
        notif2.setNotificationId(UUID.randomUUID().toString());
        notif2.setUserId(userId);
        notif2.setType(NotificationEntity.NotificationType.PAYMENT_RECEIVED);
        notif2.setChannel(NotificationEntity.NotificationChannel.PUSH);
        notif2.setMessage("Payment received");
        notif2.setStatus(NotificationEntity.NotificationStatus.PENDING);
        notificationRepository.save(notif2);

        // Notification for different user
        NotificationEntity notif3 = new NotificationEntity();
        notif3.setNotificationId(UUID.randomUUID().toString());
        notif3.setUserId("USER-999");
        notif3.setType(NotificationEntity.NotificationType.SESSION_STARTED);
        notif3.setChannel(NotificationEntity.NotificationChannel.EMAIL);
        notif3.setMessage("Message for other user");
        notif3.setStatus(NotificationEntity.NotificationStatus.SENT);
        notificationRepository.save(notif3);

        // When
        List<Notification> userNotifications = notificationService.getByUserId(userId);

        // Then
        assertThat(userNotifications).hasSize(2);
        assertThat(userNotifications).allMatch(n -> n.getUserId().equals(userId));
    }

    @Test
    void updateStatus_ShouldUpdateStatus() {
        // Given
        String notificationId = UUID.randomUUID().toString();
        NotificationEntity entity = new NotificationEntity();
        entity.setNotificationId(notificationId);
        entity.setUserId("USER-004");
        entity.setType(NotificationEntity.NotificationType.SESSION_STARTED);
        entity.setChannel(NotificationEntity.NotificationChannel.EMAIL);
        entity.setMessage("Test message");
        entity.setStatus(NotificationEntity.NotificationStatus.PENDING);
        notificationRepository.save(entity);

        // When
        Optional<Notification> updated = notificationService.updateStatus(notificationId, "SENT");

        // Then
        assertThat(updated).isPresent();
        assertThat(updated.get().getStatus()).isEqualTo("SENT");
        assertThat(updated.get().getSentAt()).isNotNull();

        // Verify in database
        NotificationEntity persistedEntity = notificationRepository.findById(notificationId).orElseThrow();
        assertThat(persistedEntity.getStatus()).isEqualTo(NotificationEntity.NotificationStatus.SENT);
        assertThat(persistedEntity.getSentAt()).isNotNull();
    }

    @Test
    void updateStatus_ShouldReturnEmpty_WhenNotExists() {
        // When
        Optional<Notification> result = notificationService.updateStatus("NON-EXISTENT", "SENT");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void deleteNotification_ShouldRemoveNotification() {
        // Given
        String notificationId = UUID.randomUUID().toString();
        NotificationEntity entity = new NotificationEntity();
        entity.setNotificationId(notificationId);
        entity.setUserId("USER-005");
        entity.setType(NotificationEntity.NotificationType.SESSION_COMPLETED);
        entity.setChannel(NotificationEntity.NotificationChannel.EMAIL);
        entity.setMessage("Test message");
        entity.setStatus(NotificationEntity.NotificationStatus.SENT);
        notificationRepository.save(entity);

        // Verify it exists
        assertThat(notificationRepository.existsById(notificationId)).isTrue();

        // When
        boolean deleted = notificationService.deleteNotification(notificationId);

        // Then
        assertThat(deleted).isTrue();
        assertThat(notificationRepository.existsById(notificationId)).isFalse();
    }

    @Test
    void deleteNotification_ShouldReturnFalse_WhenNotExists() {
        // When
        boolean deleted = notificationService.deleteNotification("NON-EXISTENT");

        // Then
        assertThat(deleted).isFalse();
    }

    @Test
    void createTemplate_ShouldPersistTemplate() {
        // Given
        NotificationTemplate templateDto = NotificationTemplate.builder()
                .name("Welcome Email")
                .type("ACCOUNT_UPDATE")
                .subject("Welcome to ChargeRoute")
                .bodyTemplate("Welcome {{userName}}!")
                .build();

        // When
        NotificationTemplate createdTemplate = notificationService.createTemplate(templateDto);

        // Then
        assertThat(createdTemplate).isNotNull();
        assertThat(createdTemplate.getTemplateId()).isNotNull();
        assertThat(createdTemplate.getName()).isEqualTo("Welcome Email");

        // Verify in database
        Optional<NotificationTemplateEntity> persistedEntity =
                templateRepository.findById(Long.parseLong(createdTemplate.getTemplateId()));
        assertThat(persistedEntity).isPresent();
    }

    @Test
    void getActiveTemplates_ShouldReturnOnlyActiveTemplates() {
        // Given
        NotificationTemplateEntity activeTemplate = new NotificationTemplateEntity();
        activeTemplate.setTemplateName("Active Template");
        activeTemplate.setNotificationType(NotificationEntity.NotificationType.SESSION_STARTED);
        activeTemplate.setChannel(NotificationEntity.NotificationChannel.EMAIL);
        activeTemplate.setSubject("Active");
        activeTemplate.setBodyTemplate("Active body");
        activeTemplate.setActive(true);
        templateRepository.save(activeTemplate);

        NotificationTemplateEntity inactiveTemplate = new NotificationTemplateEntity();
        inactiveTemplate.setTemplateName("Inactive Template");
        inactiveTemplate.setNotificationType(NotificationEntity.NotificationType.SESSION_COMPLETED);
        inactiveTemplate.setChannel(NotificationEntity.NotificationChannel.EMAIL);
        inactiveTemplate.setSubject("Inactive");
        inactiveTemplate.setBodyTemplate("Inactive body");
        inactiveTemplate.setActive(false);
        templateRepository.save(inactiveTemplate);

        // When
        List<NotificationTemplate> activeTemplates = notificationService.getActiveTemplates();

        // Then
        assertThat(activeTemplates).hasSize(1);
        assertThat(activeTemplates.get(0).getName()).isEqualTo("Active Template");
    }

    @Test
    void getAllNotifications_ShouldReturnAllNotifications() {
        // Given
        NotificationEntity notif1 = new NotificationEntity();
        notif1.setNotificationId(UUID.randomUUID().toString());
        notif1.setUserId("USER-006");
        notif1.setType(NotificationEntity.NotificationType.SESSION_STARTED);
        notif1.setChannel(NotificationEntity.NotificationChannel.EMAIL);
        notif1.setMessage("Notification 1");
        notif1.setStatus(NotificationEntity.NotificationStatus.SENT);
        notificationRepository.save(notif1);

        NotificationEntity notif2 = new NotificationEntity();
        notif2.setNotificationId(UUID.randomUUID().toString());
        notif2.setUserId("USER-007");
        notif2.setType(NotificationEntity.NotificationType.PAYMENT_RECEIVED);
        notif2.setChannel(NotificationEntity.NotificationChannel.SMS);
        notif2.setMessage("Notification 2");
        notif2.setStatus(NotificationEntity.NotificationStatus.PENDING);
        notificationRepository.save(notif2);

        // When
        List<Notification> allNotifications = notificationService.getAllNotifications();

        // Then
        assertThat(allNotifications).hasSize(2);
    }
}
