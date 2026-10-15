package com.chargeroute.session.config;

import com.chargeroute.session.handler.SessionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;

/**
 * Functional Router Configuration for Session Management Service.
 * Maps HTTP routes to reactive handler functions.
 */
@Configuration
public class RouterConfig {

    @Bean
    public RouterFunction<ServerResponse> sessionRoutes(SessionHandler handler) {
        return RouterFunctions.nest(path("/api/sessions"),
                RouterFunctions.route()
                        // POST /api/sessions - Create session
                        .POST("", accept(MediaType.APPLICATION_JSON), handler::createSession)

                        // GET /api/sessions - Get all sessions
                        .GET("", handler::getAllSessions)

                        // GET /api/sessions/active - Get active sessions
                        .GET("/active", handler::getActiveSessions)

                        // GET /api/sessions/user/{userId} - Get sessions by user
                        .GET("/user/{userId}", handler::getSessionsByUserId)

                        // GET /api/sessions/{sessionId} - Get single session
                        .GET("/{sessionId}", handler::getSession)

                        // PUT /api/sessions/{sessionId} - Update session
                        .PUT("/{sessionId}", accept(MediaType.APPLICATION_JSON), handler::updateSession)

                        // DELETE /api/sessions/{sessionId} - Delete session
                        .DELETE("/{sessionId}", handler::deleteSession)

                        // GET /api/sessions/admin/health - Health check
                        .GET("/admin/health", handler::healthCheck)

                        .build()
        );
    }
}
