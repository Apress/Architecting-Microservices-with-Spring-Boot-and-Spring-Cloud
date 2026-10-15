package com.chargeroute.chargeunit.observability;

import com.chargeroute.chargeunit.entity.ChargeUnitEntity;
import com.chargeroute.chargeunit.repository.ChargeUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Custom health indicator for Charge Unit Management Service.
 * Provides detailed health information about charge unit service capabilities.
 */
@Component("chargeUnitServiceHealth")
@RequiredArgsConstructor
@Slf4j
public class ChargeUnitHealthIndicator implements ReactiveHealthIndicator {

    private final ChargeUnitRepository chargeUnitRepository;
    private final ChargeUnitObservabilityMetrics chargeUnitMetrics;

    @Override
    public Mono<Health> health() {
        return checkChargeUnitServiceHealth()
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(ex -> {
                    log.error("Health check failed", ex);
                    return Mono.just(Health.down()
                            .withDetail("error", ex.getMessage())
                            .withDetail("errorType", ex.getClass().getSimpleName())
                            .build());
                });
    }

    private Mono<Health> checkChargeUnitServiceHealth() {
        return chargeUnitRepository.count()
                .flatMap(totalCount ->
                        chargeUnitRepository.findByStatus(ChargeUnitEntity.ChargeUnitStatus.AVAILABLE)
                                .count()
                                .map(availableCount -> {
                                    int available = availableCount.intValue();

                                    Health.Builder builder = Health.up()
                                            .withDetail("database", "UP")
                                            .withDetail("totalChargeUnits", totalCount)
                                            .withDetail("availableChargeUnits", available);

                                    // Update metrics
                                    chargeUnitMetrics.setAvailableChargeUnits(available);

                                    // Calculate and update utilization if there are units
                                    if (totalCount > 0) {
                                        int utilization = (int) (((totalCount - available) * 100.0) / totalCount);
                                        chargeUnitMetrics.setUtilizationPercentage(utilization);
                                        builder.withDetail("utilizationPercentage", utilization);
                                    }

                                    // Add warning if all units are unavailable
                                    if (totalCount > 0 && available == 0) {
                                        builder.withDetail("warning", "No charge units available");
                                    }

                                    // Add warning if utilization is very high
                                    if (totalCount > 0 && available < totalCount * 0.1) {
                                        builder.withDetail("warning", "High charge unit utilization (>90%)");
                                    }

                                    return builder.build();
                                })
                )
                .onErrorResume(ex -> {
                    log.error("Database health check failed", ex);
                    return Mono.just(Health.down()
                            .withDetail("database", "DOWN")
                            .withDetail("error", ex.getMessage())
                            .build());
                });
    }
}
