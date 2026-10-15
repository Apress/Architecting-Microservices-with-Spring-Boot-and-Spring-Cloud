package com.chargeroute.session.consumer;

import com.chargeroute.session.event.ChargeUnitStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Consumer for ChargeUnitStatusChangedEvent.
 * <p>
 * This demonstrates the BROADCAST pattern:
 * - Charge Unit Service publishes status changes to a topic exchange
 * - Session Service consumes these events to track availability
 * - Notification Service ALSO consumes the same events (different queue)
 * - Each consumer processes independently without knowledge of others
 * <p>
 * Use Case: Session Service needs to know which charge units are available
 * when users want to start a new charging session.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChargeUnitEventConsumer {

    /**
     * Handle charge unit status changes.
     * <p>
     * In a real implementation, this would:
     * - Update internal cache of available charge units
     * - Notify waiting sessions when a unit becomes available
     * - Update session records if active session's unit goes offline
     */
    @Bean
    public Consumer<ChargeUnitStatusChangedEvent> handleChargeUnitStatusChanged() {
        return event -> {
            log.info("Received ChargeUnitStatusChangedEvent: unit={}, previousStatus={}, newStatus={}",
                    event.getChargeUnitId(), event.getPreviousStatus(), event.getNewStatus());

            // Log the status transition for demonstration
            if ("AVAILABLE".equals(event.getNewStatus())) {
                log.info("Charge unit {} is now AVAILABLE at location: {}",
                        event.getChargeUnitId(), event.getLocation());
                // In real app: Update availability cache, notify waiting sessions
            } else if ("OCCUPIED".equals(event.getNewStatus())) {
                log.info("Charge unit {} is now OCCUPIED", event.getChargeUnitId());
                // In real app: Mark as unavailable in cache
            } else if ("OFFLINE".equals(event.getNewStatus())) {
                log.warn("Charge unit {} went OFFLINE - may need to handle active sessions",
                        event.getChargeUnitId());
                // In real app: Check for active sessions and handle gracefully
            }

            log.debug("Processed ChargeUnitStatusChangedEvent for correlation: {}",
                    event.getCorrelationId());
        };
    }
}
