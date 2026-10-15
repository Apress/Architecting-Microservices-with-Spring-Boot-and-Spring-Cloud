package com.chargeroute.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private String id;
    private String email;
    private String displayName;
    private String membershipLevel;
    private BigDecimal accountBalance;
    private Integer totalSessions;
    private Double totalKwhConsumed;
    private LocalDateTime memberSince;
}
