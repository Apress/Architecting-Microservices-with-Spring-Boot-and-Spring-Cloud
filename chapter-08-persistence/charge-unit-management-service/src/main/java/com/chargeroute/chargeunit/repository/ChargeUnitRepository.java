package com.chargeroute.chargeunit.repository;

import com.chargeroute.chargeunit.entity.ChargeUnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ChargeUnitRepository extends JpaRepository<ChargeUnitEntity, String> {

    /**
     * Find all charge units with a specific status
     */
    List<ChargeUnitEntity> findByStatus(ChargeUnitEntity.ChargeUnitStatus status);

    /**
     * Find all charge units with a specific connector type
     */
    List<ChargeUnitEntity> findByConnectorType(ChargeUnitEntity.ConnectorType connectorType);

    /**
     * Find charge units within a given radius from a location
     * Using the Haversine formula to calculate distance
     *
     * @param latitude  Center latitude
     * @param longitude Center longitude
     * @param radiusKm  Radius in kilometers
     * @return List of charge units within the radius
     */
    @Query("SELECT c FROM ChargeUnitEntity c WHERE " +
            "(6371 * acos(cos(radians(:latitude)) * cos(radians(c.latitude)) * " +
            "cos(radians(c.longitude) - radians(:longitude)) + sin(radians(:latitude)) * " +
            "sin(radians(c.latitude)))) <= :radiusKm")
    List<ChargeUnitEntity> findNearLocation(
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("radiusKm") double radiusKm
    );
}
