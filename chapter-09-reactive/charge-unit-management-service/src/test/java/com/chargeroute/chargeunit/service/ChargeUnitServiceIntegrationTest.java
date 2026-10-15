package com.chargeroute.chargeunit.service;

import com.chargeroute.chargeunit.TestSecurityConfig;
import com.chargeroute.chargeunit.dto.ChargeUnit;
import com.chargeroute.chargeunit.dto.ChargeUnitRegistrationRequest;
import com.chargeroute.chargeunit.entity.ChargeUnitEntity;
import com.chargeroute.chargeunit.repository.ChargeUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class ChargeUnitServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("chargeunitdb_test")
            .withUsername("test")
            .withPassword("test");

    /**
     * Expose the Testcontainer as a Spring bean for Flyway DataSource injection
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
    private ChargeUnitService chargeUnitService;

    @Autowired
    private ChargeUnitRepository chargeUnitRepository;

    @BeforeEach
    void setUp() {
        // R2DBC deleteAll() returns Mono<Void>, must block to ensure completion before tests
        chargeUnitRepository.deleteAll().block();
    }

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

        // When & Then - Use StepVerifier for reactive testing
        StepVerifier.create(chargeUnitService.createChargeUnit(request))
                .assertNext(createdChargeUnit -> {
                    assertThat(createdChargeUnit).isNotNull();
                    assertThat(createdChargeUnit.getChargeUnitId()).isNotNull();
                    assertThat(createdChargeUnit.getStationName()).isEqualTo("Downtown Charging Station");
                    assertThat(createdChargeUnit.getStatus()).isEqualTo("OFFLINE");
                    assertThat(createdChargeUnit.getCurrentPowerKw()).isEqualTo(0);
                })
                .verifyComplete();
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
        ChargeUnit created = chargeUnitService.createChargeUnit(request).block();

        // When & Then
        StepVerifier.create(chargeUnitService.getChargeUnit(created.getChargeUnitId()))
                .assertNext(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getStationName()).isEqualTo("Test Station");
                })
                .verifyComplete();
    }

    @Test
    void shouldUpdateChargeUnit() {
        // Given
        ChargeUnitRegistrationRequest request = ChargeUnitRegistrationRequest.builder()
                .stationName("Original Name")
                .address("Original Address")
                .latitude(new BigDecimal("37.7749"))
                .longitude(new BigDecimal("-122.4194"))
                .connectorType("CCS")
                .maxPowerKw(100)
                .pricePerKwh(new BigDecimal("0.40"))
                .build();
        ChargeUnit created = chargeUnitService.createChargeUnit(request).block();

        ChargeUnit updateDto = ChargeUnit.builder()
                .stationName("Updated Name")
                .address("Updated Address")
                .build();

        // When & Then
        StepVerifier.create(chargeUnitService.updateChargeUnit(created.getChargeUnitId(), updateDto))
                .assertNext(updated -> {
                    assertThat(updated.getStationName()).isEqualTo("Updated Name");
                    assertThat(updated.getAddress()).isEqualTo("Updated Address");
                })
                .verifyComplete();
    }

    @Test
    void shouldDeleteChargeUnit() {
        // Given
        ChargeUnitRegistrationRequest request = ChargeUnitRegistrationRequest.builder()
                .stationName("To Delete")
                .address("Delete Address")
                .latitude(new BigDecimal("37.7749"))
                .longitude(new BigDecimal("-122.4194"))
                .connectorType("Type2")
                .maxPowerKw(50)
                .pricePerKwh(new BigDecimal("0.35"))
                .build();
        ChargeUnit created = chargeUnitService.createChargeUnit(request).block();

        // When - Delete the charge unit
        chargeUnitService.deleteChargeUnit(created.getChargeUnitId()).block();

        // Then - Verify charge unit is gone
        StepVerifier.create(chargeUnitService.getChargeUnit(created.getChargeUnitId()))
                .verifyComplete();
    }

    @Test
    void shouldGetAllChargeUnits() {
        // Given
        chargeUnitService.createChargeUnit(createTestRequest("Station 1", "Address 1")).block();
        chargeUnitService.createChargeUnit(createTestRequest("Station 2", "Address 2")).block();
        chargeUnitService.createChargeUnit(createTestRequest("Station 3", "Address 3")).block();

        // When & Then - Use StepVerifier to count Flux emissions
        StepVerifier.create(chargeUnitService.getAllChargeUnits())
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    void shouldGetChargeUnitsByStatus() {
        // Given
        ChargeUnit unit1 = chargeUnitService.createChargeUnit(createTestRequest("Unit 1", "Addr 1")).block();
        chargeUnitService.createChargeUnit(createTestRequest("Unit 2", "Addr 2")).block();

        // When & Then
        StepVerifier.create(chargeUnitService.getChargeUnitsByStatus(ChargeUnitEntity.ChargeUnitStatus.OFFLINE))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void shouldGetAvailableChargeUnits() {
        // Given - Create units with different statuses
        ChargeUnit unit = chargeUnitService.createChargeUnit(createTestRequest("Available Unit", "Addr")).block();

        // Update one to AVAILABLE status
        ChargeUnit updateDto = ChargeUnit.builder()
                .status("AVAILABLE")
                .build();
        chargeUnitService.updateChargeUnit(unit.getChargeUnitId(), updateDto).block();

        // When & Then
        StepVerifier.create(chargeUnitService.getAvailableChargeUnits())
                .expectNextCount(1)
                .verifyComplete();
    }

    private ChargeUnitRegistrationRequest createTestRequest(String stationName, String address) {
        return ChargeUnitRegistrationRequest.builder()
                .stationName(stationName)
                .address(address)
                .latitude(new BigDecimal("37.7749"))
                .longitude(new BigDecimal("-122.4194"))
                .connectorType("CCS")
                .maxPowerKw(100)
                .pricePerKwh(new BigDecimal("0.40"))
                .build();
    }
}
