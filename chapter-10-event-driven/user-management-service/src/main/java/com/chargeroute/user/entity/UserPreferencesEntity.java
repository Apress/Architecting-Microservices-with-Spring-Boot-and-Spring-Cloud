package com.chargeroute.user.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * UserPreferences entity for R2DBC reactive persistence.
 * Uses the same user_id as primary key (shared with UserEntity).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("user_preferences")
public class UserPreferencesEntity {

    @Id
    @Column("user_id")
    private String userId;

    /**
     * R2DBC doesn't support @OneToOne relationships.
     * Mark as @Transient - this is the foreign key relationship.
     */
    @Transient
    private UserEntity user;

    @Column("preferred_connector_type")
    private String preferredConnectorType;

    @Column("notifications_enabled")
    private Boolean notificationsEnabled;

    @Column("notification_channel")
    private String notificationChannel; // EMAIL, SMS, PUSH, ALL

    @Column("auto_charge_enabled")
    private Boolean autoChargeEnabled;

    @Column("max_charge_amount")
    private BigDecimal maxChargeAmount;

    @Column("preferred_payment_method")
    private String preferredPaymentMethod;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column("version")
    private Long version;
}
