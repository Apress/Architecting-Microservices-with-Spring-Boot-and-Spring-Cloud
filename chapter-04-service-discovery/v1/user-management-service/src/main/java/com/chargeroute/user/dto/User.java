package com.chargeroute.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String membershipLevel; // BASIC, SILVER, GOLD, PLATINUM
    private BigDecimal accountBalance;
    private LocalDateTime registeredAt;
    private LocalDateTime lastActiveAt;
    private Boolean emailVerified;
    private List<String> preferredLocationIds;
    private UserPreferences preferences;
}