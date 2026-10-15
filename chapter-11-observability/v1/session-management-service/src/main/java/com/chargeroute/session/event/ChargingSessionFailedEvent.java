package com.chargeroute.session.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Event published when a charging session fails.
 * This is a compensation event that triggers rollback actions:
 * - Charge unit service releases unit reservation
 * - Notification service sends failure alert to user
 * - Any reserved resources are released
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ChargingSessionFailedEvent extends BaseEvent {

    private String sessionId;
    private String userId;
    private String chargeUnitId;
    private String failureReason;

    public ChargingSessionFailedEvent(String correlationId, String sessionId,
                                       String userId, String chargeUnitId,
                                       String failureReason) {
        super(correlationId, "ChargingSessionFailed");
        this.sessionId = sessionId;
        this.userId = userId;
        this.chargeUnitId = chargeUnitId;
        this.failureReason = failureReason;
    }
}
