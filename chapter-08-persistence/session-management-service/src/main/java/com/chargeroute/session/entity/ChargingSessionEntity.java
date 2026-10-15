package com.chargeroute.session.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "charging_sessions", indexes = {
        @Index(name = "idx_session_user_id", columnList = "user_id"),
        @Index(name = "idx_session_charge_unit_id", columnList = "charge_unit_id"),
        @Index(name = "idx_session_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
public class ChargingSessionEntity {

    @Id
    @Column(name = "session_id", nullable = false, length = 50)
    private String sessionId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "charge_unit_id", nullable = false, length = 50)
    private String chargeUnitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "initial_battery_level")
    private Integer initialBatteryLevel;

    @Column(name = "final_battery_level")
    private Integer finalBatteryLevel;

    @Column(name = "energy_delivered_kwh", precision = 10, scale = 2)
    private BigDecimal energyDeliveredKwh;

    @Column(name = "total_cost", precision = 10, scale = 2)
    private BigDecimal totalCost;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SessionMetricEntity> metrics = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    public enum SessionStatus {
        INITIATED,
        ACTIVE,
        COMPLETED,
        FAILED
    }

    /**
     * Helper method to add a metric to the session
     */
    public void addMetric(SessionMetricEntity metric) {
        metrics.add(metric);
        metric.setSession(this);
    }

    /**
     * Helper method to remove a metric from the session
     */
    public void removeMetric(SessionMetricEntity metric) {
        metrics.remove(metric);
        metric.setSession(null);
    }
}
