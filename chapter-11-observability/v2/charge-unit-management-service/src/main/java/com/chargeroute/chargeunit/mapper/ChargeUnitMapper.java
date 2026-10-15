package com.chargeroute.chargeunit.mapper;

import com.chargeroute.chargeunit.dto.ChargeUnit;
import com.chargeroute.chargeunit.dto.ChargeUnitRegistrationRequest;
import com.chargeroute.chargeunit.entity.ChargeUnitEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChargeUnitMapper {

    /**
     * Map ChargeUnitEntity to ChargeUnit DTO
     */
    @Mapping(target = "connectorType", expression = "java(entity.getConnectorType().name())")
    @Mapping(target = "status", expression = "java(entity.getStatus().name())")
    ChargeUnit toDto(ChargeUnitEntity entity);

    /**
     * Map ChargeUnit DTO to ChargeUnitEntity
     */
    @Mapping(target = "connectorType", expression = "java(parseConnectorType(dto.getConnectorType()))")
    @Mapping(target = "status", expression = "java(parseStatus(dto.getStatus()))")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ChargeUnitEntity toEntity(ChargeUnit dto);

    /**
     * Map ChargeUnitRegistrationRequest to ChargeUnitEntity
     */
    @Mapping(target = "chargeUnitId", ignore = true)
    @Mapping(target = "connectorType", expression = "java(parseConnectorType(request.getConnectorType()))")
    @Mapping(target = "status", constant = "OFFLINE")
    @Mapping(target = "currentPowerKw", constant = "0")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ChargeUnitEntity toEntity(ChargeUnitRegistrationRequest request);

    /**
     * Update ChargeUnitEntity from ChargeUnit DTO
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "chargeUnitId", ignore = true)
    @Mapping(target = "connectorType", expression = "java(dto.getConnectorType() != null ? parseConnectorType(dto.getConnectorType()) : entity.getConnectorType())")
    @Mapping(target = "status", expression = "java(dto.getStatus() != null ? parseStatus(dto.getStatus()) : entity.getStatus())")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(ChargeUnit dto, @MappingTarget ChargeUnitEntity entity);

    /**
     * Helper method to parse connector type string to enum
     */
    default ChargeUnitEntity.ConnectorType parseConnectorType(String connectorType) {
        if (connectorType == null) {
            return null;
        }
        try {
            return ChargeUnitEntity.ConnectorType.valueOf(connectorType);
        } catch (IllegalArgumentException e) {
            return ChargeUnitEntity.ConnectorType.Type2; // default
        }
    }

    /**
     * Helper method to parse status string to enum
     */
    default ChargeUnitEntity.ChargeUnitStatus parseStatus(String status) {
        if (status == null) {
            return null;
        }
        try {
            return ChargeUnitEntity.ChargeUnitStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return ChargeUnitEntity.ChargeUnitStatus.OFFLINE; // default
        }
    }
}
