package com.chargeroute.notification.mapper;

import com.chargeroute.notification.dto.NotificationTemplate;
import com.chargeroute.notification.entity.NotificationTemplateEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for converting between NotificationTemplate entities and DTOs
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface NotificationTemplateMapper {

    /**
     * Convert NotificationTemplateEntity to NotificationTemplate DTO
     */
    @Mapping(source = "templateId", target = "templateId")
    @Mapping(source = "templateName", target = "name")
    @Mapping(source = "notificationType", target = "type")
    @Mapping(target = "requiredVariables", ignore = true)
    @Mapping(target = "defaultValues", ignore = true)
    NotificationTemplate toDto(NotificationTemplateEntity entity);

    /**
     * Convert NotificationTemplate DTO to NotificationTemplateEntity
     */
    @Mapping(source = "templateId", target = "templateId")
    @Mapping(source = "name", target = "templateName")
    @Mapping(source = "type", target = "notificationType")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    NotificationTemplateEntity toEntity(NotificationTemplate dto);

    /**
     * Convert list of entities to DTOs
     */
    List<NotificationTemplate> toDtoList(List<NotificationTemplateEntity> entities);

    /**
     * Update existing entity from DTO, ignoring null values
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "templateId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(NotificationTemplate dto, @MappingTarget NotificationTemplateEntity entity);
}
