package com.chargeroute.chargeunit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "charge_units", indexes = {
        @Index(name = "idx_charge_unit_status", columnList = "status"),
        @Index(name = "idx_charge_unit_connector_type", columnList = "connector_type")
})
@EntityListeners(AuditingEntityListener.class)
public class ChargeUnitEntity {

    @Id
    @Column(name = "charge_unit_id", nullable = false, length = 50)
    private String chargeUnitId;

    @Column(name = "station_name", nullable = false, length = 200)
    private String stationName;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "connector_type", nullable = false, length = 20)
    private ConnectorType connectorType;

    @Column(name = "max_power_kw", nullable = false)
    private Integer maxPowerKw;

    @Column(name = "current_power_kw")
    private Integer currentPowerKw;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ChargeUnitStatus status;

    @Column(name = "price_per_kwh", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerKwh;

    @Column(name = "last_maintenance_date")
    private LocalDate lastMaintenanceDate;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    public enum ConnectorType {
        CCS,
        CHAdeMO,
        Type2,
        Tesla
    }

    public enum ChargeUnitStatus {
        AVAILABLE,
        OCCUPIED,
        OFFLINE,
        MAINTENANCE
    }
}
