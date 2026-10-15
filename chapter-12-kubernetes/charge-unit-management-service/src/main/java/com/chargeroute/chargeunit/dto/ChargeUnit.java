package com.chargeroute.chargeunit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeUnit {
    private String chargeUnitId;
    private String stationName;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String connectorType; // CCS, CHAdeMO, Type2, Tesla
    private Integer maxPowerKw;
    private Integer currentPowerKw;
    private String status; // AVAILABLE, OCCUPIED, OFFLINE, MAINTENANCE
    private BigDecimal pricePerKwh;
    private LocalDate lastMaintenanceDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
