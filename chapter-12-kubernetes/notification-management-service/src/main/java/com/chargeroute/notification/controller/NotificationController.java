package com.chargeroute.notification.controller;

import com.chargeroute.notification.dto.Notification;
import com.chargeroute.notification.dto.NotificationRequest;
import com.chargeroute.notification.dto.NotificationResponse;
import com.chargeroute.notification.dto.NotificationTemplate;
import com.chargeroute.notification.dto.client.User;
import com.chargeroute.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Slf4j
@RequiredArgsConstructor
public class NotificationController {

    private final RestTemplate restTemplate;
    private final NotificationService notificationService;

    // Service name for Eureka discovery
    private static final String USER_SERVICE_NAME = "user-management-service";

    @GetMapping("/admin/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "service", "Notification Service",
                "status", "UP",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Create a new notification
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Notification> createNotification(@Valid @RequestBody Notification notification) {
        log.info("Creating new notification for user: {}", notification.getUserId());

        // Generate notification ID if not provided
        if (notification.getNotificationId() == null || notification.getNotificationId().isEmpty()) {
            notification.setNotificationId(UUID.randomUUID().toString());
        }

        Notification createdNotification = notificationService.createNotification(notification);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdNotification.getNotificationId())
                .toUri();

        return ResponseEntity.created(location).body(createdNotification);
    }

    /**
     * Send notification (legacy endpoint - creates notification with user validation)
     */
    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> sendNotification(@Valid @RequestBody NotificationRequest request) {
        try {
            // Verify user exists
            String userUrl = "http://" + USER_SERVICE_NAME + "/api/users/" + request.getUserId();
            log.info("Fetching user preferences via Eureka from: {}", userUrl);
            User user = restTemplate.getForObject(userUrl, User.class);

            if (user == null) {
                log.warn("User not found: {}", request.getUserId());
                return ResponseEntity.badRequest().build();
            }

            // Check if notifications are enabled for user
            if (user.getPreferences() != null &&
                    Boolean.FALSE.equals(user.getPreferences().getNotificationsEnabled())) {
                log.info("Notifications disabled for user: {}", request.getUserId());

                NotificationResponse response = NotificationResponse.builder()
                        .notificationId(UUID.randomUUID().toString())
                        .status("SKIPPED")
                        .sentAt(LocalDateTime.now())
                        .channel(request.getChannel())
                        .errorMessage("User has disabled notifications")
                        .build();

                return ResponseEntity.ok(response);
            }

            // Create the notification
            Notification notification = Notification.builder()
                    .notificationId(UUID.randomUUID().toString())
                    .userId(request.getUserId())
                    .type(request.getType())
                    .channel(request.getChannel())
                    .subject(request.getSubject())
                    .message(request.getMessage())
                    .status("SENT")
                    .sentAt(LocalDateTime.now())
                    .build();

            Notification createdNotification = notificationService.createNotification(notification);

            log.info("Sent {} notification to user: {} ({})",
                    request.getChannel(), user.getId(), user.getEmail());

            NotificationResponse response = NotificationResponse.builder()
                    .notificationId(createdNotification.getNotificationId())
                    .status("SENT")
                    .sentAt(LocalDateTime.now())
                    .channel(request.getChannel())
                    .build();

            return ResponseEntity.ok(response);

        } catch (RestClientException e) {
            log.error("Failed to send notification due to User Service error: {}", e.getMessage());

            NotificationResponse failedResponse = NotificationResponse.builder()
                    .notificationId(UUID.randomUUID().toString())
                    .status("FAILED")
                    .sentAt(LocalDateTime.now())
                    .channel(request.getChannel())
                    .errorMessage("User service unavailable")
                    .build();

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(failedResponse);
        }
    }

    /**
     * Get all notifications
     */
    @GetMapping
    public ResponseEntity<List<Notification>> getAllNotifications() {
        log.debug("Fetching all notifications");
        List<Notification> notifications = notificationService.getAllNotifications();
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get notification by ID
     */
    @GetMapping("/{notificationId}")
    public ResponseEntity<Notification> getNotification(@PathVariable String notificationId) {
        log.debug("Fetching notification: {}", notificationId);
        return notificationService.getById(notificationId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get notifications by user ID
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getNotificationsByUserId(@PathVariable String userId) {
        log.debug("Fetching notifications for user: {}", userId);
        List<Notification> notifications = notificationService.getByUserId(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Update notification status
     */
    @PutMapping("/{notificationId}/status")
    public ResponseEntity<Notification> updateStatus(
            @PathVariable String notificationId,
            @RequestParam String status) {
        log.info("Updating notification {} status to: {}", notificationId, status);

        return notificationService.updateStatus(notificationId, status)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Delete a notification
     */
    @DeleteMapping("/{notificationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteNotification(@PathVariable String notificationId) {
        log.info("Deleting notification: {}", notificationId);

        if (notificationService.deleteNotification(notificationId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Get all notification templates
     */
    @GetMapping("/templates")
    public ResponseEntity<List<NotificationTemplate>> getTemplates() {
        log.debug("Fetching all notification templates");
        List<NotificationTemplate> templates = notificationService.getActiveTemplates();
        return ResponseEntity.ok(templates);
    }

    /**
     * Get template by ID
     */
    @GetMapping("/templates/{templateId}")
    public ResponseEntity<NotificationTemplate> getTemplate(@PathVariable Long templateId) {
        log.debug("Fetching template: {}", templateId);
        return notificationService.getTemplateById(templateId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Create a new notification template
     */
    @PostMapping("/templates")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<NotificationTemplate> createTemplate(@Valid @RequestBody NotificationTemplate template) {
        log.info("Creating notification template: {}", template.getName());

        NotificationTemplate createdTemplate = notificationService.createTemplate(template);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdTemplate.getTemplateId())
                .toUri();

        return ResponseEntity.created(location).body(createdTemplate);
    }
}
