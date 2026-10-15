package com.chargeroute.user.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Event published when user information is updated.
 * Allows other services to maintain eventual consistency.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UserUpdatedEvent extends BaseEvent {

    private String userId;
    private String email;
    private String fullName;
    private String membershipTier;

    public UserUpdatedEvent(String correlationId, String userId, String email,
                             String fullName, String membershipTier) {
        super(correlationId, "UserUpdated");
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.membershipTier = membershipTier;
    }
}
