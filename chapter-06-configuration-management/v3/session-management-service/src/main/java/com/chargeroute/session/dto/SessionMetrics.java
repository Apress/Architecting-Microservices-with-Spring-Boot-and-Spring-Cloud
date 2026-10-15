package com.chargeroute.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionMetrics {
    private Double averagePower;
    private Double maxPower;
    private Integer chargingDurationMinutes;
    private Double efficiency; // Percentage
    private Map<String, Object> additionalMetrics;
}
