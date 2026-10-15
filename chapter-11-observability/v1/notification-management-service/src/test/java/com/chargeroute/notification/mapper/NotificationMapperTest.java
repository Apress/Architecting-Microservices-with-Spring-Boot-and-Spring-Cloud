package com.chargeroute.notification.mapper;

import com.chargeroute.notification.dto.Notification;
import com.chargeroute.notification.entity.NotificationEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * High-level tests for NotificationMapper to verify MapStruct mapping functionality.
 * Tests entity-to-DTO and DTO-to-entity conversions for notifications.
 */
@SpringBootTest
@ActiveProfiles("test")
class NotificationMapperTest {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    private NotificationMapper notificationMapper;

    @Test
    void toDto_shouldMapEntityToDto() {
        // Given
        NotificationEntity entity = new NotificationEntity();
        entity.setNotificationId("notif-123");
        entity.setUserId("user-456");
        entity.setType(NotificationEntity.NotificationType.SESSION_COMPLETED);
        entity.setChannel(NotificationEntity.NotificationChannel.EMAIL);
        entity.setSubject("Your charging session is complete");
        entity.setMessage("Your vehicle has been fully charged. Session ID: ABC123");
        entity.setStatus(NotificationEntity.NotificationStatus.SENT);
        entity.setSentAt(LocalDateTime.now().minusMinutes(10));

        // When
        Notification dto = notificationMapper.toDto(entity);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getNotificationId()).isEqualTo("notif-123");
        assertThat(dto.getUserId()).isEqualTo("user-456");
        assertThat(dto.getType()).isEqualTo("SESSION_COMPLETED");
        assertThat(dto.getChannel()).isEqualTo("EMAIL");
        assertThat(dto.getSubject()).isEqualTo("Your charging session is complete");
        assertThat(dto.getMessage()).isEqualTo("Your vehicle has been fully charged. Session ID: ABC123");
        assertThat(dto.getStatus()).isEqualTo("SENT");
        assertThat(dto.getSentAt()).isEqualTo(entity.getSentAt());
    }

    @Test
    void toDto_shouldHandleNullEntity() {
        // When
        Notification dto = notificationMapper.toDto(null);

        // Then
        assertThat(dto).isNull();
    }

    @Test
    void toEntity_shouldMapDtoToEntity() {
        // Given
        Notification dto = new Notification();
        dto.setNotificationId("notif-789");
        dto.setUserId("user-111");
        dto.setType("PAYMENT_RECEIVED");
        dto.setChannel("SMS");
        dto.setSubject("Payment confirmed");
        dto.setMessage("Your payment of $25.00 has been received");
        dto.setStatus("PENDING");

        // When
        NotificationEntity entity = notificationMapper.toEntity(dto);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getNotificationId()).isEqualTo("notif-789");
        assertThat(entity.getUserId()).isEqualTo("user-111");
        assertThat(entity.getType()).isEqualTo(NotificationEntity.NotificationType.PAYMENT_RECEIVED);
        assertThat(entity.getChannel()).isEqualTo(NotificationEntity.NotificationChannel.SMS);
        assertThat(entity.getSubject()).isEqualTo("Payment confirmed");
        assertThat(entity.getMessage()).isEqualTo("Your payment of $25.00 has been received");
        assertThat(entity.getStatus()).isEqualTo(NotificationEntity.NotificationStatus.PENDING);
    }

    @Test
    void updateEntityFromDto_shouldUpdateOnlyNonNullFields() {
        // Given
        NotificationEntity entity = new NotificationEntity();
        entity.setNotificationId("notif-update-test");
        entity.setUserId("user-original");
        entity.setType(NotificationEntity.NotificationType.SESSION_STARTED);
        entity.setChannel(NotificationEntity.NotificationChannel.EMAIL);
        entity.setSubject("Original subject");
        entity.setMessage("Original message");
        entity.setStatus(NotificationEntity.NotificationStatus.PENDING);

        Notification dto = new Notification();
        dto.setStatus("SENT");
        dto.setSentAt(LocalDateTime.now());
        // Other fields are null

        // When
        notificationMapper.updateEntityFromDto(dto, entity);

        // Then
        assertThat(entity.getNotificationId()).isEqualTo("notif-update-test"); // Should not change
        assertThat(entity.getUserId()).isEqualTo("user-original"); // Should not change
        assertThat(entity.getType()).isEqualTo(NotificationEntity.NotificationType.SESSION_STARTED); // Should not change
        assertThat(entity.getChannel()).isEqualTo(NotificationEntity.NotificationChannel.EMAIL); // Should not change
        assertThat(entity.getSubject()).isEqualTo("Original subject"); // Should not change
        assertThat(entity.getMessage()).isEqualTo("Original message"); // Should not change
        assertThat(entity.getStatus()).isEqualTo(NotificationEntity.NotificationStatus.SENT); // Should be updated
        assertThat(entity.getSentAt()).isNotNull(); // Should be updated
    }

    @Test
    void notificationTypeMapping_shouldHandleAllTypes() {
        // Test all notification types
        NotificationEntity.NotificationType[] types = NotificationEntity.NotificationType.values();

        for (NotificationEntity.NotificationType type : types) {
            NotificationEntity entity = new NotificationEntity();
            entity.setNotificationId("test-" + type.name());
            entity.setUserId("user");
            entity.setType(type);
            entity.setChannel(NotificationEntity.NotificationChannel.EMAIL);
            entity.setSubject("Test");
            entity.setMessage("Test message");
            entity.setStatus(NotificationEntity.NotificationStatus.PENDING);

            Notification dto = notificationMapper.toDto(entity);

            assertThat(dto.getType()).isEqualTo(type.name());
        }
    }

    @Test
    void channelMapping_shouldHandleAllChannels() {
        // Test all channel values
        NotificationEntity.NotificationChannel[] channels = NotificationEntity.NotificationChannel.values();

        for (NotificationEntity.NotificationChannel channel : channels) {
            NotificationEntity entity = new NotificationEntity();
            entity.setNotificationId("test-" + channel.name());
            entity.setUserId("user");
            entity.setType(NotificationEntity.NotificationType.SESSION_STARTED);
            entity.setChannel(channel);
            entity.setSubject("Test");
            entity.setMessage("Test message");
            entity.setStatus(NotificationEntity.NotificationStatus.PENDING);

            Notification dto = notificationMapper.toDto(entity);

            assertThat(dto.getChannel()).isEqualTo(channel.name());
        }
    }

    @Test
    void statusMapping_shouldHandleAllStatuses() {
        // Test all status values
        NotificationEntity.NotificationStatus[] statuses = NotificationEntity.NotificationStatus.values();

        for (NotificationEntity.NotificationStatus status : statuses) {
            NotificationEntity entity = new NotificationEntity();
            entity.setNotificationId("test-" + status.name());
            entity.setUserId("user");
            entity.setType(NotificationEntity.NotificationType.PAYMENT_RECEIVED);
            entity.setChannel(NotificationEntity.NotificationChannel.PUSH);
            entity.setSubject("Test");
            entity.setMessage("Test message");
            entity.setStatus(status);

            Notification dto = notificationMapper.toDto(entity);

            assertThat(dto.getStatus()).isEqualTo(status.name());
        }
    }

    @Test
    void stringToEnum_shouldParseTypeCorrectly() {
        // Given
        Notification dto = new Notification();
        dto.setNotificationId("test");
        dto.setUserId("user");
        dto.setType("MAINTENANCE_ALERT");
        dto.setChannel("EMAIL");
        dto.setSubject("Test");
        dto.setMessage("Test");
        dto.setStatus("PENDING");

        // When
        NotificationEntity entity = notificationMapper.toEntity(dto);

        // Then
        assertThat(entity.getType()).isEqualTo(NotificationEntity.NotificationType.MAINTENANCE_ALERT);
    }

    @Test
    void stringToEnum_shouldParseChannelCorrectly() {
        // Given
        Notification dto = new Notification();
        dto.setNotificationId("test");
        dto.setUserId("user");
        dto.setType("SESSION_STARTED");
        dto.setChannel("PUSH");
        dto.setSubject("Test");
        dto.setMessage("Test");
        dto.setStatus("PENDING");

        // When
        NotificationEntity entity = notificationMapper.toEntity(dto);

        // Then
        assertThat(entity.getChannel()).isEqualTo(NotificationEntity.NotificationChannel.PUSH);
    }

    @Test
    void stringToEnum_shouldParseStatusCorrectly() {
        // Given
        Notification dto = new Notification();
        dto.setNotificationId("test");
        dto.setUserId("user");
        dto.setType("SESSION_COMPLETED");
        dto.setChannel("EMAIL");
        dto.setSubject("Test");
        dto.setMessage("Test");
        dto.setStatus("FAILED");

        // When
        NotificationEntity entity = notificationMapper.toEntity(dto);

        // Then
        assertThat(entity.getStatus()).isEqualTo(NotificationEntity.NotificationStatus.FAILED);
    }

    @Test
    void roundTrip_shouldPreserveData() {
        // Given
        NotificationEntity originalEntity = new NotificationEntity();
        originalEntity.setNotificationId("round-trip-456");
        originalEntity.setUserId("user-789");
        originalEntity.setType(NotificationEntity.NotificationType.PAYMENT_FAILED);
        originalEntity.setChannel(NotificationEntity.NotificationChannel.SMS);
        originalEntity.setSubject("Payment Failed");
        originalEntity.setMessage("Your payment could not be processed. Please update your payment method.");
        originalEntity.setStatus(NotificationEntity.NotificationStatus.RETRYING);
        originalEntity.setSentAt(LocalDateTime.now().minusHours(1));

        // When - Convert to DTO and back to Entity
        Notification dto = notificationMapper.toDto(originalEntity);
        NotificationEntity roundTripEntity = notificationMapper.toEntity(dto);

        // Then
        assertThat(roundTripEntity.getNotificationId()).isEqualTo(originalEntity.getNotificationId());
        assertThat(roundTripEntity.getUserId()).isEqualTo(originalEntity.getUserId());
        assertThat(roundTripEntity.getType()).isEqualTo(originalEntity.getType());
        assertThat(roundTripEntity.getChannel()).isEqualTo(originalEntity.getChannel());
        assertThat(roundTripEntity.getSubject()).isEqualTo(originalEntity.getSubject());
        assertThat(roundTripEntity.getMessage()).isEqualTo(originalEntity.getMessage());
        assertThat(roundTripEntity.getStatus()).isEqualTo(originalEntity.getStatus());
        assertThat(roundTripEntity.getSentAt()).isEqualTo(originalEntity.getSentAt());
    }

    @Test
    void longMessageMapping_shouldHandleLongText() {
        // Given
        String longMessage = "This is a very long notification message that contains detailed information about " +
                "the charging session, including the start time, end time, energy delivered, cost breakdown, " +
                "payment method used, and additional notes about the transaction. This tests the mapper's " +
                "ability to handle longer text fields without truncation or data loss.";

        NotificationEntity entity = new NotificationEntity();
        entity.setNotificationId("long-msg-test");
        entity.setUserId("user");
        entity.setType(NotificationEntity.NotificationType.SESSION_COMPLETED);
        entity.setChannel(NotificationEntity.NotificationChannel.EMAIL);
        entity.setSubject("Detailed Session Summary");
        entity.setMessage(longMessage);
        entity.setStatus(NotificationEntity.NotificationStatus.SENT);

        // When
        Notification dto = notificationMapper.toDto(entity);

        // Then
        assertThat(dto.getMessage()).isEqualTo(longMessage);
        assertThat(dto.getMessage()).hasSize(longMessage.length());
    }

    @Test
    void multipleEnumCombinations_shouldMapCorrectly() {
        // Test various combinations of type, channel, and status
        NotificationEntity entity = new NotificationEntity();
        entity.setNotificationId("combo-test");
        entity.setUserId("user-123");
        entity.setType(NotificationEntity.NotificationType.SESSION_STARTED);
        entity.setChannel(NotificationEntity.NotificationChannel.PUSH);
        entity.setSubject("Charging Started");
        entity.setMessage("Your EV is now charging");
        entity.setStatus(NotificationEntity.NotificationStatus.SENT);

        Notification dto = notificationMapper.toDto(entity);

        assertThat(dto.getType()).isEqualTo("SESSION_STARTED");
        assertThat(dto.getChannel()).isEqualTo("PUSH");
        assertThat(dto.getStatus()).isEqualTo("SENT");

        // Convert back
        NotificationEntity backToEntity = notificationMapper.toEntity(dto);

        assertThat(backToEntity.getType()).isEqualTo(NotificationEntity.NotificationType.SESSION_STARTED);
        assertThat(backToEntity.getChannel()).isEqualTo(NotificationEntity.NotificationChannel.PUSH);
        assertThat(backToEntity.getStatus()).isEqualTo(NotificationEntity.NotificationStatus.SENT);
    }
}
