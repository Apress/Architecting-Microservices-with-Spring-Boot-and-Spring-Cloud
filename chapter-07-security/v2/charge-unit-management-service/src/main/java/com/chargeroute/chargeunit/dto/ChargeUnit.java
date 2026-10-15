package com.chargeroute.chargeunit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeUnit {
    private String id;
    private String locationId;
    private String status; // AVAILABLE, IN_USE, OUT_OF_SERVICE
    private String type; // AC_SLOW, DC_FAST, DC_ULTRA_FAST
    private Double power; // kW rating
    private String connector; // CCS, CHAdeMO, Type2
    private BigDecimal pricePerKwh;
    private LocalDateTime lastMaintenanceDate;
}
