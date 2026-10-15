package com.chargeroute.session.mapper;

import com.chargeroute.session.dto.ChargeSession;
import com.chargeroute.session.dto.SessionMetric;
import com.chargeroute.session.entity.ChargingSessionEntity;
import com.chargeroute.session.entity.SessionMetricEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * High-level tests for SessionMapper to verify MapStruct mapping functionality.
 * Tests entity-to-DTO and DTO-to-entity conversions for charging sessions and metrics.
 */
@SpringBootTest
@ActiveProfiles("test")
class SessionMapperTest {




    @Autowired
    private SessionMapper sessionMapper;

    @Test
    void toDto_shouldMapEntityToDto() {
        // Given
        ChargingSessionEntity entity = new ChargingSessionEntity();
        entity.setSessionId("session-123");
        entity.setUserId("user-456");
        entity.setChargeUnitId("unit-789");
        entity.setStatus(ChargingSessionEntity.SessionStatus.ACTIVE);
        entity.setStartTime(LocalDateTime.now().minusHours(1));
        entity.setInitialBatteryLevel(25);
        entity.setFinalBatteryLevel(85);
        entity.setEnergyDeliveredKwh(new BigDecimal("45.5"));
        entity.setTotalCost(new BigDecimal("18.20"));

        // When
        ChargeSession dto = sessionMapper.toDto(entity);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo("session-123");
        assertThat(dto.getUserId()).isEqualTo("user-456");
        assertThat(dto.getChargeUnitId()).isEqualTo("unit-789");
        assertThat(dto.getStatus()).isEqualTo("ACTIVE");
        assertThat(dto.getStartTime()).isEqualTo(entity.getStartTime());
        assertThat(dto.getEnergyDelivered()).isEqualTo(45.5);
        assertThat(dto.getTotalCost()).isEqualTo(new BigDecimal("18.20"));
    }

    @Test
    void toDto_shouldHandleNullEntity() {
        // When
        ChargeSession dto = sessionMapper.toDto(null);

        // Then
        assertThat(dto).isNull();
    }

