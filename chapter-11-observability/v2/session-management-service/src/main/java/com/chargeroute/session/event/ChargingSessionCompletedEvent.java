package com.chargeroute.session.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Event published when a charging session completes successfully.
 * Triggers downstream actions:
 * - Charge unit service marks unit as AVAILABLE
 * - Notification service sends receipt to user
 * - Billing service processes payment
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ChargingSessionCompletedEvent extends BaseEvent {

    private String sessionId;
    private String userId;
    private String chargeUnitId;
    private BigDecimal totalEnergyKwh;
    private BigDecimal totalCost;
    private Integer durationMinutes;

    public ChargingSessionCompletedEvent(String correlationId, String sessionId,
                                          String userId, String chargeUnitId,
                                          BigDecimal totalEnergyKwh, BigDecimal totalCost,
                                          Integer durationMinutes) {
        super(correlationId, "ChargingSessionCompleted");
        this.sessionId = sessionId;
        this.userId = userId;
        this.chargeUnitId = chargeUnitId;
        this.totalEnergyKwh = totalEnergyKwh;
        this.totalCost = totalCost;
        this.durationMinutes = durationMinutes;
    }
}
