package com.chargeroute.chargeunit.repository;

import com.chargeroute.chargeunit.entity.ChargeUnitEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;

/**
 * Reactive repository for ChargeUnit entities using R2DBC.
 * Returns Flux for multiple results.
 */
@Repository
public interface ChargeUnitRepository extends R2dbcRepository<ChargeUnitEntity, String> {

    /**
     * Find all charge units with a specific status.
     *
     * @return Flux<ChargeUnitEntity> - emits 0 to N charge units
     */
    Flux<ChargeUnitEntity> findByStatus(ChargeUnitEntity.ChargeUnitStatus status);

    /**
     * Find all charge units with a specific connector type.
     *
     * @return Flux<ChargeUnitEntity> - emits 0 to N charge units
     */
    Flux<ChargeUnitEntity> findByConnectorType(ChargeUnitEntity.ConnectorType connectorType);

    /**
     * Find charge units within a given radius from a location.
     * Using the Haversine formula to calculate distance.
     * <p>
     * Note: R2DBC uses SQL, not JPQL
     *
     * @param latitude  Center latitude
     * @param longitude Center longitude
     * @param radiusKm  Radius in kilometers
     * @return Flux<ChargeUnitEntity> - emits 0 to N charge units within the radius
     */
    @Query("SELECT * FROM charge_units WHERE " +
            "(6371 * acos(cos(radians(:latitude)) * cos(radians(latitude)) * " +
            "cos(radians(longitude) - radians(:longitude)) + sin(radians(:latitude)) * " +
            "sin(radians(latitude)))) <= :radiusKm")
    Flux<ChargeUnitEntity> findNearLocation(
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("radiusKm") double radiusKm
    );
}
