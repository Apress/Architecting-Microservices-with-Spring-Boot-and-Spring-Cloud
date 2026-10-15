package com.chargeroute.session.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SessionMetric entity for R2DBC reactive persistence.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("session_metrics")
public class SessionMetricEntity {

    @Id
    @Column("metric_id")
    private Long metricId;

    @Transient
    private ChargingSessionEntity session;

    @Column("recorded_at")
    private LocalDateTime recordedAt;

    @Column("current_power_kw")
    private BigDecimal currentPowerKw;

    @Column("voltage")
    private BigDecimal voltage;

    @Column("current_amps")
    private BigDecimal currentAmps;

    @Column("battery_level")
    private Integer batteryLevel;

    @Column("temperature_celsius")
    private BigDecimal temperatureCelsius;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;
}
