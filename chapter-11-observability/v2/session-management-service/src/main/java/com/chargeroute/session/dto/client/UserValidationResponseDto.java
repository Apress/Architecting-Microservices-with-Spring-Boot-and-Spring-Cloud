package com.chargeroute.session.dto.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user validation response from user-management-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserValidationResponseDto {
    private boolean valid;
    private String userId;
    private String membershipType;
    private String message;

    public static UserValidationResponseDto invalid(String message) {
        return UserValidationResponseDto.builder()
                .valid(false)
                .message(message)
                .build();
    }
}
