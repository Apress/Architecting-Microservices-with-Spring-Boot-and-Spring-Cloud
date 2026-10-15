package com.chargeroute.chargeunit.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeUnitRegistrationRequest {

    @NotBlank(message = "Station name is required")
    @Size(max = 200, message = "Station name must not exceed 200 characters")
    private String stationName;

    @NotBlank(message = "Address is required")
    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private BigDecimal latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private BigDecimal longitude;

    @NotBlank(message = "Connector type is required")
    private String connectorType;

    @NotNull(message = "Max power is required")
    @Min(value = 1, message = "Max power must be at least 1 kW")
    @Max(value = 500, message = "Max power must not exceed 500 kW")
    private Integer maxPowerKw;

    @NotNull(message = "Price per kWh is required")
    @DecimalMin(value = "0.01", message = "Price per kWh must be at least 0.01")
    private BigDecimal pricePerKwh;

    private LocalDate lastMaintenanceDate;
}
