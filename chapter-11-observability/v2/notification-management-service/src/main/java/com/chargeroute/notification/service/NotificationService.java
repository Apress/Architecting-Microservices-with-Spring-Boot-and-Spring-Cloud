package com.chargeroute.notification.service;

import com.chargeroute.notification.dto.Notification;
import com.chargeroute.notification.dto.NotificationTemplate;
import com.chargeroute.notification.entity.NotificationEntity;
import com.chargeroute.notification.entity.NotificationTemplateEntity;
import com.chargeroute.notification.mapper.NotificationMapper;
import com.chargeroute.notification.mapper.NotificationTemplateMapper;
import com.chargeroute.notification.observability.NotificationObservabilityMetrics;
import com.chargeroute.notification.repository.NotificationRepository;
import com.chargeroute.notification.repository.NotificationTemplateRepository;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.annotation.NewSpan;
import io.micrometer.tracing.annotation.SpanTag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for managing notifications
 * Handles business logic and coordinates between controllers and repositories
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationTemplateMapper templateMapper;
    private final NotificationObservabilityMetrics observabilityMetrics;

    /**
     * Create a new notification
     *
     * @param notification the notification DTO to create
     * @return the created notification DTO with generated ID
     */
    @Transactional
    @NewSpan("notification-create")
    @Observed(name = "notification.create", contextualName = "create-notification")
    public Notification createNotification(@SpanTag("user.id") Notification notification) {
        log.info("Creating new notification for user: {} of type: {}",
                notification.getUserId(), notification.getType());

        NotificationEntity entity = notificationMapper.toEntity(notification);

        // Set default status if not provided
        if (entity.getStatus() == null) {
            entity.setStatus(NotificationEntity.NotificationStatus.PENDING);
        }

        try {
            NotificationEntity savedEntity = notificationRepository.save(entity);
            log.info("Created notification with ID: {}", savedEntity.getNotificationId());
            return notificationMapper.toDto(savedEntity);
        } catch (Exception e) {
            log.error("Failed to create notification", e);
            observabilityMetrics.recordNotificationFailed(entity.getChannel().name());
            throw e;
        }
    }

    /**
     * Get a notification by ID
     *
     * @param notificationId the notification ID
     * @return Optional containing the notification DTO if found
     */
    public Optional<Notification> getById(String notificationId) {
        log.debug("Fetching notification with ID: {}", notificationId);
        return notificationRepository.findById(notificationId)
                .map(notificationMapper::toDto);
    }

    /**
     * Get all notifications
     *
     * @return list of all notifications
     */
    public List<Notification> getAllNotifications() {
        log.debug("Fetching all notifications");
        return notificationMapper.toDtoList(notificationRepository.findAll());
    }

    /**
     * Get all notifications for a specific user
     *
     * @param userId the user ID
     * @return list of notifications for the user
     */
    public List<Notification> getByUserId(String userId) {
        log.debug("Fetching notifications for user: {}", userId);
        return notificationMapper.toDtoList(notificationRepository.findByUserId(userId));
    }

    /**
     * Get notifications by status
     *
     * @param status the notification status
     * @return list of notifications with the specified status
     */
    public List<Notification> getByStatus(String status) {
        log.debug("Fetching notifications with status: {}", status);
        NotificationEntity.NotificationStatus entityStatus =
                notificationMapper.mapStringToNotificationStatus(status);
        return notificationMapper.toDtoList(notificationRepository.findByStatus(entityStatus));
    }

    /**
     * Update notification status
     *
     * @param notificationId the notification ID
     * @param status         the new status
     * @return Optional containing the updated notification DTO if found
     */
    @Transactional
    @NewSpan("notification-update-status")
    @Observed(name = "notification.updateStatus", contextualName = "update-notification-status")
    public Optional<Notification> updateStatus(@SpanTag("notification.id") String notificationId,
                                                @SpanTag("notification.status") String status) {
        log.info("Updating notification {} status to: {}", notificationId, status);

        return notificationRepository.findById(notificationId)
                .map(entity -> {
                    NotificationEntity.NotificationStatus newStatus =
                            notificationMapper.mapStringToNotificationStatus(status);
                    entity.setStatus(newStatus);

                    if ("SENT".equals(status) && entity.getSentAt() == null) {
                        entity.setSentAt(LocalDateTime.now());
                        // Record successful notification sent
                        observabilityMetrics.recordNotificationSent(entity.getChannel().name());
                        log.info("Notification {} sent successfully via {}", notificationId, entity.getChannel());
                    } else if ("FAILED".equals(status)) {
                        // Record notification failure
                        observabilityMetrics.recordNotificationFailed(entity.getChannel().name());
                        log.warn("Notification {} failed for channel {}", notificationId, entity.getChannel());
                    }

                    NotificationEntity updatedEntity = notificationRepository.save(entity);
                    log.info("Successfully updated notification {} status", notificationId);
                    return notificationMapper.toDto(updatedEntity);
                });
    }

    /**
     * Update an existing notification
     *
     * @param notificationId  the notification ID
     * @param notificationDto the updated notification data
     * @return Optional containing the updated notification DTO if found
     */
    @Transactional
    @NewSpan("notification-update")
    @Observed(name = "notification.update", contextualName = "update-notification")
    public Optional<Notification> updateNotification(@SpanTag("notification.id") String notificationId,
                                                      Notification notificationDto) {
        log.info("Updating notification: {}", notificationId);

        return notificationRepository.findById(notificationId)
                .map(existingEntity -> {
                    notificationMapper.updateEntityFromDto(notificationDto, existingEntity);
                    NotificationEntity updatedEntity = notificationRepository.save(existingEntity);
                    log.info("Successfully updated notification: {}", notificationId);
                    return notificationMapper.toDto(updatedEntity);
                });
    }

    /**
     * Delete a notification by ID
     *
     * @param notificationId the notification ID to delete
     * @return true if the notification was deleted, false if not found
     */
    @Transactional
    public boolean deleteNotification(String notificationId) {
        log.info("Deleting notification: {}", notificationId);

        if (notificationRepository.existsById(notificationId)) {
            notificationRepository.deleteById(notificationId);
            log.info("Successfully deleted notification: {}", notificationId);
            return true;
        }

        log.warn("Notification not found for deletion: {}", notificationId);
        return false;
    }

    /**
     * Get all notification templates
     *
     * @return list of all templates
     */
    public List<NotificationTemplate> getTemplates() {
        log.debug("Fetching all notification templates");
        return templateMapper.toDtoList(templateRepository.findAll());
    }

    /**
     * Get active notification templates
     *
     * @return list of active templates
     */
    public List<NotificationTemplate> getActiveTemplates() {
        log.debug("Fetching active notification templates");
        return templateMapper.toDtoList(templateRepository.findByActiveTrue());
    }

    /**
     * Get templates by notification type
     *
     * @param type the notification type
     * @return list of templates for the specified type
     */
    public List<NotificationTemplate> getTemplatesByType(String type) {
        log.debug("Fetching templates for type: {}", type);
        NotificationEntity.NotificationType entityType =
                notificationMapper.mapStringToNotificationType(type);
        return templateMapper.toDtoList(templateRepository.findByNotificationType(entityType));
    }

    /**
     * Create a new notification template
     *
     * @param template the template DTO to create
     * @return the created template DTO
     */
    @Transactional
    public NotificationTemplate createTemplate(NotificationTemplate template) {
        log.info("Creating new notification template: {}", template.getName());

        NotificationTemplateEntity entity = templateMapper.toEntity(template);

        // Set default active state if not provided
        if (entity.getActive() == null) {
            entity.setActive(true);
        }

        // Set default channel if not provided
        if (entity.getChannel() == null) {
            entity.setChannel(NotificationEntity.NotificationChannel.EMAIL);
        }

        NotificationTemplateEntity savedEntity = templateRepository.save(entity);

        log.info("Created template with ID: {}", savedEntity.getTemplateId());
        return templateMapper.toDto(savedEntity);
    }

    /**
     * Get a template by ID
     *
     * @param templateId the template ID
     * @return Optional containing the template DTO if found
     */
    public Optional<NotificationTemplate> getTemplateById(Long templateId) {
        log.debug("Fetching template with ID: {}", templateId);
        return templateRepository.findById(templateId)
                .map(templateMapper::toDto);
    }
}
