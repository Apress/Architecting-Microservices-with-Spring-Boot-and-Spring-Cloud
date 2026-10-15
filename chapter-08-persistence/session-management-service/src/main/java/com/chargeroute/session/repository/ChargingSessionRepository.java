package com.chargeroute.session.repository;

import com.chargeroute.session.entity.ChargingSessionEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChargingSessionRepository extends JpaRepository<ChargingSessionEntity, String> {

    /**
     * Find all sessions for a specific user
     */
    List<ChargingSessionEntity> findByUserId(String userId);

    /**
     * Find all sessions for a specific charge unit
     */
    List<ChargingSessionEntity> findByChargeUnitId(String chargeUnitId);

    /**
     * Find all sessions with a specific status
     */
    List<ChargingSessionEntity> findByStatus(ChargingSessionEntity.SessionStatus status);

    /**
     * Find session by ID with metrics eagerly fetched
     * This avoids N+1 query problems when accessing metrics
     */
    @EntityGraph(attributePaths = {"metrics"})
    @Query("SELECT s FROM ChargingSessionEntity s WHERE s.sessionId = :sessionId")
    Optional<ChargingSessionEntity> findByIdWithMetrics(@Param("sessionId") String sessionId);

    /**
     * Find all sessions for a user with metrics eagerly fetched
     */
    @EntityGraph(attributePaths = {"metrics"})
    @Query("SELECT s FROM ChargingSessionEntity s WHERE s.userId = :userId")
    List<ChargingSessionEntity> findByUserIdWithMetrics(@Param("userId") String userId);

    /**
     * Find active sessions (INITIATED or ACTIVE status)
     */
    @Query("SELECT s FROM ChargingSessionEntity s WHERE s.status IN ('INITIATED', 'ACTIVE')")
    List<ChargingSessionEntity> findActiveSessions();
}
