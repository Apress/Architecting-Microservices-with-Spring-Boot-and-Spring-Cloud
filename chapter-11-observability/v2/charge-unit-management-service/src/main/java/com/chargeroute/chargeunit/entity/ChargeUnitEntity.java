package com.chargeroute.chargeunit.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ChargeUnit entity for R2DBC reactive persistence.
 * <p>
 * Key differences from JPA:
 * - @Table from org.springframework.data.relational.core.mapping
 * - @Column from org.springframework.data.relational.core.mapping
 * - @Id from org.springframework.data.annotation
 * - No @EntityListeners - R2DBC handles auditing differently
 * - No @Index annotations - indexes defined in database schema/migrations
 * - R2DBC stores enums as strings automatically
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("charge_units")
public class ChargeUnitEntity {

    @Id
    @Column("charge_unit_id")
    private String chargeUnitId;

    @Column("station_name")
    private String stationName;

    @Column("address")
    private String address;

    @Column("latitude")
    private BigDecimal latitude;

    @Column("longitude")
    private BigDecimal longitude;

    // R2DBC stores enums as strings automatically
    @Column("connector_type")
    private ConnectorType connectorType;

    @Column("max_power_kw")
    private Integer maxPowerKw;

    @Column("current_power_kw")
    private Integer currentPowerKw;

    // R2DBC stores enums as strings automatically
    @Column("status")
    private ChargeUnitStatus status;

    @Column("price_per_kwh")
    private BigDecimal pricePerKwh;

    @Column("last_maintenance_date")
    private LocalDate lastMaintenanceDate;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column("version")
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
