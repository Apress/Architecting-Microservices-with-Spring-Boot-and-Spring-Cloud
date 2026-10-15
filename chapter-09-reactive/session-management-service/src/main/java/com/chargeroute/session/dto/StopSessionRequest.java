package com.chargeroute.session.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StopSessionRequest {
    @NotNull
    private String sessionId;

    @NotNull
    private String userId;

    private String reason; // USER_REQUESTED, COMPLETED, EMERGENCY_STOP
}
