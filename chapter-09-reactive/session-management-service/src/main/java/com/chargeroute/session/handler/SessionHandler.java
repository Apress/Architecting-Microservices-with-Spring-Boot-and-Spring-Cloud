package com.chargeroute.session.handler;

import com.chargeroute.session.dto.ChargeSession;
import com.chargeroute.session.dto.StartSessionRequest;
import com.chargeroute.session.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Reactive Handler for Session operations using WebFlux functional endpoints.
 */
@Slf4j
@Component
public class SessionHandler {

    private final SessionService sessionService;

    public SessionHandler(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * Create a new charging session.
     */
    public Mono<ServerResponse> createSession(ServerRequest request) {
        return request.bodyToMono(ChargeSession.class)
                .doOnNext(session -> log.info("Creating new charging session for user: {}", session.getUserId()))
                .flatMap(sessionService::createSession)
                .flatMap(session -> ServerResponse
                        .created(URI.create("/api/sessions/" + session.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(session))
                .onErrorResume(IllegalArgumentException.class, e ->
                        ServerResponse.badRequest().bodyValue(Map.of("error", e.getMessage()))
                );
    }

    /**
     * Get all sessions.
     */
    public Mono<ServerResponse> getAllSessions(ServerRequest request) {
        log.debug("Fetching all sessions");
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(sessionService.getAllSessions(), ChargeSession.class);
    }

    /**
     * Get a session by ID.
     */
    public Mono<ServerResponse> getSession(ServerRequest request) {
        String sessionId = request.pathVariable("sessionId");
        log.debug("Fetching session: {}", sessionId);

        return sessionService.getSession(sessionId)
                .flatMap(session -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(session))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    /**
     * Get sessions by user ID.
     */
    public Mono<ServerResponse> getSessionsByUserId(ServerRequest request) {
        String userId = request.pathVariable("userId");
        log.debug("Fetching sessions for user: {}", userId);

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(sessionService.getSessionsByUserId(userId), ChargeSession.class);
    }

    /**
     * Get all active sessions.
     */
    public Mono<ServerResponse> getActiveSessions(ServerRequest request) {
        log.debug("Fetching active sessions");
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(sessionService.getActiveSessions(), ChargeSession.class);
    }

    /**
     * Update an existing session.
     */
    public Mono<ServerResponse> updateSession(ServerRequest request) {
        String sessionId = request.pathVariable("sessionId");
        log.info("Updating session: {}", sessionId);

        return request.bodyToMono(ChargeSession.class)
                .flatMap(session -> sessionService.updateSession(sessionId, session))
                .flatMap(updated -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(updated))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    /**
     * Delete a session.
     */
    public Mono<ServerResponse> deleteSession(ServerRequest request) {
        String sessionId = request.pathVariable("sessionId");
        log.info("Deleting session: {}", sessionId);

        return sessionService.deleteSession(sessionId)
                .flatMap(deleted -> {
                    if (deleted) {
                        return ServerResponse.noContent().build();
                    }
                    return ServerResponse.notFound().build();
                });
    }

    /**
     * Health check endpoint.
     */
    public Mono<ServerResponse> healthCheck(ServerRequest request) {
        Map<String, String> health = Map.of(
                "service", "Session Management Service",
                "status", "UP",
                "timestamp", LocalDateTime.now().toString()
        );
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(health);
    }
}
