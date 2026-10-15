package com.chargeroute.notification.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    @NotNull
    private String userId;

    @NotNull
    private String type; // SESSION_STARTED, SESSION_COMPLETED, LOW_BALANCE, MAINTENANCE

    @NotNull
    private String channel; // EMAIL, SMS, PUSH, IN_APP

    private String subject;

    @NotNull
    private String message;

    private Map<String, Object> templateData;
    private NotificationPriority priority;
    private Boolean requiresAcknowledgment;
}