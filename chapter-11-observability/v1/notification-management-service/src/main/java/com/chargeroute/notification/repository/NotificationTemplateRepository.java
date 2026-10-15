package com.chargeroute.notification.repository;

import com.chargeroute.notification.entity.NotificationEntity;
import com.chargeroute.notification.entity.NotificationTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplateEntity, Long> {

    /**
     * Find templates by notification type
     */
    List<NotificationTemplateEntity> findByNotificationType(NotificationEntity.NotificationType notificationType);

    /**
     * Find a template by name and channel
     */
    Optional<NotificationTemplateEntity> findByTemplateNameAndChannel(
            String templateName,
            NotificationEntity.NotificationChannel channel
    );

    /**
     * Find all active templates
     */
    List<NotificationTemplateEntity> findByActiveTrue();

    /**
     * Find active templates by type
     */
    List<NotificationTemplateEntity> findByNotificationTypeAndActiveTrue(
            NotificationEntity.NotificationType notificationType
    );
}
