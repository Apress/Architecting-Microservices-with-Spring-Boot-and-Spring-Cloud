package com.chargeroute.session.repository;

import com.chargeroute.session.entity.ChargingSessionEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Reactive repository for ChargingSession entities using R2DBC.
 */
@Repository
public interface ChargingSessionRepository extends R2dbcRepository<ChargingSessionEntity, String> {

    /**
     * Find all sessions for a specific user
     */
    Flux<ChargingSessionEntity> findByUserId(String userId);

    /**
     * Find all sessions for a specific charge unit
     */
    Flux<ChargingSessionEntity> findByChargeUnitId(String chargeUnitId);

    /**
     * Find all sessions with a specific status
     */
    Flux<ChargingSessionEntity> findByStatus(ChargingSessionEntity.SessionStatus status);
}
