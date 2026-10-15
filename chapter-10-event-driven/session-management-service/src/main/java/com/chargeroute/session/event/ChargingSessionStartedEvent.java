package com.chargeroute.session.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Event published when a charging session successfully starts.
 * Triggers downstream actions:
 * - Charge unit service marks unit as OCCUPIED
 * - Notification service sends confirmation to user
 * - Analytics service records session start
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ChargingSessionStartedEvent extends BaseEvent {

    private String sessionId;
    private String userId;
    private String chargeUnitId;
    private BigDecimal initialEnergyKwh;

    public ChargingSessionStartedEvent(String correlationId, String sessionId,
                                       String userId, String chargeUnitId,
                                       BigDecimal initialEnergyKwh) {
        super(correlationId, "ChargingSessionStarted");
        this.sessionId = sessionId;
        this.userId = userId;
        this.chargeUnitId = chargeUnitId;
        this.initialEnergyKwh = initialEnergyKwh;
    }
}
