package com.chargeroute.notification.dto.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferences {
    private Boolean notificationsEnabled;
    private String notificationChannel; // EMAIL, SMS, PUSH, ALL
}