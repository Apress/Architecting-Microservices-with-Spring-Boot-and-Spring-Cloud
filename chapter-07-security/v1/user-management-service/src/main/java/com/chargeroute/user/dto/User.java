package com.chargeroute.user.dto;

import com.chargeroute.user.model.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private UserEntity.UserStatus status;
    private String membershipType;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    public enum UserStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        PENDING_VERIFICATION
    }
}