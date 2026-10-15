package com.chargeroute.session.mapper;

import com.chargeroute.session.dto.ChargeSession;
import com.chargeroute.session.dto.SessionMetric;
import com.chargeroute.session.entity.ChargingSessionEntity;
import com.chargeroute.session.entity.SessionMetricEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SessionMapper {

    /**
     * Map ChargingSessionEntity to ChargeSession DTO
     */
    @Mapping(target = "id", source = "sessionId")
    @Mapping(target = "status", expression = "java(entity.getStatus().name())")
    @Mapping(target = "energyDelivered", source = "energyDeliveredKwh")
    @Mapping(target = "userEmail", ignore = true)
    @Mapping(target = "locationId", ignore = true)
    @Mapping(target = "peakPower", ignore = true)
    @Mapping(target = "paymentMethodId", ignore = true)
    @Mapping(target = "metrics", ignore = true)
    ChargeSession toDto(ChargingSessionEntity entity);

    /**
     * Map ChargeSession DTO to ChargingSessionEntity
     */
    @Mapping(target = "sessionId", source = "id")
    @Mapping(target = "status", expression = "java(parseStatus(dto.getStatus()))")
    @Mapping(target = "energyDeliveredKwh", source = "energyDelivered")
    @Mapping(target = "metrics", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ChargingSessionEntity toEntity(ChargeSession dto);

    /**
     * Update ChargingSessionEntity from ChargeSession DTO
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "sessionId", ignore = true)
    @Mapping(target = "status", expression = "java(dto.getStatus() != null ? parseStatus(dto.getStatus()) : entity.getStatus())")
    @Mapping(target = "energyDeliveredKwh", source = "energyDelivered")
    @Mapping(target = "metrics", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(ChargeSession dto, @MappingTarget ChargingSessionEntity entity);

    /**
     * Map SessionMetricEntity to SessionMetric DTO
     */
    @Mapping(target = "sessionId", source = "session.sessionId")
    SessionMetric toMetricDto(SessionMetricEntity entity);

    /**
     * Map SessionMetric DTO to SessionMetricEntity
     */
    @Mapping(target = "session", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    SessionMetricEntity toMetricEntity(SessionMetric dto);

    /**
     * Map list of entities to list of DTOs
     */
    List<ChargeSession> toDtoList(List<ChargingSessionEntity> entities);

    /**
     * Map list of metric entities to list of metric DTOs
     */
    List<SessionMetric> toMetricDtoList(List<SessionMetricEntity> entities);

    /**
     * Helper method to parse status string to enum
     */
    default ChargingSessionEntity.SessionStatus parseStatus(String status) {
        if (status == null) {
            return null;
        }
        try {
            return ChargingSessionEntity.SessionStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ChargingSessionEntity.SessionStatus.INITIATED; // default
        }
    }
}
