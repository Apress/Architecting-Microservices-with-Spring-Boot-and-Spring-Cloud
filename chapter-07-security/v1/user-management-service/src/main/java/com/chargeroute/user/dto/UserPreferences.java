package com.chargeroute.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferences {
    private String preferredConnectorType;
    private Boolean notificationsEnabled;
    private String notificationChannel; // EMAIL, SMS, PUSH, ALL
    private Boolean autoChargeEnabled;
    private BigDecimal maxChargeAmount;
    private String preferredPaymentMethod;
}
