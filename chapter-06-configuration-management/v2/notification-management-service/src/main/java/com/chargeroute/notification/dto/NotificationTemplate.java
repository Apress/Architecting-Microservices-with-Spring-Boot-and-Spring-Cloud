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
public class NotificationTemplate {
    private String templateId;
    private String name;
    private String type;
    private String subject;
    private String bodyTemplate;
    private List<String> requiredVariables;
    private Map<String, String> defaultValues;
}
