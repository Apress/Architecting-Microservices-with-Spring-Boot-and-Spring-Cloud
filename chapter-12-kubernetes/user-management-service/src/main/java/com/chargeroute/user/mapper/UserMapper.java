package com.chargeroute.user.mapper;

import com.chargeroute.user.dto.User;
import com.chargeroute.user.dto.UserPreferences;
import com.chargeroute.user.dto.UserRegistrationRequest;
import com.chargeroute.user.entity.UserEntity;
import com.chargeroute.user.entity.UserPreferencesEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    /**
     * Map UserEntity to User DTO
     */
    User toDto(UserEntity entity);

    /**
     * Map User DTO to UserEntity
     */
    @Mapping(target = "preferences", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserEntity toEntity(User dto);

    /**
     * Map UserRegistrationRequest to UserEntity
     */
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "status", constant = "PENDING_VERIFICATION")
    @Mapping(target = "preferences", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    UserEntity toEntity(UserRegistrationRequest request);

    /**
     * Map UserPreferencesEntity to UserPreferences DTO
     */
    @Mapping(target = "preferredConnectorType", source = "preferredConnectorType")
    @Mapping(target = "notificationsEnabled", source = "notificationsEnabled")
    @Mapping(target = "notificationChannel", source = "notificationChannel")
    @Mapping(target = "autoChargeEnabled", source = "autoChargeEnabled")
    @Mapping(target = "maxChargeAmount", source = "maxChargeAmount")
    @Mapping(target = "preferredPaymentMethod", source = "preferredPaymentMethod")
    UserPreferences toPreferencesDto(UserPreferencesEntity entity);

    /**
     * Map UserPreferences DTO to UserPreferencesEntity
     */
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserPreferencesEntity toPreferencesEntity(UserPreferences dto);

    /**
     * Update UserEntity from User DTO
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "preferences", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(User dto, @MappingTarget UserEntity entity);

    /**
     * Update UserPreferencesEntity from UserPreferences DTO
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updatePreferencesEntityFromDto(UserPreferences dto, @MappingTarget UserPreferencesEntity entity);
}
