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

import java.time.LocalDateTime;

/**
 * User entity for R2DBC reactive persistence.
 * Note: R2DBC uses Spring Data relational annotations, not JPA.
 * <p>
 * Key differences from JPA:
 * - @Table from org.springframework.data.relational.core.mapping
 * - @Column from org.springframework.data.relational.core.mapping
 * - @Id from org.springframework.data.annotation
 * - No @EntityListeners - R2DBC handles auditing differently
 * - No @OneToOne - R2DBC doesn't support relationships (must handle manually)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class UserEntity {

    @Id
    @Column("user_id")
    private String userId;

    @Column("email")
    private String email;

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("phone_number")
    private String phoneNumber;

    // R2DBC stores enums as strings automatically
    @Column("status")
    private UserStatus status;

    @Column("membership_type")
    private String membershipType;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("last_login_at")
    private LocalDateTime lastLoginAt;

    @Version
    @Column("version")
    private Long version;

    /**
     * R2DBC doesn't support @OneToOne relationships.
     * Mark as @Transient and load separately in service layer if needed.
     */
    @Transient
    private UserPreferencesEntity preferences;

    public enum UserStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        PENDING_VERIFICATION
    }
}
