package com.chargeroute.session.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartSessionRequest {
    @NotNull
    private String userId;

    @NotNull
    private String chargeUnitId;

    private String paymentMethodId;
    private Double targetKwh; // Optional target energy
    private BigDecimal maxCost; // Optional cost limit
}
