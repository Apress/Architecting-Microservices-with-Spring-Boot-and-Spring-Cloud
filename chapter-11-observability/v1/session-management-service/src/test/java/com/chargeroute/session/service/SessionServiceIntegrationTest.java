package com.chargeroute.session.service;

import com.chargeroute.session.TestSecurityConfig;
import com.chargeroute.session.dto.ChargeSession;
import com.chargeroute.session.repository.ChargingSessionRepository;
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
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration"})
@Testcontainers
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class SessionServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("sessiondb_test")
            .withUsername("test")
            .withPassword("test");

    @org.springframework.boot.test.context.TestConfiguration
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
    private SessionService sessionService;

    @Autowired
    private ChargingSessionRepository sessionRepository;

    @BeforeEach
    void setUp() {
        sessionRepository.deleteAll().block();
    }

    @Test
    void shouldCreateSession() {
        ChargeSession session = ChargeSession.builder()
                .userId("user-123")
                .chargeUnitId("cu-456")
                .startTime(LocalDateTime.now())
                .build();

        StepVerifier.create(sessionService.createSession(session))
                .assertNext(created -> {
                    assertThat(created).isNotNull();
                    assertThat(created.getId()).isNotNull();
                    assertThat(created.getUserId()).isEqualTo("user-123");
                    assertThat(created.getStatus()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void shouldGetSessionById() {
        ChargeSession session = createTestSession("user-123", "cu-456");
        ChargeSession created = sessionService.createSession(session).block();

        StepVerifier.create(sessionService.getSession(created.getId()))
                .assertNext(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getUserId()).isEqualTo("user-123");
                })
                .verifyComplete();
    }

    @Test
    void shouldGetSessionByIdNotFound() {
        StepVerifier.create(sessionService.getSession("non-existent"))
                .verifyComplete();
    }

    @Test
    void shouldGetAllSessions() {
        sessionService.createSession(createTestSession("user-1", "cu-1")).block();
        sessionService.createSession(createTestSession("user-2", "cu-2")).block();
        sessionService.createSession(createTestSession("user-3", "cu-3")).block();

        StepVerifier.create(sessionService.getAllSessions())
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    void shouldGetSessionsByUserId() {
        sessionService.createSession(createTestSession("user-123", "cu-1")).block();
        sessionService.createSession(createTestSession("user-123", "cu-2")).block();
        sessionService.createSession(createTestSession("user-456", "cu-3")).block();

        StepVerifier.create(sessionService.getSessionsByUserId("user-123"))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void shouldGetSessionsByChargeUnitId() {
        sessionService.createSession(createTestSession("user-1", "cu-123")).block();
        sessionService.createSession(createTestSession("user-2", "cu-123")).block();
        sessionService.createSession(createTestSession("user-3", "cu-456")).block();

        StepVerifier.create(sessionService.getSessionsByChargeUnitId("cu-123"))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void shouldGetActiveSessions() {
        sessionService.createSession(createTestSession("user-1", "cu-1")).block();
        sessionService.createSession(createTestSession("user-2", "cu-2")).block();

        StepVerifier.create(sessionService.getActiveSessions())
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void shouldUpdateSession() {
        ChargeSession session = createTestSession("user-123", "cu-456");
        ChargeSession created = sessionService.createSession(session).block();

        ChargeSession updateDto = ChargeSession.builder()
                .status("ACTIVE")
                .energyDelivered(15.5)
                .totalCost(new BigDecimal("7.75"))
                .build();

        StepVerifier.create(sessionService.updateSession(created.getId(), updateDto))
                .assertNext(updated -> {
                    assertThat(updated.getStatus()).isEqualTo("ACTIVE");
                    assertThat(updated.getEnergyDelivered()).isEqualTo(15.5);
                })
                .verifyComplete();
    }

    @Test
    void shouldDeleteSession() {
        ChargeSession session = createTestSession("user-123", "cu-456");
        ChargeSession created = sessionService.createSession(session).block();

        StepVerifier.create(sessionService.deleteSession(created.getId()))
                .assertNext(deleted -> assertThat(deleted).isTrue())
                .verifyComplete();

        StepVerifier.create(sessionService.getSession(created.getId()))
                .verifyComplete();
    }

    @Test
    void shouldDeleteSessionNotFound() {
        StepVerifier.create(sessionService.deleteSession("non-existent"))
                .assertNext(deleted -> assertThat(deleted).isFalse())
                .verifyComplete();
    }

    private ChargeSession createTestSession(String userId, String chargeUnitId) {
        return ChargeSession.builder()
                .userId(userId)
                .chargeUnitId(chargeUnitId)
                .startTime(LocalDateTime.now())
                .build();
    }
}
