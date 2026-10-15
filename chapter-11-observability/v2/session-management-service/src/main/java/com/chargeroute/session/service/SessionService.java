package com.chargeroute.session.service;

import com.chargeroute.session.dto.ChargeSession;
import com.chargeroute.session.entity.ChargingSessionEntity;
import com.chargeroute.session.mapper.SessionMapper;
import com.chargeroute.session.observability.SessionObservabilityMetrics;
import com.chargeroute.session.repository.ChargingSessionRepository;
import com.chargeroute.session.repository.SessionMetricRepository;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.annotation.NewSpan;
import io.micrometer.tracing.annotation.SpanTag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

/**
 * Reactive service layer for managing charging sessions.
 * Converted from blocking JPA to reactive R2DBC.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SessionService {

    private final ChargingSessionRepository sessionRepository;
    private final SessionMetricRepository metricRepository;
    private final SessionMapper sessionMapper;
    private final SessionObservabilityMetrics observabilityMetrics;

    /**
     * Create a new charging session
     *
     * @param session the session DTO to create
     * @return Mono of created session DTO with generated ID
     */
    @NewSpan("session-create")
    @Observed(name = "session.create", contextualName = "create-charging-session")
    public Mono<ChargeSession> createSession(@SpanTag("user.id") ChargeSession session) {
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

        return sessionRepository.save(entity)
                .doOnNext(saved -> {
                    log.info("Created charging session with ID: {}", saved.getSessionId());
                    observabilityMetrics.recordSessionStarted();
                })
                .doOnError(error -> {
                    log.error("Failed to create charging session", error);
                    observabilityMetrics.recordSessionFailed();
                })
                .map(sessionMapper::toDto);
    }

    /**
     * Get a session by ID
     *
     * @param sessionId the session ID
     * @return Mono containing the session DTO if found
     */
    public Mono<ChargeSession> getSession(String sessionId) {
        log.debug("Fetching session with ID: {}", sessionId);
        return sessionRepository.findById(sessionId)
                .map(sessionMapper::toDto);
    }

    /**
     * Get all sessions
     *
     * @return Flux of all sessions
     */
    public Flux<ChargeSession> getAllSessions() {
        log.debug("Fetching all sessions");
        return sessionRepository.findAll()
                .map(sessionMapper::toDto);
    }

    /**
     * Get all sessions for a specific user
     *
     * @param userId the user ID
     * @return Flux of sessions for the user
     */
    public Flux<ChargeSession> getSessionsByUserId(String userId) {
        log.debug("Fetching sessions for user: {}", userId);
        return sessionRepository.findByUserId(userId)
                .map(sessionMapper::toDto);
    }

    /**
     * Get all sessions for a specific charge unit
     *
     * @param chargeUnitId the charge unit ID
     * @return Flux of sessions for the charge unit
     */
    public Flux<ChargeSession> getSessionsByChargeUnitId(String chargeUnitId) {
        log.debug("Fetching sessions for charge unit: {}", chargeUnitId);
        return sessionRepository.findByChargeUnitId(chargeUnitId)
                .map(sessionMapper::toDto);
    }

    /**
     * Get all active sessions (INITIATED or ACTIVE status)
     *
     * @return Flux of active sessions
     */
    public Flux<ChargeSession> getActiveSessions() {
        log.debug("Fetching all active sessions");
        return sessionRepository.findByStatus(ChargingSessionEntity.SessionStatus.INITIATED)
                .concatWith(sessionRepository.findByStatus(ChargingSessionEntity.SessionStatus.ACTIVE))
                .map(sessionMapper::toDto);
    }

    /**
     * Update an existing session
     *
     * @param sessionId  the session ID to update
     * @param sessionDto the updated session data
     * @return Mono containing the updated session DTO if found
     */
    @NewSpan("session-update")
    @Observed(name = "session.update", contextualName = "update-charging-session")
    public Mono<ChargeSession> updateSession(@SpanTag("session.id") String sessionId, ChargeSession sessionDto) {
        log.info("Updating session: {}", sessionId);

        return sessionRepository.findById(sessionId)
                .flatMap(existingEntity -> {
                    // Check if session is being completed
                    boolean isCompleting = existingEntity.getStatus() != ChargingSessionEntity.SessionStatus.COMPLETED
                            && sessionDto.getStatus() != null
                            && "COMPLETED".equals(sessionDto.getStatus());

                    sessionMapper.updateEntityFromDto(sessionDto, existingEntity);
                    return sessionRepository.save(existingEntity)
                            .doOnNext(updated -> {
                                if (isCompleting && existingEntity.getStartTime() != null && existingEntity.getEndTime() != null) {
                                    Duration duration = Duration.between(
                                            existingEntity.getStartTime(),
                                            existingEntity.getEndTime()
                                    );
                                    observabilityMetrics.recordSessionCompleted(duration);
                                    log.info("Session completed: {}, duration: {} minutes", sessionId, duration.toMinutes());
                                }
                            });
                })
                .doOnNext(updated -> log.info("Successfully updated session: {}", sessionId))
                .map(sessionMapper::toDto);
    }

    /**
     * Delete a session by ID
     *
     * @param sessionId the session ID to delete
     * @return Mono<Boolean> - true if deleted, false if not found
     */
    public Mono<Boolean> deleteSession(String sessionId) {
        log.info("Deleting session: {}", sessionId);

        return sessionRepository.existsById(sessionId)
                .flatMap(exists -> {
                    if (exists) {
                        return sessionRepository.deleteById(sessionId)
                                .doOnSuccess(v -> log.info("Successfully deleted session: {}", sessionId))
                                .thenReturn(true);
                    } else {
                        log.warn("Session not found for deletion: {}", sessionId);
                        return Mono.just(false);
                    }
                });
    }
}
