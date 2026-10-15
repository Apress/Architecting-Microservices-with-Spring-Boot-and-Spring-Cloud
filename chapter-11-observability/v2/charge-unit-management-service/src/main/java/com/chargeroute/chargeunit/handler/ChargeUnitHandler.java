package com.chargeroute.chargeunit.handler;

import com.chargeroute.chargeunit.dto.ChargeUnit;
import com.chargeroute.chargeunit.dto.ChargeUnitRegistrationRequest;
import com.chargeroute.chargeunit.entity.ChargeUnitEntity;
import com.chargeroute.chargeunit.service.ChargeUnitService;
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
 * Reactive Handler for Charge Unit operations using WebFlux functional endpoints.
 */
@Slf4j
@Component
public class ChargeUnitHandler {

    private final ChargeUnitService chargeUnitService;

    public ChargeUnitHandler(ChargeUnitService chargeUnitService) {
        this.chargeUnitService = chargeUnitService;
    }

    /**
     * Create a new charge unit.
     */
    public Mono<ServerResponse> createChargeUnit(ServerRequest request) {
        return request.bodyToMono(ChargeUnitRegistrationRequest.class)
                .doOnNext(req -> log.info("Creating new charge unit at station: {}", req.getStationName()))
                .flatMap(chargeUnitService::createChargeUnit)
                .flatMap(chargeUnit -> ServerResponse
                        .created(URI.create("/api/charge-units/" + chargeUnit.getChargeUnitId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(chargeUnit))
                .onErrorResume(IllegalArgumentException.class, e ->
                        ServerResponse.badRequest().bodyValue(Map.of("error", e.getMessage()))
                );
    }

    /**
     * Get all charge units.
     */
    public Mono<ServerResponse> getAllChargeUnits(ServerRequest request) {
        log.debug("Fetching all charge units");
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(chargeUnitService.getAllChargeUnits(), ChargeUnit.class);
    }

    /**
     * Get a charge unit by ID.
     */
    public Mono<ServerResponse> getChargeUnit(ServerRequest request) {
        String chargeUnitId = request.pathVariable("chargeUnitId");
        log.debug("Fetching charge unit: {}", chargeUnitId);

        return chargeUnitService.getChargeUnit(chargeUnitId)
                .flatMap(chargeUnit -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(chargeUnit))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    /**
     * Update a charge unit.
     */
    public Mono<ServerResponse> updateChargeUnit(ServerRequest request) {
        String chargeUnitId = request.pathVariable("chargeUnitId");
        log.info("Updating charge unit: {}", chargeUnitId);

        return request.bodyToMono(ChargeUnit.class)
                .flatMap(chargeUnit -> chargeUnitService.updateChargeUnit(chargeUnitId, chargeUnit))
                .flatMap(updated -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(updated))
                .onErrorResume(IllegalArgumentException.class, e ->
                        ServerResponse.notFound().build()
                );
    }

    /**
     * Delete a charge unit.
     */
    public Mono<ServerResponse> deleteChargeUnit(ServerRequest request) {
        String chargeUnitId = request.pathVariable("chargeUnitId");
        log.info("Deleting charge unit: {}", chargeUnitId);

        return chargeUnitService.deleteChargeUnit(chargeUnitId)
                .then(ServerResponse.noContent().build())
                .onErrorResume(IllegalArgumentException.class, e ->
                        ServerResponse.notFound().build()
                );
    }

    /**
     * Get available charge units.
     */
    public Mono<ServerResponse> getAvailableChargeUnits(ServerRequest request) {
        log.debug("Fetching available charge units");
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(chargeUnitService.getAvailableChargeUnits(), ChargeUnit.class);
    }

    /**
     * Get charge units by status.
     */
    public Mono<ServerResponse> getChargeUnitsByStatus(ServerRequest request) {
        String status = request.pathVariable("status");
        log.debug("Fetching charge units with status: {}", status);

        try {
            ChargeUnitEntity.ChargeUnitStatus chargeUnitStatus =
                    ChargeUnitEntity.ChargeUnitStatus.valueOf(status.toUpperCase());
            return ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(chargeUnitService.getChargeUnitsByStatus(chargeUnitStatus), ChargeUnit.class);
        } catch (IllegalArgumentException e) {
            return ServerResponse.badRequest()
                    .bodyValue(Map.of("error", "Invalid status: " + status));
        }
    }

    /**
     * Health check endpoint.
     */
    public Mono<ServerResponse> healthCheck(ServerRequest request) {
        Map<String, String> health = Map.of(
                "service", "Charge Unit Management Service",
                "status", "UP",
                "timestamp", LocalDateTime.now().toString()
        );
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(health);
    }
}
