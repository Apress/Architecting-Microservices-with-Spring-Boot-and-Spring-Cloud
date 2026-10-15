package com.chargeroute.session.service;

import com.chargeroute.session.dto.ChargeSession;
import com.chargeroute.session.entity.ChargingSessionEntity;
import com.chargeroute.session.mapper.SessionMapper;
import com.chargeroute.session.repository.ChargingSessionRepository;
import com.chargeroute.session.repository.SessionMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service layer for managing charging sessions
 * Handles business logic and coordinates between controllers and repositories
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionService {

    private final ChargingSessionRepository sessionRepository;
    private final SessionMetricRepository metricRepository;
    private final SessionMapper sessionMapper;

    /**
     * Create a new charging session
     *
     * @param session the session DTO to create
     * @return the created session DTO with generated ID
     */
    @Transactional
    public ChargeSession createSession(ChargeSession session) {
        log.info("Creating new charging session for user: {}", session.getUserId());

        ChargingSessionEntity entity = sessionMapper.toEntity(session);

        // Generate UUID for session_id if not provided
        if (entity.getSessionId() == null) {
            entity.setSessionId(UUID.randomUUID().toString());
        }

        // Set default status if not provided
        if (entity.getStatus() == null) {
            entity.setStatus(ChargingSessionEntity.SessionStatus.INITIATED);
        }

        ChargingSessionEntity savedEntity = sessionRepository.save(entity);

        log.info("Created charging session with ID: {}", savedEntity.getSessionId());
        return sessionMapper.toDto(savedEntity);
    }

    /**
     * Get a session by ID
     *
     * @param sessionId the session ID
     * @return Optional containing the session DTO if found
     */
    public Optional<ChargeSession> getSession(String sessionId) {
        log.debug("Fetching session with ID: {}", sessionId);
        return sessionRepository.findById(sessionId)
                .map(sessionMapper::toDto);
    }

    /**
     * Get all sessions
     *
     * @return list of all sessions
     */
    public List<ChargeSession> getAllSessions() {
        log.debug("Fetching all sessions");
        return sessionMapper.toDtoList(sessionRepository.findAll());
    }

    /**
     * Get all sessions for a specific user
     *
     * @param userId the user ID
     * @return list of sessions for the user
     */
    public List<ChargeSession> getSessionsByUserId(String userId) {
        log.debug("Fetching sessions for user: {}", userId);
        return sessionMapper.toDtoList(sessionRepository.findByUserId(userId));
    }

    /**
     * Get all sessions for a specific charge unit
     *
     * @param chargeUnitId the charge unit ID
     * @return list of sessions for the charge unit
     */
    public List<ChargeSession> getSessionsByChargeUnitId(String chargeUnitId) {
        log.debug("Fetching sessions for charge unit: {}", chargeUnitId);
        return sessionMapper.toDtoList(sessionRepository.findByChargeUnitId(chargeUnitId));
    }

    /**
     * Get all active sessions (INITIATED or ACTIVE status)
     *
     * @return list of active sessions
     */
    public List<ChargeSession> getActiveSessions() {
        log.debug("Fetching all active sessions");
        return sessionMapper.toDtoList(sessionRepository.findActiveSessions());
    }

    /**
     * Update an existing session
     *
     * @param sessionId  the session ID to update
     * @param sessionDto the updated session data
     * @return Optional containing the updated session DTO if found
     */
    @Transactional
    public Optional<ChargeSession> updateSession(String sessionId, ChargeSession sessionDto) {
        log.info("Updating session: {}", sessionId);

        return sessionRepository.findById(sessionId)
                .map(existingEntity -> {
                    sessionMapper.updateEntityFromDto(sessionDto, existingEntity);
                    ChargingSessionEntity updatedEntity = sessionRepository.save(existingEntity);
                    log.info("Successfully updated session: {}", sessionId);
                    return sessionMapper.toDto(updatedEntity);
                });
    }

    /**
     * Delete a session by ID
     *
     * @param sessionId the session ID to delete
     * @return true if the session was deleted, false if not found
     */
    @Transactional
    public boolean deleteSession(String sessionId) {
        log.info("Deleting session: {}", sessionId);

        if (sessionRepository.existsById(sessionId)) {
            sessionRepository.deleteById(sessionId);
            log.info("Successfully deleted session: {}", sessionId);
            return true;
        }

        log.warn("Session not found for deletion: {}", sessionId);
        return false;
    }

    /**
     * Get session with metrics eagerly loaded
     *
     * @param sessionId the session ID
     * @return Optional containing the session with metrics
     */
    public Optional<ChargeSession> getSessionWithMetrics(String sessionId) {
        log.debug("Fetching session with metrics: {}", sessionId);
        return sessionRepository.findByIdWithMetrics(sessionId)
                .map(sessionMapper::toDto);
    }

    /**
     * Get all sessions for a user with metrics eagerly loaded
     *
     * @param userId the user ID
     * @return list of sessions with metrics
     */
    public List<ChargeSession> getSessionsByUserIdWithMetrics(String userId) {
        log.debug("Fetching sessions with metrics for user: {}", userId);
        return sessionMapper.toDtoList(sessionRepository.findByUserIdWithMetrics(userId));
    }
}
