package com.chargeroute.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private String notificationId;
    private String status; // SENT, QUEUED, FAILED, DELIVERED
    private LocalDateTime sentAt;
    private String channel;
    private String errorMessage;
}
