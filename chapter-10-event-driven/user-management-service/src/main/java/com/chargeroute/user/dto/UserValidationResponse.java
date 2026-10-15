package com.chargeroute.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserValidationResponse {
    private boolean valid;
    private String userId;
    private String membershipType;
    private String message;

    public static UserValidationResponse valid(String userId, String membershipType) {
        return new UserValidationResponse(true, userId, membershipType, "User is valid and active");
    }

    public static UserValidationResponse invalid(String message) {
        return new UserValidationResponse(false, null, null, message);
    }
}