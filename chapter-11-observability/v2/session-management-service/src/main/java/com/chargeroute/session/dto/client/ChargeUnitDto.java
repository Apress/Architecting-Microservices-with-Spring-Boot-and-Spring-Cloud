package com.chargeroute.session.dto.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for charge unit from charge-unit-management-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeUnitDto {
    private String chargeUnitId;
    private String stationName;
    private String address;
    private String connectorType;
    private Integer maxPowerKw;
    private String status;
    private BigDecimal pricePerKwh;
}
