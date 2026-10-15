package com.chargeroute.session.dto.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeUnit {
    private String id;
    private String locationId;
    private String status;
    private String type;
    private Double power;
    private String connector;
    private BigDecimal pricePerKwh;
}