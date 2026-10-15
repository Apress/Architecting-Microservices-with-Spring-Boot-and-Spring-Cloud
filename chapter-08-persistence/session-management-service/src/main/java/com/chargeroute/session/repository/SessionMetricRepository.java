package com.chargeroute.session.repository;

import com.chargeroute.session.entity.SessionMetricEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SessionMetricRepository extends JpaRepository<SessionMetricEntity, Long> {

    /**
     * Find all metrics for a specific session, ordered by recorded time descending
     */
    List<SessionMetricEntity> findBySession_SessionIdOrderByRecordedAtDesc(String sessionId);

    /**
     * Find metrics for a session within a time range
     */
    @Query("SELECT m FROM SessionMetricEntity m WHERE m.session.sessionId = :sessionId " +
            "AND m.recordedAt BETWEEN :startTime AND :endTime ORDER BY m.recordedAt ASC")
    List<SessionMetricEntity> findBySessionIdAndTimeRange(
            @Param("sessionId") String sessionId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Find the latest N metrics for a session
     */
    @Query("SELECT m FROM SessionMetricEntity m WHERE m.session.sessionId = :sessionId " +
            "ORDER BY m.recordedAt DESC")
    List<SessionMetricEntity> findLatestMetricsBySessionId(@Param("sessionId") String sessionId);
}
