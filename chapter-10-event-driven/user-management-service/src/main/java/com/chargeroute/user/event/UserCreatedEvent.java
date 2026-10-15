package com.chargeroute.user.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Internal Spring Application Event (NOT published to RabbitMQ).
 * Used for coordinating actions within the user-management-service only.
 * <p>
 * This demonstrates the difference between:
 * - Internal events (@EventListener) - same service, same bounded context
 * - External events (Spring Cloud Stream) - cross-service integration
 * <p>
 * When a user is created, this internal event triggers preference initialization
 * within the same service, while UserRegisteredEvent is published externally
 * for other services to consume.
 */
@Getter
public class UserCreatedEvent extends ApplicationEvent {

    private final String userId;
    private final String email;

    public UserCreatedEvent(Object source, String userId, String email) {
        super(source);
        this.userId = userId;
        this.email = email;
    }
}
