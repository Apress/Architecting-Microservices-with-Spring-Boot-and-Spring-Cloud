package com.chargeroute.session.dto;

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
public class SessionMetric {
    private Long metricId;
    private String sessionId;
    private LocalDateTime recordedAt;
    private BigDecimal currentPowerKw;
    private BigDecimal voltage;
    private BigDecimal currentAmps;
    private Integer batteryLevel;
    private BigDecimal temperatureCelsius;
}
