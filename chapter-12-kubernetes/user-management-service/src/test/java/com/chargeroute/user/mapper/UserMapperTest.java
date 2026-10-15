package com.chargeroute.user.mapper;

import com.chargeroute.user.dto.User;
import com.chargeroute.user.dto.UserPreferences;
import com.chargeroute.user.dto.UserRegistrationRequest;
import com.chargeroute.user.entity.UserEntity;
import com.chargeroute.user.entity.UserPreferencesEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * High-level tests for UserMapper to verify MapStruct mapping functionality.
 * Tests entity-to-DTO and DTO-to-entity conversions.
 */
@SpringBootTest
@ActiveProfiles("test")
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    void toDto_shouldMapEntityToDto() {
        // Given
        UserEntity entity = new UserEntity();
        entity.setUserId("user-123");
        entity.setEmail("test@example.com");
        entity.setFirstName("John");
        entity.setLastName("Doe");
        entity.setPhoneNumber("+1234567890");
        entity.setStatus(UserEntity.UserStatus.ACTIVE);
        entity.setMembershipType("GOLD");
        entity.setCreatedAt(LocalDateTime.now().minusDays(10));
        entity.setLastLoginAt(LocalDateTime.now().minusHours(2));

        // When
        User dto = userMapper.toDto(entity);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getUserId()).isEqualTo("user-123");
        assertThat(dto.getEmail()).isEqualTo("test@example.com");
        assertThat(dto.getFirstName()).isEqualTo("John");
        assertThat(dto.getLastName()).isEqualTo("Doe");
        assertThat(dto.getPhoneNumber()).isEqualTo("+1234567890");
        assertThat(dto.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
        assertThat(dto.getMembershipType()).isEqualTo("GOLD");
        assertThat(dto.getCreatedAt()).isEqualTo(entity.getCreatedAt());
        assertThat(dto.getLastLoginAt()).isEqualTo(entity.getLastLoginAt());
    }

    @Test
    void toDto_shouldHandleNullEntity() {
        // When
        User dto = userMapper.toDto(null);

        // Then
        assertThat(dto).isNull();
    }

    @Test
    void toEntity_shouldMapDtoToEntity() {
        // Given
        User dto = User.builder()
                .userId("user-456")
                .email("jane@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .phoneNumber("+0987654321")
                .status(User.UserStatus.PENDING_VERIFICATION)
                .membershipType("SILVER")
                .build();

        // When
        UserEntity entity = userMapper.toEntity(dto);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getUserId()).isEqualTo("user-456");
        assertThat(entity.getEmail()).isEqualTo("jane@example.com");
        assertThat(entity.getFirstName()).isEqualTo("Jane");
        assertThat(entity.getLastName()).isEqualTo("Smith");
        assertThat(entity.getPhoneNumber()).isEqualTo("+0987654321");
        assertThat(entity.getStatus()).isEqualTo(UserEntity.UserStatus.PENDING_VERIFICATION);
        assertThat(entity.getMembershipType()).isEqualTo("SILVER");
    }

    @Test
    void toEntity_fromRegistrationRequest_shouldMapCorrectly() {
        // Given
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .email("newuser@example.com")
                .firstName("Bob")
                .lastName("Wilson")
                .phoneNumber("+1122334455")
                .password("securePassword123")
                .build();

        // When
        UserEntity entity = userMapper.toEntity(request);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getUserId()).isNull(); // Should be ignored by mapper
        assertThat(entity.getEmail()).isEqualTo("newuser@example.com");
        assertThat(entity.getFirstName()).isEqualTo("Bob");
        assertThat(entity.getLastName()).isEqualTo("Wilson");
        assertThat(entity.getPhoneNumber()).isEqualTo("+1122334455");
        assertThat(entity.getStatus()).isEqualTo(UserEntity.UserStatus.PENDING_VERIFICATION); // Constant mapping
    }

    @Test
    void toPreferencesDto_shouldMapPreferencesEntityToDto() {
        // Given
        UserPreferencesEntity entity = new UserPreferencesEntity();
        entity.setUserId("user-789");
        entity.setPreferredConnectorType("CCS");
        entity.setNotificationsEnabled(true);
        entity.setNotificationChannel("EMAIL");
        entity.setAutoChargeEnabled(false);
        entity.setMaxChargeAmount(new BigDecimal("100.0"));
        entity.setPreferredPaymentMethod("CREDIT_CARD");

        // When
        UserPreferences dto = userMapper.toPreferencesDto(entity);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getPreferredConnectorType()).isEqualTo("CCS");
        assertThat(dto.getNotificationsEnabled()).isTrue();
        assertThat(dto.getNotificationChannel()).isEqualTo("EMAIL");
        assertThat(dto.getAutoChargeEnabled()).isFalse();
        assertThat(dto.getMaxChargeAmount()).isEqualByComparingTo(new BigDecimal("100.0"));
        assertThat(dto.getPreferredPaymentMethod()).isEqualTo("CREDIT_CARD");
    }

    @Test
    void toPreferencesEntity_shouldMapPreferencesDtoToEntity() {
        // Given
        UserPreferences dto = new UserPreferences();
        dto.setPreferredConnectorType("CHAdeMO");
        dto.setNotificationsEnabled(false);
        dto.setNotificationChannel("SMS");
        dto.setAutoChargeEnabled(true);
        dto.setMaxChargeAmount(new BigDecimal("250.0"));
        dto.setPreferredPaymentMethod("DEBIT_CARD");

        // When
        UserPreferencesEntity entity = userMapper.toPreferencesEntity(dto);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getPreferredConnectorType()).isEqualTo("CHAdeMO");
        assertThat(entity.getNotificationsEnabled()).isFalse();
        assertThat(entity.getNotificationChannel()).isEqualTo("SMS");
        assertThat(entity.getAutoChargeEnabled()).isTrue();
        assertThat(entity.getMaxChargeAmount()).isEqualByComparingTo(new BigDecimal("250.0"));
        assertThat(entity.getPreferredPaymentMethod()).isEqualTo("DEBIT_CARD");
    }

    @Test
    void updateEntityFromDto_shouldUpdateOnlyNonNullFields() {
        // Given
        UserEntity entity = new UserEntity();
        entity.setUserId("user-999");
        entity.setEmail("old@example.com");
        entity.setFirstName("Old");
        entity.setLastName("Name");
        entity.setPhoneNumber("+1111111111");
        entity.setStatus(UserEntity.UserStatus.ACTIVE);
        entity.setMembershipType("BASIC");

        User dto = User.builder()
                .firstName("Updated")
                .phoneNumber("+9999999999")
                // Other fields are null
                .build();

        // When
        userMapper.updateEntityFromDto(dto, entity);

        // Then
        assertThat(entity.getUserId()).isEqualTo("user-999"); // Should not change
        assertThat(entity.getEmail()).isEqualTo("old@example.com"); // Should not change (null in DTO)
        assertThat(entity.getFirstName()).isEqualTo("Updated"); // Should be updated
        assertThat(entity.getLastName()).isEqualTo("Name"); // Should not change (null in DTO)
        assertThat(entity.getPhoneNumber()).isEqualTo("+9999999999"); // Should be updated
        assertThat(entity.getStatus()).isEqualTo(UserEntity.UserStatus.ACTIVE); // Should not change
        assertThat(entity.getMembershipType()).isEqualTo("BASIC"); // Should not change
    }

    @Test
    void enumMapping_shouldMapCorrectlyBetweenEntityAndDto() {
        // Test all enum values
        for (UserEntity.UserStatus entityStatus : UserEntity.UserStatus.values()) {
            UserEntity entity = new UserEntity();
            entity.setStatus(entityStatus);
            entity.setUserId("test");
            entity.setEmail("test@test.com");

            User dto = userMapper.toDto(entity);

            assertThat(dto.getStatus().name()).isEqualTo(entityStatus.name());
        }
    }

    @Test
    void roundTrip_shouldPreserveData() {
        // Given
        UserEntity originalEntity = new UserEntity();
        originalEntity.setUserId("round-trip-123");
        originalEntity.setEmail("roundtrip@example.com");
        originalEntity.setFirstName("Round");
        originalEntity.setLastName("Trip");
        originalEntity.setPhoneNumber("+5555555555");
        originalEntity.setStatus(UserEntity.UserStatus.SUSPENDED);
        originalEntity.setMembershipType("PLATINUM");
        originalEntity.setCreatedAt(LocalDateTime.now().minusDays(30));
        originalEntity.setLastLoginAt(LocalDateTime.now().minusHours(5));

        // When - Convert to DTO and back to Entity
        User dto = userMapper.toDto(originalEntity);
        UserEntity roundTripEntity = userMapper.toEntity(dto);

        // Then
        assertThat(roundTripEntity.getUserId()).isEqualTo(originalEntity.getUserId());
        assertThat(roundTripEntity.getEmail()).isEqualTo(originalEntity.getEmail());
        assertThat(roundTripEntity.getFirstName()).isEqualTo(originalEntity.getFirstName());
        assertThat(roundTripEntity.getLastName()).isEqualTo(originalEntity.getLastName());
        assertThat(roundTripEntity.getPhoneNumber()).isEqualTo(originalEntity.getPhoneNumber());
        assertThat(roundTripEntity.getStatus()).isEqualTo(originalEntity.getStatus());
        assertThat(roundTripEntity.getMembershipType()).isEqualTo(originalEntity.getMembershipType());
    }
}
