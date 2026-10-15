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
public class SessionSummary {
    private String sessionId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double energyDelivered;
    private BigDecimal totalCost;
    private String chargeUnitId;
    private String locationName;
}
