package com.chargeroute.user.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Event published when a new user registers on the ChargeRoute platform.
 * This event triggers downstream actions like:
 * - Creating default user preferences
 * - Sending welcome notification
 * - Initializing user analytics profile
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UserRegisteredEvent extends BaseEvent {

    private String userId;
    private String email;
    private String fullName;
    private String membershipTier;

    public UserRegisteredEvent(String correlationId, String userId, String email,
                                String fullName, String membershipTier) {
        super(correlationId, "UserRegistered");
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.membershipTier = membershipTier;
    }
}
