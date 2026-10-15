package com.chargeroute.notification.controller;

import com.chargeroute.notification.dto.BulkNotificationRequest;
import com.chargeroute.notification.dto.NotificationPriority;
import com.chargeroute.notification.dto.NotificationRequest;
import com.chargeroute.notification.dto.NotificationResponse;
import com.chargeroute.notification.dto.NotificationStatus;
import com.chargeroute.notification.dto.NotificationTemplate;
import com.chargeroute.notification.dto.client.User;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/notifications")
@Slf4j
public class NotificationController {

    private final WebClient webClient;

    // Service URL for Eureka discovery - no more hardcoded URLs!
    private static final String USER_SERVICE_URL = "http://USER-MANAGEMENT-SERVICE";

    private final Map<String, NotificationStatus> notificationStatusMap = new ConcurrentHashMap<>();
    private final Map<String, NotificationTemplate> templates = new ConcurrentHashMap<>();
    private final AtomicInteger notificationCounter = new AtomicInteger(0);

    public NotificationController(WebClient webClient) {
        this.webClient = webClient;
        this.initializeTemplates();
    }

    @GetMapping("/admin/health")
    @PreAuthorize("hasAuthority('SCOPE_admin.all')")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "service", "Notification Service",
                "status", "UP",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @PostMapping("/send")
    @PreAuthorize("hasAuthority('SCOPE_service.internal')")
    public ResponseEntity<NotificationResponse> sendNotification(@Valid @RequestBody NotificationRequest request) {
        // Verify user exists and get preferences
        // Using WebClient with OAuth2 client credentials for service-to-service auth
        try {
            String userUrl = USER_SERVICE_URL + "/api/users/" + request.getUserId();
            log.info("Fetching user preferences via Eureka from: {}", userUrl);
            User user = this.webClient.get()
                    .uri(userUrl)
                    .retrieve()
                    .bodyToMono(User.class)
                    .block();

            if (Objects.isNull(user)) {
                log.warn("User not found: {}", request.getUserId());
                return ResponseEntity.badRequest().build();
            }

            // Check if notifications are enabled for user
            if (user.getPreferences() != null &&
                    Boolean.FALSE.equals(user.getPreferences().getNotificationsEnabled())) {
                log.info("Notifications disabled for user: {}", request.getUserId());

                NotificationResponse response = NotificationResponse.builder()
                        .notificationId(generateNotificationId())
                        .status("SKIPPED")
                        .sentAt(LocalDateTime.now())
                        .channel(request.getChannel())
                        .errorMessage("User has disabled notifications")
                        .build();

                return ResponseEntity.ok(response);
            }

            // Determine the actual channel to use based on user preferences
            String effectiveChannel = determineChannel(user, request.getChannel());

            // Process the notification based on priority
            String notificationId = generateNotificationId();
            String status = processNotification(request, effectiveChannel, user);

            // Store notification status
            NotificationStatus notificationStatus = NotificationStatus.builder()
                    .notificationId(notificationId)
                    .userId(user.getId())
                    .status(status)
                    .sentAt(LocalDateTime.now())
                    .deliveredAt("SENT".equals(status) ? LocalDateTime.now() : null)
                    .retryCount(0)
                    .build();

            this.notificationStatusMap.put(notificationId, notificationStatus);

            // Simulate sending notification
            log.info("Sending {} notification to user: {} ({}) - Type: {}, Subject: {}",
                    effectiveChannel, user.getId(), user.getEmail(),
                    request.getType(), request.getSubject());

            NotificationResponse response = NotificationResponse.builder()
                    .notificationId(notificationId)
                    .status(status)
                    .sentAt(LocalDateTime.now())
                    .channel(effectiveChannel)
                    .build();

            return ResponseEntity.ok(response);

        } catch (WebClientResponseException e) {
            log.error("Failed to send notification due to User Service error: {} {}",
                    e.getStatusCode(), e.getMessage());

            String notificationId = generateNotificationId();
            NotificationResponse failedResponse = NotificationResponse.builder()
                    .notificationId(notificationId)
                    .status("FAILED")
                    .sentAt(LocalDateTime.now())
                    .channel(request.getChannel())
                    .errorMessage("User service unavailable")
                    .build();

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(failedResponse);
        } catch (Exception e) {
            log.error("Failed to send notification due to unexpected error: {}", e.getMessage());

            String notificationId = generateNotificationId();
            NotificationResponse failedResponse = NotificationResponse.builder()
                    .notificationId(notificationId)
                    .status("FAILED")
                    .sentAt(LocalDateTime.now())
                    .channel(request.getChannel())
                    .errorMessage("User service unavailable")
                    .build();

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(failedResponse);
        }
    }

