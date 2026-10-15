package com.chargeroute.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event received when a new user registers (published by user-management-service).
 * Notification service consumes this to send welcome email.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {
    private String eventId;
    private LocalDateTime occurredAt;
    private String correlationId;
    private String eventType;
    private String userId;
    private String email;
    private String fullName;
    private String membershipTier;
}
