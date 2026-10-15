package com.chargeroute.chargeunit.service;

import com.chargeroute.chargeunit.dto.ChargeUnit;
import com.chargeroute.chargeunit.dto.ChargeUnitRegistrationRequest;
import com.chargeroute.chargeunit.repository.ChargeUnitRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class ChargeUnitServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("chargeunitdb")
            .withUsername("chargeunitservice")
            .withPassword("chargeunitpass123");

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost:9000/realms/test");
    }

    @Autowired
    private ChargeUnitService chargeUnitService;

    @Autowired
    private ChargeUnitRepository chargeUnitRepository;

    @Test
    void shouldCreateChargeUnit() {
        // Given
        ChargeUnitRegistrationRequest request = ChargeUnitRegistrationRequest.builder()
                .stationName("Downtown Charging Station")
                .address("123 Main St, San Francisco, CA")
                .latitude(new BigDecimal("37.7749"))
                .longitude(new BigDecimal("-122.4194"))
                .connectorType("CCS")
                .maxPowerKw(150)
                .pricePerKwh(new BigDecimal("0.45"))
                .lastMaintenanceDate(LocalDate.now().minusDays(30))
                .build();

        // When
        ChargeUnit createdChargeUnit = chargeUnitService.createChargeUnit(request);

        // Then
        assertThat(createdChargeUnit).isNotNull();
        assertThat(createdChargeUnit.getChargeUnitId()).isNotNull();
        assertThat(createdChargeUnit.getStationName()).isEqualTo("Downtown Charging Station");
        assertThat(createdChargeUnit.getStatus()).isEqualTo("OFFLINE");
        assertThat(createdChargeUnit.getCurrentPowerKw()).isEqualTo(0);
    }

    @Test
    void shouldGetChargeUnitById() {
        // Given
        ChargeUnitRegistrationRequest request = ChargeUnitRegistrationRequest.builder()
                .stationName("Test Station")
                .address("456 Test Ave")
                .latitude(new BigDecimal("37.7749"))
                .longitude(new BigDecimal("-122.4194"))
                .connectorType("Type2")
                .maxPowerKw(50)
                .pricePerKwh(new BigDecimal("0.35"))
                .build();
        ChargeUnit created = chargeUnitService.createChargeUnit(request);

        // When
        Optional<ChargeUnit> found = chargeUnitService.getChargeUnit(created.getChargeUnitId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getStationName()).isEqualTo("Test Station");
    }

    @Test
    void shouldUpdateChargeUnit() {
        // Given
        ChargeUnitRegistrationRequest request = ChargeUnitRegistrationRequest.builder()
                .stationName("Original Station")
                .address("789 Original St")
                .latitude(new BigDecimal("37.7749"))
                .longitude(new BigDecimal("-122.4194"))
                .connectorType("CCS")
                .maxPowerKw(100)
                .pricePerKwh(new BigDecimal("0.40"))
                .build();
        ChargeUnit created = chargeUnitService.createChargeUnit(request);

        // When
        ChargeUnit updateDto = ChargeUnit.builder()
                .stationName("Updated Station")
                .status("AVAILABLE")
                .currentPowerKw(75)
                .build();
        ChargeUnit updated = chargeUnitService.updateChargeUnit(created.getChargeUnitId(), updateDto);

        // Then
        assertThat(updated.getStationName()).isEqualTo("Updated Station");
        assertThat(updated.getStatus()).isEqualTo("AVAILABLE");
        assertThat(updated.getCurrentPowerKw()).isEqualTo(75);
    }

    @Test
    void shouldDeleteChargeUnit() {
        // Given
        ChargeUnitRegistrationRequest request = ChargeUnitRegistrationRequest.builder()
                .stationName("Delete Me Station")
                .address("Delete St")
                .latitude(new BigDecimal("37.7749"))
                .longitude(new BigDecimal("-122.4194"))
                .connectorType("Tesla")
                .maxPowerKw(250)
                .pricePerKwh(new BigDecimal("0.50"))
                .build();
        ChargeUnit created = chargeUnitService.createChargeUnit(request);

        // When
        chargeUnitService.deleteChargeUnit(created.getChargeUnitId());

        // Then
        Optional<ChargeUnit> found = chargeUnitService.getChargeUnit(created.getChargeUnitId());
        assertThat(found).isEmpty();
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentChargeUnit() {
        // When/Then
        assertThatThrownBy(() -> chargeUnitService.deleteChargeUnit("non-existent-id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Charge unit not found");
    }

    @Test
    void shouldGetAllChargeUnits() {
        // Given
        chargeUnitRepository.deleteAll();
        ChargeUnitRegistrationRequest request1 = ChargeUnitRegistrationRequest.builder()
                .stationName("Station 1")
                .address("Address 1")
                .latitude(new BigDecimal("37.7749"))
                .longitude(new BigDecimal("-122.4194"))
                .connectorType("CCS")
                .maxPowerKw(150)
                .pricePerKwh(new BigDecimal("0.45"))
                .build();
        ChargeUnitRegistrationRequest request2 = ChargeUnitRegistrationRequest.builder()
                .stationName("Station 2")
                .address("Address 2")
                .latitude(new BigDecimal("37.7849"))
                .longitude(new BigDecimal("-122.4294"))
                .connectorType("Type2")
                .maxPowerKw(50)
                .pricePerKwh(new BigDecimal("0.35"))
                .build();
        chargeUnitService.createChargeUnit(request1);
        chargeUnitService.createChargeUnit(request2);

        // When
        List<ChargeUnit> allChargeUnits = chargeUnitService.getAllChargeUnits();

        // Then
        assertThat(allChargeUnits).hasSize(2);
    }

    @Test
    void shouldGetAvailableChargeUnits() {
        // Given
        chargeUnitRepository.deleteAll();
        ChargeUnitRegistrationRequest request = ChargeUnitRegistrationRequest.builder()
                .stationName("Available Station")
                .address("Available St")
                .latitude(new BigDecimal("37.7749"))
                .longitude(new BigDecimal("-122.4194"))
                .connectorType("CCS")
                .maxPowerKw(150)
                .pricePerKwh(new BigDecimal("0.45"))
                .build();
        ChargeUnit created = chargeUnitService.createChargeUnit(request);

        // Update to available
        ChargeUnit updateDto = ChargeUnit.builder().status("AVAILABLE").build();
        chargeUnitService.updateChargeUnit(created.getChargeUnitId(), updateDto);

        // When
        List<ChargeUnit> availableUnits = chargeUnitService.getAvailableChargeUnits();

        // Then
        assertThat(availableUnits).isNotEmpty();
        assertThat(availableUnits).allMatch(unit -> unit.getStatus().equals("AVAILABLE"));
    }

    @Test
    void shouldFindChargeUnitsNearLocation() {
        // Given
        chargeUnitRepository.deleteAll();
        BigDecimal centerLat = new BigDecimal("37.7749");
        BigDecimal centerLon = new BigDecimal("-122.4194");

        ChargeUnitRegistrationRequest nearbyRequest = ChargeUnitRegistrationRequest.builder()
                .stationName("Nearby Station")
                .address("Nearby St")
                .latitude(centerLat.add(new BigDecimal("0.01"))) // ~1 km away
                .longitude(centerLon.add(new BigDecimal("0.01")))
                .connectorType("CCS")
                .maxPowerKw(150)
                .pricePerKwh(new BigDecimal("0.45"))
                .build();
        chargeUnitService.createChargeUnit(nearbyRequest);

        // When
        List<ChargeUnit> nearbyUnits = chargeUnitService.findNearLocation(centerLat, centerLon, 5.0);

        // Then
        assertThat(nearbyUnits).isNotEmpty();
    }
}
