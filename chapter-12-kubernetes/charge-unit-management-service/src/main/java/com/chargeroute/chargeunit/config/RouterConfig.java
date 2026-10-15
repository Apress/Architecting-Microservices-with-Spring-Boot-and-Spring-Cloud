package com.chargeroute.chargeunit.config;

import com.chargeroute.chargeunit.handler.ChargeUnitHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;

/**
 * Functional Router Configuration for Charge Unit Management Service.
 * Maps HTTP routes to reactive handler functions.
 */
@Configuration
public class RouterConfig {

    @Bean
    public RouterFunction<ServerResponse> chargeUnitRoutes(ChargeUnitHandler handler) {
        return RouterFunctions.nest(path("/api/charge-units"),
                RouterFunctions.route()
                        // POST /api/charge-units - Create charge unit
                        .POST("", accept(MediaType.APPLICATION_JSON), handler::createChargeUnit)

                        // GET /api/charge-units - Get all charge units
                        .GET("", handler::getAllChargeUnits)

                        // GET /api/charge-units/available - Get available charge units
                        .GET("/available", handler::getAvailableChargeUnits)

                        // GET /api/charge-units/status/{status} - Get by status
                        .GET("/status/{status}", handler::getChargeUnitsByStatus)

                        // GET /api/charge-units/{chargeUnitId} - Get single charge unit
                        .GET("/{chargeUnitId}", handler::getChargeUnit)

                        // PUT /api/charge-units/{chargeUnitId} - Update charge unit
                        .PUT("/{chargeUnitId}", accept(MediaType.APPLICATION_JSON), handler::updateChargeUnit)

                        // DELETE /api/charge-units/{chargeUnitId} - Delete charge unit
                        .DELETE("/{chargeUnitId}", handler::deleteChargeUnit)

                        // GET /api/charge-units/admin/health - Health check
                        .GET("/admin/health", handler::healthCheck)

                        .build()
        );
    }
}
