package com.chargeroute.chargeunit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for real-time charge unit status updates.
 * Used for Server-Sent Events (SSE) streaming.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeUnitStatusUpdate {

    private String chargeUnitId;
    private String stationName;
    private String status;
    private Integer currentPowerKw;
    private Integer maxPowerKw;
    private LocalDateTime timestamp;
    private String message;

    /**
     * Calculate utilization percentage
     */
    public Double getUtilizationPercent() {
        if (currentPowerKw == null || maxPowerKw == null || maxPowerKw == 0) {
            return 0.0;
        }
        return (currentPowerKw.doubleValue() / maxPowerKw.doubleValue()) * 100.0;
    }
}