    @PostMapping("/send-bulk")
    @PreAuthorize("hasAuthority('SCOPE_admin.all')")
    public ResponseEntity<Map<String, Object>> sendBulkNotification(@Valid @RequestBody BulkNotificationRequest request) {
        log.info("Processing bulk notification for {} users - Type: {}",
                request.getUserIds().size(), request.getType());

        int queued = 0;
        int failed = 0;
        List<String> notificationIds = new ArrayList<>();

        // Process each user (in real implementation, this would be async)
        for (String userId : request.getUserIds()) {
            try {
                // Try to verify user exists using WebClient with OAuth2 token
                String userUrl = USER_SERVICE_URL + "/api/users/" + userId;
                User user = this.webClient.get()
                        .uri(userUrl)
                        .retrieve()
                        .bodyToMono(User.class)
                        .block();

                if (user != null) {
                    String notificationId = generateNotificationId();
                    notificationIds.add(notificationId);

                    // Store as queued
                    NotificationStatus status = NotificationStatus.builder()
                            .notificationId(notificationId)
                            .userId(userId)
                            .status("QUEUED")
                            .sentAt(LocalDateTime.now())
                            .retryCount(0)
                            .build();

                    this.notificationStatusMap.put(notificationId, status);
                    queued++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                log.error("Failed to queue notification for user {}: {}", userId, e.getMessage());
                failed++;
            }
        }

        String batchId = "BATCH-" + UUID.randomUUID().toString();

        Map<String, Object> result = Map.of(
                "batchId", batchId,
                "totalRecipients", request.getUserIds().size(),
                "queued", queued,
                "failed", failed,
                "notificationIds", notificationIds,
                "timestamp", LocalDateTime.now()
        );

        log.info("Bulk notification batch {} completed: {} queued, {} failed",
                batchId, queued, failed);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/status/{notificationId}")
    public ResponseEntity<NotificationStatus> getNotificationStatus(@PathVariable String notificationId) {
        NotificationStatus status = this.notificationStatusMap.get(notificationId);

        if (Objects.isNull(status)) {
            // Return mock data for demo
            status = NotificationStatus.builder()
                    .notificationId(notificationId)
                    .userId("USER-123")
                    .status("DELIVERED")
                    .sentAt(LocalDateTime.now().minusMinutes(5))
                    .deliveredAt(LocalDateTime.now().minusMinutes(4))
                    .readAt(LocalDateTime.now().minusMinutes(2))
                    .retryCount(0)
                    .build();
        }

        return ResponseEntity.ok(status);
    }

    @GetMapping("/templates")
    public ResponseEntity<Collection<NotificationTemplate>> getTemplates() {
        return ResponseEntity.ok(this.templates.values());
    }

    @GetMapping("/templates/{templateId}")
    public ResponseEntity<NotificationTemplate> getTemplate(@PathVariable String templateId) {
        NotificationTemplate template = this.templates.get(templateId);
        if (Objects.isNull(template)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(template);
    }

    @PostMapping("/templates")
    @PreAuthorize("hasAuthority('SCOPE_admin.all')")
    public ResponseEntity<NotificationTemplate> createTemplate(@Valid @RequestBody NotificationTemplate template) {
        String templateId = "TMPL-" + UUID.randomUUID().toString();
        template.setTemplateId(templateId);
        this.templates.put(templateId, template);

        log.info("Created notification template: {} - {}", templateId, template.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(template);
    }

    // Helper methods
    private String determineChannel(User user, String requestedChannel) {
        // Use user preferences if available, otherwise use requested channel
        if (user.getPreferences() != null && user.getPreferences().getNotificationChannel() != null) {
            String preferredChannel = user.getPreferences().getNotificationChannel();

            // If user prefers "ALL", use the requested channel
            if ("ALL".equals(preferredChannel)) {
                return requestedChannel;
            }

            return preferredChannel;
        }
        return requestedChannel;
    }

    private String processNotification(NotificationRequest request, String channel, User user) {
        // Simulate different processing based on priority
        if (NotificationPriority.URGENT.equals(request.getPriority())) {
            return "SENT";
        } else if (NotificationPriority.HIGH.equals(request.getPriority())) {
            return "QUEUED";
        } else {
            // Normal processing
            return simulateSending(channel, user) ? "SENT" : "FAILED";
        }
    }

    private boolean simulateSending(String channel, User user) {
        // Simulate sending notification through different channels
        switch (channel) {
            case "EMAIL":
                log.debug("Sending email to: {}", user.getEmail());
                return true;
            case "SMS":
                log.debug("Sending SMS to: {}", user.getPhoneNumber());
                return user.getPhoneNumber() != null;
            case "PUSH":
                log.debug("Sending push notification to user: {}", user.getId());
                return true;
            case "IN_APP":
                log.debug("Creating in-app notification for user: {}", user.getId());
                return true;
            default:
                return false;
        }
    }

    private String generateNotificationId() {
        return String.format("NOTIF-%06d-%s",
                this.notificationCounter.incrementAndGet(),
                UUID.randomUUID().toString().substring(0, 8));
    }

    private void initializeTemplates() {
        // Session started template
        NotificationTemplate sessionStarted = NotificationTemplate.builder()
                .templateId("TMPL-001")
                .name("Session Started")
                .type("SESSION_STARTED")
                .subject("Charging Session Started")
                .bodyTemplate("Your charging session at {{locationName}} has started. Unit: {{chargeUnitId}}")
                .requiredVariables(List.of("locationName", "chargeUnitId"))
                .defaultValues(Map.of("locationName", "Unknown Location"))
                .build();
        this.templates.put(sessionStarted.getTemplateId(), sessionStarted);

        // Session completed template
        NotificationTemplate sessionCompleted = NotificationTemplate.builder()
                .templateId("TMPL-002")
                .name("Session Completed")
                .type("SESSION_COMPLETED")
                .subject("Charging Session Completed")
                .bodyTemplate("Your session is complete! Energy: {{energyDelivered}} kWh, Cost: ${{totalCost}}")
                .requiredVariables(List.of("energyDelivered", "totalCost"))
                .defaultValues(new HashMap<>())
                .build();
        this.templates.put(sessionCompleted.getTemplateId(), sessionCompleted);

        // Low balance template
        NotificationTemplate lowBalance = NotificationTemplate.builder()
                .templateId("TMPL-003")
                .name("Low Balance Alert")
                .type("LOW_BALANCE")
                .subject("Account Balance Low")
                .bodyTemplate("Your account balance (${{balance}}) is below ${{threshold}}. Please add funds.")
                .requiredVariables(List.of("balance", "threshold"))
                .defaultValues(Map.of("threshold", "10.00"))
                .build();
        this.templates.put(lowBalance.getTemplateId(), lowBalance);
    }
}
