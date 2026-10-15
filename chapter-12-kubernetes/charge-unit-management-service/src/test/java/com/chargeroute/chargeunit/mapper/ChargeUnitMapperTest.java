package com.chargeroute.chargeunit.mapper;

import com.chargeroute.chargeunit.dto.ChargeUnit;
import com.chargeroute.chargeunit.dto.ChargeUnitRegistrationRequest;
import com.chargeroute.chargeunit.entity.ChargeUnitEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * High-level tests for ChargeUnitMapper to verify MapStruct mapping functionality.
 * Tests entity-to-DTO and DTO-to-entity conversions for charge units.
 */
@SpringBootTest
@ActiveProfiles("test")
class ChargeUnitMapperTest {

    @Autowired
    private ChargeUnitMapper chargeUnitMapper;

    @Test
    void toDto_shouldMapEntityToDto() {
        // Given
        ChargeUnitEntity entity = new ChargeUnitEntity();
        entity.setChargeUnitId("unit-123");
        entity.setStationName("Downtown Charging Station");
        entity.setAddress("123 Main St, City, State 12345");
        entity.setLatitude(new BigDecimal("40.7128"));
        entity.setLongitude(new BigDecimal("-74.0060"));
        entity.setConnectorType(ChargeUnitEntity.ConnectorType.CCS);
        entity.setMaxPowerKw(150);
        entity.setCurrentPowerKw(145);
        entity.setStatus(ChargeUnitEntity.ChargeUnitStatus.AVAILABLE);
        entity.setPricePerKwh(new BigDecimal("0.35"));
        entity.setLastMaintenanceDate(LocalDate.now().minusDays(30));

        // When
        ChargeUnit dto = chargeUnitMapper.toDto(entity);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getChargeUnitId()).isEqualTo("unit-123");
        assertThat(dto.getStationName()).isEqualTo("Downtown Charging Station");
        assertThat(dto.getAddress()).isEqualTo("123 Main St, City, State 12345");
        assertThat(dto.getLatitude()).isEqualTo(new BigDecimal("40.7128"));
        assertThat(dto.getLongitude()).isEqualTo(new BigDecimal("-74.0060"));
        assertThat(dto.getConnectorType()).isEqualTo("CCS");
        assertThat(dto.getMaxPowerKw()).isEqualTo(150);
        assertThat(dto.getCurrentPowerKw()).isEqualTo(145);
        assertThat(dto.getStatus()).isEqualTo("AVAILABLE");
        assertThat(dto.getPricePerKwh()).isEqualTo(new BigDecimal("0.35"));
        assertThat(dto.getLastMaintenanceDate()).isEqualTo(entity.getLastMaintenanceDate());
    }

    @Test
    void toDto_shouldHandleNullEntity() {
        // When
        ChargeUnit dto = chargeUnitMapper.toDto(null);

        // Then
        assertThat(dto).isNull();
    }

    @Test
    void toEntity_shouldMapDtoToEntity() {
        // Given
        ChargeUnit dto = new ChargeUnit();
        dto.setChargeUnitId("unit-456");
        dto.setStationName("Airport Fast Charger");
        dto.setAddress("Airport Terminal 1");
        dto.setLatitude(new BigDecimal("37.7749"));
        dto.setLongitude(new BigDecimal("-122.4194"));
        dto.setConnectorType("CHAdeMO");
        dto.setMaxPowerKw(100);
        dto.setCurrentPowerKw(95);
        dto.setStatus("OCCUPIED");
        dto.setPricePerKwh(new BigDecimal("0.40"));
        dto.setLastMaintenanceDate(LocalDate.now().minusDays(15));

        // When
        ChargeUnitEntity entity = chargeUnitMapper.toEntity(dto);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getChargeUnitId()).isEqualTo("unit-456");
        assertThat(entity.getStationName()).isEqualTo("Airport Fast Charger");
        assertThat(entity.getAddress()).isEqualTo("Airport Terminal 1");
        assertThat(entity.getLatitude()).isEqualTo(new BigDecimal("37.7749"));
        assertThat(entity.getLongitude()).isEqualTo(new BigDecimal("-122.4194"));
        assertThat(entity.getConnectorType()).isEqualTo(ChargeUnitEntity.ConnectorType.CHAdeMO);
        assertThat(entity.getMaxPowerKw()).isEqualTo(100);
        assertThat(entity.getCurrentPowerKw()).isEqualTo(95);
        assertThat(entity.getStatus()).isEqualTo(ChargeUnitEntity.ChargeUnitStatus.OCCUPIED);
        assertThat(entity.getPricePerKwh()).isEqualTo(new BigDecimal("0.40"));
    }

    @Test
    void toEntity_fromRegistrationRequest_shouldMapCorrectly() {
        // Given
        ChargeUnitRegistrationRequest request = ChargeUnitRegistrationRequest.builder()
                .stationName("New Station")
                .address("789 New St")
                .latitude(new BigDecimal("34.0522"))
                .longitude(new BigDecimal("-118.2437"))
                .connectorType("Type2")
                .maxPowerKw(50)
                .pricePerKwh(new BigDecimal("0.30"))
                .build();

        // When
        ChargeUnitEntity entity = chargeUnitMapper.toEntity(request);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getChargeUnitId()).isNull(); // Should be ignored
        assertThat(entity.getStationName()).isEqualTo("New Station");
        assertThat(entity.getAddress()).isEqualTo("789 New St");
        assertThat(entity.getLatitude()).isEqualTo(new BigDecimal("34.0522"));
        assertThat(entity.getLongitude()).isEqualTo(new BigDecimal("-118.2437"));
        assertThat(entity.getConnectorType()).isEqualTo(ChargeUnitEntity.ConnectorType.Type2);
        assertThat(entity.getMaxPowerKw()).isEqualTo(50);
        assertThat(entity.getStatus()).isEqualTo(ChargeUnitEntity.ChargeUnitStatus.OFFLINE); // Default
        assertThat(entity.getPricePerKwh()).isEqualTo(new BigDecimal("0.30"));
    }

    @Test
    void updateEntityFromDto_shouldUpdateOnlyNonNullFields() {
        // Given
        ChargeUnitEntity entity = new ChargeUnitEntity();
        entity.setChargeUnitId("unit-999");
        entity.setStationName("Old Station");
        entity.setAddress("Old Address");
        entity.setStatus(ChargeUnitEntity.ChargeUnitStatus.AVAILABLE);
        entity.setPricePerKwh(new BigDecimal("0.25"));
        entity.setMaxPowerKw(100);

        ChargeUnit dto = new ChargeUnit();
        dto.setStationName("Updated Station");
        dto.setPricePerKwh(new BigDecimal("0.45"));
        // Other fields are null

        // When
        chargeUnitMapper.updateEntityFromDto(dto, entity);

        // Then
        assertThat(entity.getChargeUnitId()).isEqualTo("unit-999"); // Should not change
        assertThat(entity.getStationName()).isEqualTo("Updated Station"); // Should be updated
        assertThat(entity.getAddress()).isEqualTo("Old Address"); // Should not change
        assertThat(entity.getStatus()).isEqualTo(ChargeUnitEntity.ChargeUnitStatus.AVAILABLE); // Should not change
        assertThat(entity.getPricePerKwh()).isEqualTo(new BigDecimal("0.45")); // Should be updated
        assertThat(entity.getMaxPowerKw()).isEqualTo(100); // Should not change
    }

    @Test
    void connectorTypeMapping_shouldHandleAllTypes() {
        // Test all connector types
        ChargeUnitEntity.ConnectorType[] types = ChargeUnitEntity.ConnectorType.values();

        for (ChargeUnitEntity.ConnectorType type : types) {
            ChargeUnitEntity entity = new ChargeUnitEntity();
            entity.setChargeUnitId("test");
            entity.setStationName("Test");
            entity.setConnectorType(type);
            entity.setStatus(ChargeUnitEntity.ChargeUnitStatus.AVAILABLE);

            ChargeUnit dto = chargeUnitMapper.toDto(entity);

            assertThat(dto.getConnectorType()).isEqualTo(type.name());
        }
    }

    @Test
    void statusMapping_shouldHandleAllStatuses() {
        // Test all status values
        ChargeUnitEntity.ChargeUnitStatus[] statuses = ChargeUnitEntity.ChargeUnitStatus.values();

        for (ChargeUnitEntity.ChargeUnitStatus status : statuses) {
            ChargeUnitEntity entity = new ChargeUnitEntity();
            entity.setChargeUnitId("test");
            entity.setStationName("Test");
            entity.setConnectorType(ChargeUnitEntity.ConnectorType.CCS);
            entity.setStatus(status);

            ChargeUnit dto = chargeUnitMapper.toDto(entity);

            assertThat(dto.getStatus()).isEqualTo(status.name());
        }
    }

    @Test
    void stringToEnum_shouldParseConnectorTypeCorrectly() {
        // Given
        ChargeUnit dto = new ChargeUnit();
        dto.setChargeUnitId("test");
        dto.setStationName("Test");
        dto.setConnectorType("Tesla");
        dto.setStatus("AVAILABLE");

        // When
        ChargeUnitEntity entity = chargeUnitMapper.toEntity(dto);

        // Then
        assertThat(entity.getConnectorType()).isEqualTo(ChargeUnitEntity.ConnectorType.Tesla);
    }

    @Test
    void stringToEnum_shouldParseStatusCorrectly() {
        // Given
        ChargeUnit dto = new ChargeUnit();
        dto.setChargeUnitId("test");
        dto.setStationName("Test");
        dto.setConnectorType("CCS");
        dto.setStatus("MAINTENANCE");

        // When
        ChargeUnitEntity entity = chargeUnitMapper.toEntity(dto);

        // Then
        assertThat(entity.getStatus()).isEqualTo(ChargeUnitEntity.ChargeUnitStatus.MAINTENANCE);
    }

    @Test
    void roundTrip_shouldPreserveData() {
        // Given
        ChargeUnitEntity originalEntity = new ChargeUnitEntity();
        originalEntity.setChargeUnitId("round-trip-789");
        originalEntity.setStationName("Round Trip Station");
        originalEntity.setAddress("Round Trip Address");
        originalEntity.setLatitude(new BigDecimal("51.5074"));
        originalEntity.setLongitude(new BigDecimal("-0.1278"));
        originalEntity.setConnectorType(ChargeUnitEntity.ConnectorType.Type2);
        originalEntity.setMaxPowerKw(75);
        originalEntity.setCurrentPowerKw(70);
        originalEntity.setStatus(ChargeUnitEntity.ChargeUnitStatus.OCCUPIED);
        originalEntity.setPricePerKwh(new BigDecimal("0.38"));
        originalEntity.setLastMaintenanceDate(LocalDate.now().minusDays(45));

        // When - Convert to DTO and back to Entity
        ChargeUnit dto = chargeUnitMapper.toDto(originalEntity);
        ChargeUnitEntity roundTripEntity = chargeUnitMapper.toEntity(dto);

        // Then
        assertThat(roundTripEntity.getChargeUnitId()).isEqualTo(originalEntity.getChargeUnitId());
        assertThat(roundTripEntity.getStationName()).isEqualTo(originalEntity.getStationName());
        assertThat(roundTripEntity.getAddress()).isEqualTo(originalEntity.getAddress());
        assertThat(roundTripEntity.getLatitude()).isEqualTo(originalEntity.getLatitude());
        assertThat(roundTripEntity.getLongitude()).isEqualTo(originalEntity.getLongitude());
        assertThat(roundTripEntity.getConnectorType()).isEqualTo(originalEntity.getConnectorType());
        assertThat(roundTripEntity.getMaxPowerKw()).isEqualTo(originalEntity.getMaxPowerKw());
        assertThat(roundTripEntity.getCurrentPowerKw()).isEqualTo(originalEntity.getCurrentPowerKw());
        assertThat(roundTripEntity.getStatus()).isEqualTo(originalEntity.getStatus());
        assertThat(roundTripEntity.getPricePerKwh()).isEqualTo(originalEntity.getPricePerKwh());
    }

    @Test
    void bigDecimalMapping_shouldPreservePrecision() {
        // Given
        ChargeUnitEntity entity = new ChargeUnitEntity();
        entity.setChargeUnitId("precision-test");
        entity.setStationName("Precision Test");
        entity.setLatitude(new BigDecimal("40.712776"));
        entity.setLongitude(new BigDecimal("-74.005974"));
        entity.setPricePerKwh(new BigDecimal("0.3567"));
        entity.setConnectorType(ChargeUnitEntity.ConnectorType.CCS);
        entity.setStatus(ChargeUnitEntity.ChargeUnitStatus.AVAILABLE);

        // When
        ChargeUnit dto = chargeUnitMapper.toDto(entity);

        // Then
        assertThat(dto.getLatitude()).isEqualByComparingTo(entity.getLatitude());
        assertThat(dto.getLongitude()).isEqualByComparingTo(entity.getLongitude());
        assertThat(dto.getPricePerKwh()).isEqualByComparingTo(entity.getPricePerKwh());
    }
}
