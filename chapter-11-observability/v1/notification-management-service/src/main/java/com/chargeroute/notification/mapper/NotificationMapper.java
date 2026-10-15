package com.chargeroute.notification.mapper;

import com.chargeroute.notification.dto.Notification;
import com.chargeroute.notification.entity.NotificationEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for converting between Notification entities and DTOs
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface NotificationMapper {

    /**
     * Convert NotificationEntity to Notification DTO
     */
    @Mapping(source = "type", target = "type")
    @Mapping(source = "channel", target = "channel")
    @Mapping(source = "status", target = "status")
    Notification toDto(NotificationEntity entity);

    /**
     * Convert Notification DTO to NotificationEntity
     */
    @Mapping(source = "type", target = "type")
    @Mapping(source = "channel", target = "channel")
    @Mapping(source = "status", target = "status")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    NotificationEntity toEntity(Notification dto);

    /**
     * Convert list of entities to DTOs
     */
    List<Notification> toDtoList(List<NotificationEntity> entities);

    /**
     * Update existing entity from DTO, ignoring null values
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "notificationId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(Notification dto, @MappingTarget NotificationEntity entity);

    /**
     * Map string type to enum
     */
    default NotificationEntity.NotificationType mapStringToNotificationType(String type) {
        if (type == null) {
            return null;
        }
        return switch (type.toUpperCase()) {
            case "SESSION_STARTED" -> NotificationEntity.NotificationType.SESSION_STARTED;
            case "SESSION_COMPLETED" -> NotificationEntity.NotificationType.SESSION_COMPLETED;
            case "PAYMENT_RECEIVED" -> NotificationEntity.NotificationType.PAYMENT_RECEIVED;
            case "PAYMENT_FAILED" -> NotificationEntity.NotificationType.PAYMENT_FAILED;
            case "MAINTENANCE_ALERT" -> NotificationEntity.NotificationType.MAINTENANCE_ALERT;
            case "LOW_BATTERY_WARNING" -> NotificationEntity.NotificationType.LOW_BATTERY_WARNING;
            case "CHARGING_COMPLETE" -> NotificationEntity.NotificationType.CHARGING_COMPLETE;
            case "ACCOUNT_UPDATE" -> NotificationEntity.NotificationType.ACCOUNT_UPDATE;
            default -> NotificationEntity.NotificationType.ACCOUNT_UPDATE;
        };
    }

    /**
     * Map enum type to string
     */
    default String mapNotificationTypeToString(NotificationEntity.NotificationType type) {
        if (type == null) {
            return null;
        }
        return type.name();
    }

    /**
     * Map string channel to enum
     */
    default NotificationEntity.NotificationChannel mapStringToNotificationChannel(String channel) {
        if (channel == null) {
            return null;
        }
        return switch (channel.toUpperCase()) {
            case "EMAIL" -> NotificationEntity.NotificationChannel.EMAIL;
            case "SMS" -> NotificationEntity.NotificationChannel.SMS;
            case "PUSH" -> NotificationEntity.NotificationChannel.PUSH;
            case "IN_APP" -> NotificationEntity.NotificationChannel.IN_APP;
            default -> NotificationEntity.NotificationChannel.EMAIL;
        };
    }

    /**
     * Map enum channel to string
     */
    default String mapNotificationChannelToString(NotificationEntity.NotificationChannel channel) {
        if (channel == null) {
            return null;
        }
        return channel.name();
    }

    /**
     * Map string status to enum
     */
    default NotificationEntity.NotificationStatus mapStringToNotificationStatus(String status) {
        if (status == null) {
            return null;
        }
        return switch (status.toUpperCase()) {
            case "PENDING" -> NotificationEntity.NotificationStatus.PENDING;
            case "SENT" -> NotificationEntity.NotificationStatus.SENT;
            case "FAILED" -> NotificationEntity.NotificationStatus.FAILED;
            case "RETRYING" -> NotificationEntity.NotificationStatus.RETRYING;
            default -> NotificationEntity.NotificationStatus.PENDING;
        };
    }

    /**
     * Map enum status to string
     */
    default String mapNotificationStatusToString(NotificationEntity.NotificationStatus status) {
        if (status == null) {
            return null;
        }
        return status.name();
    }
}
