package com.chargeroute.user.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "chargeroute.user")
@Validated
public class UserProperties {

    @Min(value = 1, message = "Session timeout must be at least 1 minute")
    private int sessionTimeoutMinutes = 30;

    @Min(value = 1, message = "Max login attempts must be at least 1")
    private int maxLoginAttempts = 5;

    @Min(value = 1, message = "Account lockout duration must be at least 1 minute")
    private int accountLockoutMinutes = 15;

    @Valid
    @NotNull
    private Membership membership = new Membership();

    @Valid
    @NotNull
    private Notification notification = new Notification();

    @Valid
    @NotNull
    private Validation validation = new Validation();

    @Data
    public static class Membership {
        private boolean allowBasicUpgrade = true;
        private boolean allowPremiumDowngrade = true;
        private int premiumBenefitMultiplier = 2;
        private double basicMonthlyCost = 9.99;
        private double premiumMonthlyCost = 29.99;
    }

    @Data
    public static class Notification {
        private boolean sendWelcomeEmail = true;
        private boolean sendAccountUpdates = true;
        private boolean sendSecurityAlerts = true;
        private boolean sendMembershipChanges = true;
    }

    @Data
    public static class Validation {
        private boolean emailVerificationRequired = true;
        private boolean phoneVerificationRequired = true;
    }
}