package com.chargeroute.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkNotificationRequest {
    private List<String> userIds;
    private String type;
    private String channel;
    private String subject;
    private String message;
    private Map<String, Object> commonData;
    private NotificationPriority priority;
}