    @Test
    void toEntity_shouldMapDtoToEntity() {
        // Given
        ChargeSession dto = new ChargeSession();
        dto.setId("session-789");
        dto.setUserId("user-111");
        dto.setChargeUnitId("unit-222");
        dto.setStatus("COMPLETED");
        dto.setStartTime(LocalDateTime.now().minusHours(2));
        dto.setEndTime(LocalDateTime.now());
        dto.setEnergyDelivered(60.0);
        dto.setTotalCost(new BigDecimal("24.00"));

        // When
        ChargingSessionEntity entity = sessionMapper.toEntity(dto);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getSessionId()).isEqualTo("session-789");
        assertThat(entity.getUserId()).isEqualTo("user-111");
        assertThat(entity.getChargeUnitId()).isEqualTo("unit-222");
        assertThat(entity.getStatus()).isEqualTo(ChargingSessionEntity.SessionStatus.COMPLETED);
        assertThat(entity.getStartTime()).isEqualTo(dto.getStartTime());
        assertThat(entity.getEndTime()).isEqualTo(dto.getEndTime());
        assertThat(entity.getEnergyDeliveredKwh()).isEqualByComparingTo(new BigDecimal("60.0"));
        assertThat(entity.getTotalCost()).isEqualByComparingTo(new BigDecimal("24.00"));
    }

    @Test
    void toMetricDto_shouldMapMetricEntityToDto() {
        // Given
        ChargingSessionEntity session = new ChargingSessionEntity();
        session.setSessionId("session-123");

        SessionMetricEntity entity = new SessionMetricEntity();
        entity.setMetricId(42L);
        entity.setSession(session);
        entity.setRecordedAt(LocalDateTime.now());
        entity.setCurrentPowerKw(new BigDecimal("75.5"));
        entity.setVoltage(new BigDecimal("420.0"));
        entity.setCurrentAmps(new BigDecimal("180.0"));
        entity.setBatteryLevel(55);
        entity.setTemperatureCelsius(new BigDecimal("30.0"));

        // When
        SessionMetric dto = sessionMapper.toMetricDto(entity);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getMetricId()).isEqualTo(42L);
        assertThat(dto.getSessionId()).isEqualTo("session-123");
        assertThat(dto.getRecordedAt()).isEqualTo(entity.getRecordedAt());
        assertThat(dto.getCurrentPowerKw()).isEqualByComparingTo(new BigDecimal("75.5"));
        assertThat(dto.getVoltage()).isEqualByComparingTo(new BigDecimal("420.0"));
        assertThat(dto.getCurrentAmps()).isEqualByComparingTo(new BigDecimal("180.0"));
        assertThat(dto.getBatteryLevel()).isEqualTo(55);
        assertThat(dto.getTemperatureCelsius()).isEqualByComparingTo(new BigDecimal("30.0"));
    }

    @Test
    void toMetricEntity_shouldMapMetricDtoToEntity() {
        // Given
        SessionMetric dto = new SessionMetric();
        dto.setMetricId(99L);
        dto.setSessionId("session-456");
        dto.setRecordedAt(LocalDateTime.now().minusMinutes(5));
        dto.setCurrentPowerKw(new BigDecimal("42.0"));
        dto.setVoltage(new BigDecimal("380.0"));
        dto.setCurrentAmps(new BigDecimal("110.0"));
        dto.setBatteryLevel(72);
        dto.setTemperatureCelsius(new BigDecimal("26.0"));

        // When
        SessionMetricEntity entity = sessionMapper.toMetricEntity(dto);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getMetricId()).isEqualTo(99L);
        assertThat(entity.getRecordedAt()).isEqualTo(dto.getRecordedAt());
        assertThat(entity.getCurrentPowerKw()).isEqualByComparingTo(new BigDecimal("42.0"));
        assertThat(entity.getVoltage()).isEqualByComparingTo(new BigDecimal("380.0"));
        assertThat(entity.getCurrentAmps()).isEqualByComparingTo(new BigDecimal("110.0"));
        assertThat(entity.getBatteryLevel()).isEqualTo(72);
        assertThat(entity.getTemperatureCelsius()).isEqualByComparingTo(new BigDecimal("26.0"));
    }

    @Test
    void updateEntityFromDto_shouldUpdateOnlyNonNullFields() {
        // Given
        ChargingSessionEntity entity = new ChargingSessionEntity();
        entity.setSessionId("session-update-test");
        entity.setUserId("user-original");
        entity.setChargeUnitId("unit-original");
        entity.setStatus(ChargingSessionEntity.SessionStatus.ACTIVE);
        entity.setInitialBatteryLevel(20);
        entity.setEnergyDeliveredKwh(new BigDecimal("10.0"));

        ChargeSession dto = new ChargeSession();
        dto.setStatus("COMPLETED");
        dto.setEnergyDelivered(55.5);
        // Other fields are null

        // When
        sessionMapper.updateEntityFromDto(dto, entity);

        // Then
        assertThat(entity.getSessionId()).isEqualTo("session-update-test"); // Should not change
        assertThat(entity.getUserId()).isEqualTo("user-original"); // Should not change
        assertThat(entity.getChargeUnitId()).isEqualTo("unit-original"); // Should not change
        assertThat(entity.getStatus()).isEqualTo(ChargingSessionEntity.SessionStatus.COMPLETED); // Should be updated
        assertThat(entity.getInitialBatteryLevel()).isEqualTo(20); // Should not change
        assertThat(entity.getEnergyDeliveredKwh()).isEqualByComparingTo(new BigDecimal("55.5")); // Should be updated
    }

    @Test
    void statusMapping_shouldHandleAllStatuses() {
        // Test all status values
        ChargingSessionEntity.SessionStatus[] statuses = ChargingSessionEntity.SessionStatus.values();

        for (ChargingSessionEntity.SessionStatus status : statuses) {
            ChargingSessionEntity entity = new ChargingSessionEntity();
            entity.setSessionId("test-" + status.name());
            entity.setUserId("user");
            entity.setChargeUnitId("unit");
            entity.setStatus(status);

            ChargeSession dto = sessionMapper.toDto(entity);

            assertThat(dto.getStatus()).isEqualTo(status.name());
        }
    }

    @Test
    void stringToEnum_shouldParseStatusCorrectly() {
        // Given
        ChargeSession dto = new ChargeSession();
        dto.setId("test");
        dto.setUserId("user");
        dto.setChargeUnitId("unit");
        dto.setStatus("FAILED");

        // When
        ChargingSessionEntity entity = sessionMapper.toEntity(dto);

        // Then
        assertThat(entity.getStatus()).isEqualTo(ChargingSessionEntity.SessionStatus.FAILED);
    }

    @Test
    void roundTrip_shouldPreserveSessionData() {
        // Given
        ChargingSessionEntity originalEntity = new ChargingSessionEntity();
        originalEntity.setSessionId("round-trip-session");
        originalEntity.setUserId("user-999");
        originalEntity.setChargeUnitId("unit-888");
        originalEntity.setStatus(ChargingSessionEntity.SessionStatus.COMPLETED);
        originalEntity.setStartTime(LocalDateTime.now().minusHours(3));
        originalEntity.setEndTime(LocalDateTime.now());
        originalEntity.setInitialBatteryLevel(10);
        originalEntity.setFinalBatteryLevel(100);
        originalEntity.setEnergyDeliveredKwh(new BigDecimal("72.3"));
        originalEntity.setTotalCost(new BigDecimal("28.92"));

        // When - Convert to DTO and back to Entity
        ChargeSession dto = sessionMapper.toDto(originalEntity);
        ChargingSessionEntity roundTripEntity = sessionMapper.toEntity(dto);

        // Then
        assertThat(roundTripEntity.getSessionId()).isEqualTo(originalEntity.getSessionId());
        assertThat(roundTripEntity.getUserId()).isEqualTo(originalEntity.getUserId());
        assertThat(roundTripEntity.getChargeUnitId()).isEqualTo(originalEntity.getChargeUnitId());
        assertThat(roundTripEntity.getStatus()).isEqualTo(originalEntity.getStatus());
        assertThat(roundTripEntity.getStartTime()).isEqualTo(originalEntity.getStartTime());
        assertThat(roundTripEntity.getEndTime()).isEqualTo(originalEntity.getEndTime());
        assertThat(roundTripEntity.getEnergyDeliveredKwh()).isEqualByComparingTo(originalEntity.getEnergyDeliveredKwh());
        assertThat(roundTripEntity.getTotalCost()).isEqualByComparingTo(originalEntity.getTotalCost());
    }

    @Test
    void bigDecimalMapping_shouldPreservePrecision() {
        // Given
        ChargingSessionEntity entity = new ChargingSessionEntity();
        entity.setSessionId("precision-test");
        entity.setUserId("user");
        entity.setChargeUnitId("unit");
        entity.setStatus(ChargingSessionEntity.SessionStatus.ACTIVE);
        entity.setEnergyDeliveredKwh(new BigDecimal("45.123456"));
        entity.setTotalCost(new BigDecimal("18.049382"));

        // When
        ChargeSession dto = sessionMapper.toDto(entity);

        // Then
        assertThat(dto.getEnergyDelivered()).isEqualTo(45.123456);
        assertThat(dto.getTotalCost()).isEqualByComparingTo(entity.getTotalCost());
    }

    @Test
    void toMetricDtoList_shouldMapList() {
        // Given
        ChargingSessionEntity session = new ChargingSessionEntity();
        session.setSessionId("session-with-metrics");

        List<SessionMetricEntity> entities = new ArrayList<>();

        SessionMetricEntity metric1 = new SessionMetricEntity();
        metric1.setMetricId(1L);
        metric1.setSession(session);
        metric1.setRecordedAt(LocalDateTime.now().minusMinutes(20));
        metric1.setCurrentPowerKw(new BigDecimal("50.0"));
        metric1.setBatteryLevel(35);
        entities.add(metric1);

        SessionMetricEntity metric2 = new SessionMetricEntity();
        metric2.setMetricId(2L);
        metric2.setSession(session);
        metric2.setRecordedAt(LocalDateTime.now().minusMinutes(10));
        metric2.setCurrentPowerKw(new BigDecimal("48.5"));
        metric2.setBatteryLevel(65);
        entities.add(metric2);

        // When
        List<SessionMetric> dtos = sessionMapper.toMetricDtoList(entities);

        // Then
        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getCurrentPowerKw()).isEqualByComparingTo(new BigDecimal("50.0"));
        assertThat(dtos.get(0).getBatteryLevel()).isEqualTo(35);
        assertThat(dtos.get(1).getCurrentPowerKw()).isEqualByComparingTo(new BigDecimal("48.5"));
        assertThat(dtos.get(1).getBatteryLevel()).isEqualTo(65);
    }
}
