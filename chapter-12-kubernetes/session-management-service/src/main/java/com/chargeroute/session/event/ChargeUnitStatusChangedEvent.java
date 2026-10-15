package com.chargeroute.session.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Event received when a charge unit's status changes.
 * Session Service consumes this to track charge unit availability.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ChargeUnitStatusChangedEvent extends BaseEvent {

    private String chargeUnitId;
    private String previousStatus;
    private String newStatus;
    private BigDecimal currentPowerKw;
    private String location;

    public ChargeUnitStatusChangedEvent(String correlationId, String chargeUnitId,
                                         String previousStatus, String newStatus,
                                         BigDecimal currentPowerKw, String location) {
        super(correlationId, "ChargeUnitStatusChanged");
        this.chargeUnitId = chargeUnitId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.currentPowerKw = currentPowerKw;
        this.location = location;
    }
}
