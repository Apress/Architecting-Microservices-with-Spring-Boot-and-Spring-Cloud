package com.chargeroute.session.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ChargingSession entity for R2DBC reactive persistence.
 * Converted from JPA to R2DBC.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("charging_sessions")
public class ChargingSessionEntity {

    @Id
    @Column("session_id")
    private String sessionId;

    @Column("user_id")
    private String userId;

    @Column("charge_unit_id")
    private String chargeUnitId;

    // R2DBC stores enums as strings automatically
    @Column("status")
    private SessionStatus status;

    @Column("start_time")
    private LocalDateTime startTime;

    @Column("end_time")
    private LocalDateTime endTime;

    @Column("initial_battery_level")
    private Integer initialBatteryLevel;

    @Column("final_battery_level")
    private Integer finalBatteryLevel;

    @Column("energy_delivered_kwh")
    private BigDecimal energyDeliveredKwh;

    @Column("total_cost")
    private BigDecimal totalCost;

    // R2DBC doesn't support @OneToMany - mark as @Transient
    @Transient
    private List<SessionMetricEntity> metrics = new ArrayList<>();

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column("version")
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
