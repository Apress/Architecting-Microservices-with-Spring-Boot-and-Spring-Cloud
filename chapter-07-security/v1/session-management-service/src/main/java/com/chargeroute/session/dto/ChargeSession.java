package com.chargeroute.session.dto;

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
public class ChargeSession {
    private String id;
    private String userId;
    private String userEmail;
    private String chargeUnitId;
    private String locationId;
    private String status; // ACTIVE, COMPLETED, FAILED, CANCELLED
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double energyDelivered; // kWh
    private Double peakPower; // kW
    private BigDecimal totalCost;
    private String paymentMethodId;
    private SessionMetrics metrics;
}