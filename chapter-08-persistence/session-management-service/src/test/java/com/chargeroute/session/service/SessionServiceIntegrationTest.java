package com.chargeroute.session.service;

import com.chargeroute.session.dto.ChargeSession;
import com.chargeroute.session.entity.ChargingSessionEntity;
import com.chargeroute.session.repository.ChargingSessionRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for SessionService using Testcontainers
 * Tests the complete persistence layer with a real PostgreSQL database
 */
@SpringBootTest
@Testcontainers
@Transactional
class SessionServiceIntegrationTest {

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
    private SessionService sessionService;

    @Autowired
    private ChargingSessionRepository sessionRepository;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        sessionRepository.deleteAll();
    }

    @Test
    void createSession_ShouldPersistSession() {
        // Given
        ChargeSession sessionDto = ChargeSession.builder()
                .id(UUID.randomUUID().toString())
                .userId("USER-001")
                .chargeUnitId("UNIT-001")
                .status("INITIATED")
                .startTime(LocalDateTime.now())
                .build();

        // When
        ChargeSession createdSession = sessionService.createSession(sessionDto);

        // Then
        assertThat(createdSession).isNotNull();
        assertThat(createdSession.getId()).isEqualTo(sessionDto.getId());
        assertThat(createdSession.getUserId()).isEqualTo("USER-001");
        assertThat(createdSession.getChargeUnitId()).isEqualTo("UNIT-001");
        assertThat(createdSession.getStatus()).isEqualTo("INITIATED");

        // Verify it's in the database
        Optional<ChargingSessionEntity> persistedEntity = sessionRepository.findById(sessionDto.getId());
        assertThat(persistedEntity).isPresent();
        assertThat(persistedEntity.get().getUserId()).isEqualTo("USER-001");
    }

    @Test
    void getSession_ShouldReturnSession_WhenExists() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        ChargingSessionEntity entity = new ChargingSessionEntity();
        entity.setSessionId(sessionId);
        entity.setUserId("USER-002");
        entity.setChargeUnitId("UNIT-002");
        entity.setStatus(ChargingSessionEntity.SessionStatus.ACTIVE);
        entity.setStartTime(LocalDateTime.now());
        sessionRepository.save(entity);

        // When
        Optional<ChargeSession> result = sessionService.getSession(sessionId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(sessionId);
        assertThat(result.get().getUserId()).isEqualTo("USER-002");
        assertThat(result.get().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void getSession_ShouldReturnEmpty_WhenNotExists() {
        // When
        Optional<ChargeSession> result = sessionService.getSession("NON-EXISTENT");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getSessionsByUserId_ShouldReturnUserSessions() {
        // Given
        String userId = "USER-003";

        ChargingSessionEntity session1 = new ChargingSessionEntity();
        session1.setSessionId(UUID.randomUUID().toString());
        session1.setUserId(userId);
        session1.setChargeUnitId("UNIT-001");
        session1.setStatus(ChargingSessionEntity.SessionStatus.ACTIVE);
        session1.setStartTime(LocalDateTime.now());
        sessionRepository.save(session1);

        ChargingSessionEntity session2 = new ChargingSessionEntity();
        session2.setSessionId(UUID.randomUUID().toString());
        session2.setUserId(userId);
        session2.setChargeUnitId("UNIT-002");
        session2.setStatus(ChargingSessionEntity.SessionStatus.COMPLETED);
        session2.setStartTime(LocalDateTime.now().minusHours(1));
        session2.setEndTime(LocalDateTime.now());
        sessionRepository.save(session2);

        // Session for different user
        ChargingSessionEntity session3 = new ChargingSessionEntity();
        session3.setSessionId(UUID.randomUUID().toString());
        session3.setUserId("USER-999");
        session3.setChargeUnitId("UNIT-003");
        session3.setStatus(ChargingSessionEntity.SessionStatus.ACTIVE);
        session3.setStartTime(LocalDateTime.now());
        sessionRepository.save(session3);

        // When
        List<ChargeSession> userSessions = sessionService.getSessionsByUserId(userId);

        // Then
        assertThat(userSessions).hasSize(2);
        assertThat(userSessions).allMatch(s -> s.getUserId().equals(userId));
    }

    @Test
    void getActiveSessions_ShouldReturnOnlyActiveSessions() {
        // Given
        ChargingSessionEntity activeSession1 = new ChargingSessionEntity();
        activeSession1.setSessionId(UUID.randomUUID().toString());
        activeSession1.setUserId("USER-004");
        activeSession1.setChargeUnitId("UNIT-001");
        activeSession1.setStatus(ChargingSessionEntity.SessionStatus.ACTIVE);
        activeSession1.setStartTime(LocalDateTime.now());
        sessionRepository.save(activeSession1);

        ChargingSessionEntity initiatedSession = new ChargingSessionEntity();
        initiatedSession.setSessionId(UUID.randomUUID().toString());
        initiatedSession.setUserId("USER-005");
        initiatedSession.setChargeUnitId("UNIT-002");
        initiatedSession.setStatus(ChargingSessionEntity.SessionStatus.INITIATED);
        initiatedSession.setStartTime(LocalDateTime.now());
        sessionRepository.save(initiatedSession);

        ChargingSessionEntity completedSession = new ChargingSessionEntity();
        completedSession.setSessionId(UUID.randomUUID().toString());
        completedSession.setUserId("USER-006");
        completedSession.setChargeUnitId("UNIT-003");
        completedSession.setStatus(ChargingSessionEntity.SessionStatus.COMPLETED);
        completedSession.setStartTime(LocalDateTime.now().minusHours(2));
        completedSession.setEndTime(LocalDateTime.now().minusHours(1));
        sessionRepository.save(completedSession);

        // When
        List<ChargeSession> activeSessions = sessionService.getActiveSessions();

        // Then
        assertThat(activeSessions).hasSize(2);
        assertThat(activeSessions).allMatch(s ->
                s.getStatus().equals("ACTIVE") || s.getStatus().equals("INITIATED")
        );
    }

    @Test
    void updateSession_ShouldUpdateFields() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        ChargingSessionEntity entity = new ChargingSessionEntity();
        entity.setSessionId(sessionId);
        entity.setUserId("USER-007");
        entity.setChargeUnitId("UNIT-001");
        entity.setStatus(ChargingSessionEntity.SessionStatus.ACTIVE);
        entity.setStartTime(LocalDateTime.now().minusHours(1));
        sessionRepository.save(entity);

        // When - Update to completed
        ChargeSession updateDto = ChargeSession.builder()
                .status("COMPLETED")
                .endTime(LocalDateTime.now())
                .energyDelivered(45.5)
                .totalCost(new BigDecimal("20.48"))
                .build();

        Optional<ChargeSession> updatedSession = sessionService.updateSession(sessionId, updateDto);

        // Then
        assertThat(updatedSession).isPresent();
        assertThat(updatedSession.get().getStatus()).isEqualTo("COMPLETED");
        assertThat(updatedSession.get().getEndTime()).isNotNull();
        assertThat(updatedSession.get().getEnergyDelivered()).isEqualTo(45.5);
        assertThat(updatedSession.get().getTotalCost()).isEqualByComparingTo(new BigDecimal("20.48"));

        // Verify in database
        ChargingSessionEntity persistedEntity = sessionRepository.findById(sessionId).orElseThrow();
        assertThat(persistedEntity.getStatus()).isEqualTo(ChargingSessionEntity.SessionStatus.COMPLETED);
        assertThat(persistedEntity.getEndTime()).isNotNull();
    }

    @Test
    void updateSession_ShouldReturnEmpty_WhenNotExists() {
        // Given
        ChargeSession updateDto = ChargeSession.builder()
                .status("COMPLETED")
                .build();

        // When
        Optional<ChargeSession> result = sessionService.updateSession("NON-EXISTENT", updateDto);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void deleteSession_ShouldRemoveSession() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        ChargingSessionEntity entity = new ChargingSessionEntity();
        entity.setSessionId(sessionId);
        entity.setUserId("USER-008");
        entity.setChargeUnitId("UNIT-001");
        entity.setStatus(ChargingSessionEntity.SessionStatus.COMPLETED);
        entity.setStartTime(LocalDateTime.now().minusHours(2));
        entity.setEndTime(LocalDateTime.now().minusHours(1));
        sessionRepository.save(entity);

        // Verify it exists
        assertThat(sessionRepository.existsById(sessionId)).isTrue();

        // When
        boolean deleted = sessionService.deleteSession(sessionId);

        // Then
        assertThat(deleted).isTrue();
        assertThat(sessionRepository.existsById(sessionId)).isFalse();
    }

    @Test
    void deleteSession_ShouldReturnFalse_WhenNotExists() {
        // When
        boolean deleted = sessionService.deleteSession("NON-EXISTENT");

        // Then
        assertThat(deleted).isFalse();
    }

    @Test
    void getAllSessions_ShouldReturnAllSessions() {
        // Given
        ChargingSessionEntity session1 = new ChargingSessionEntity();
        session1.setSessionId(UUID.randomUUID().toString());
        session1.setUserId("USER-009");
        session1.setChargeUnitId("UNIT-001");
        session1.setStatus(ChargingSessionEntity.SessionStatus.ACTIVE);
        session1.setStartTime(LocalDateTime.now());
        sessionRepository.save(session1);

        ChargingSessionEntity session2 = new ChargingSessionEntity();
        session2.setSessionId(UUID.randomUUID().toString());
        session2.setUserId("USER-010");
        session2.setChargeUnitId("UNIT-002");
        session2.setStatus(ChargingSessionEntity.SessionStatus.COMPLETED);
        session2.setStartTime(LocalDateTime.now().minusHours(1));
        session2.setEndTime(LocalDateTime.now());
        sessionRepository.save(session2);

        // When
        List<ChargeSession> allSessions = sessionService.getAllSessions();

        // Then
        assertThat(allSessions).hasSize(2);
    }
}
