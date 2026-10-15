package com.chargeroute.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Notification
 * Used for transferring notification data between layers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private String notificationId;
    private String userId;
    private String type;  // SESSION_STARTED, SESSION_COMPLETED, PAYMENT_RECEIVED, etc.
    private String channel;  // EMAIL, SMS, PUSH, IN_APP
    private String subject;
    private String message;
    private String status;  // PENDING, SENT, FAILED, RETRYING
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
