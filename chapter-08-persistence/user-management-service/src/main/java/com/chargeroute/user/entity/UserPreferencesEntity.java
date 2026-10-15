package com.chargeroute.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_preferences")
@EntityListeners(AuditingEntityListener.class)
public class UserPreferencesEntity {

    @Id
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "preferred_connector_type", length = 50)
    private String preferredConnectorType;

    @Column(name = "notifications_enabled")
    private Boolean notificationsEnabled;

    @Column(name = "notification_channel", length = 20)
    private String notificationChannel; // EMAIL, SMS, PUSH, ALL

    @Column(name = "auto_charge_enabled")
    private Boolean autoChargeEnabled;

    @Column(name = "max_charge_amount", precision = 10, scale = 2)
    private BigDecimal maxChargeAmount;

    @Column(name = "preferred_payment_method", length = 50)
    private String preferredPaymentMethod;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;
}
