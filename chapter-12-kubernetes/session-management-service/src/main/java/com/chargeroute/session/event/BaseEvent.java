package com.chargeroute.session.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all events in the ChargeRoute platform.
 * Provides common fields for event tracking, correlation, and idempotency.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent {

    /**
     * Unique identifier for this event instance.
     * Used for idempotency - consumers can track processed event IDs.
     */
    private String eventId = UUID.randomUUID().toString();

    /**
     * Timestamp when the event occurred.
     */
    private LocalDateTime occurredAt = LocalDateTime.now();

    /**
     * Correlation ID for distributed tracing.
     * Links related events across service boundaries.
     */
    private String correlationId;

    /**
     * The type of event (e.g., "UserRegistered", "SessionStarted").
     * Useful for routing and filtering.
     */
    private String eventType;

    protected BaseEvent(String correlationId, String eventType) {
        this.correlationId = correlationId;
        this.eventType = eventType;
    }
}
