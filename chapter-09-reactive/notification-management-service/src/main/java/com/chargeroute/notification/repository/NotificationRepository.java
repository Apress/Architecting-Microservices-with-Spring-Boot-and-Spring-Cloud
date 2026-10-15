package com.chargeroute.notification.repository;

import com.chargeroute.notification.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {

    /**
     * Find all notifications for a specific user
     */
    List<NotificationEntity> findByUserId(String userId);

    /**
     * Find all notifications with a specific status
     */
    List<NotificationEntity> findByStatus(NotificationEntity.NotificationStatus status);

    /**
     * Find notifications for a user with a specific status
     */
    List<NotificationEntity> findByUserIdAndStatus(String userId, NotificationEntity.NotificationStatus status);

    /**
     * Find all notifications of a specific type
     */
    List<NotificationEntity> findByType(NotificationEntity.NotificationType type);

    /**
     * Find all notifications by channel
     */
    List<NotificationEntity> findByChannel(NotificationEntity.NotificationChannel channel);
}
