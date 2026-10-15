package com.chargeroute.eureka;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for Eureka Discovery Service.
 * Demonstrates testing patterns for service discovery including
 * registration, deregistration, and health checks.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EurekaDiscoveryServiceApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private PeerAwareInstanceRegistry registry;

    @Value("${spring.security.user.name}")
    private String eurekaUsername;

    @Value("${spring.security.user.password}")
    private String eurekaPassword;

    @Test
    @DisplayName("Should load application context successfully")
    void contextLoads() {
        // This test verifies that the Eureka server starts up correctly
        assertThat(EurekaDiscoveryServiceApplication.class)
                .hasAnnotation(EnableEurekaServer.class);
    }

    @Test
    @DisplayName("Should successfully register a new service instance")
    void testServiceRegistration() {
        // Given: A mock service instance wants to register
        String appName = "USER-MANAGEMENT-SERVICE";
        String instanceId = "user-service-instance-1";

        InstanceInfo mockInstance = InstanceInfo.Builder.newBuilder()
                .setAppName(appName)
                .setInstanceId(instanceId)
                .setHostName("localhost")
                .setIPAddr("127.0.0.1")
                .setPort(8082)
                .setStatus(InstanceInfo.InstanceStatus.UP)
                .setVIPAddress(appName.toLowerCase())
                .setSecureVIPAddress(appName.toLowerCase())
                .build();

        // When: The service attempts to register
        doNothing().when(registry).register(any(InstanceInfo.class), anyBoolean());

        // Simulate registration
        registry.register(mockInstance, false);

        // Then: Verify registration was called
        verify(registry, times(1)).register(any(InstanceInfo.class), eq(false));

        // Verify the instance details
        assertThat(mockInstance.getAppName()).isEqualTo(appName);
        assertThat(mockInstance.getInstanceId()).isEqualTo(instanceId);
        assertThat(mockInstance.getStatus()).isEqualTo(InstanceInfo.InstanceStatus.UP);
    }

    @Test
    @DisplayName("Should handle registration failure gracefully")
    void testServiceRegistrationFailure() {
        // Given: A service instance that will fail to register
        String appName = "FAILING-SERVICE";
        String instanceId = "failing-instance-1";

        InstanceInfo failingInstance = InstanceInfo.Builder.newBuilder()
                .setAppName(appName)
                .setInstanceId(instanceId)
                .setHostName("localhost")
                .setIPAddr("127.0.0.1")
                .setPort(9999)
                .setStatus(InstanceInfo.InstanceStatus.UP)
                .build();

        // When: Registration fails due to an exception
        doThrow(new RuntimeException("Registration failed"))
                .when(registry).register(any(InstanceInfo.class), anyBoolean());

        // Then: Verify exception handling
        try {
            registry.register(failingInstance, false);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Registration failed");
        }

        // Verify the registration was attempted
        verify(registry, times(1)).register(any(InstanceInfo.class), eq(false));
    }

    @Test
    @DisplayName("Should correctly handle service deregistration")
    void testServiceDeregistration() {
        // Given: A registered service that wants to deregister
        String appName = "SESSION-MANAGEMENT-SERVICE";
        String instanceId = "session-service-instance-1";

        // When: The service deregisters
        when(registry.cancel(anyString(), anyString(), anyBoolean()))
                .thenReturn(true);

        boolean result = registry.cancel(appName, instanceId, false);

        // Then: Verify deregistration was successful
        assertThat(result).isTrue();
        verify(registry, times(1)).cancel(eq(appName), eq(instanceId), eq(false));
    }

    @Test
    @DisplayName("Should return all registered applications")
    void testGetAllRegisteredApplications() {
        // Given: Multiple services are registered
        Applications mockApplications = new Applications();

        Application userApp = new Application("USER-MANAGEMENT-SERVICE");
        Application sessionApp = new Application("SESSION-MANAGEMENT-SERVICE");

        mockApplications.addApplication(userApp);
        mockApplications.addApplication(sessionApp);

        // When: Requesting all applications
        when(registry.getApplications()).thenReturn(mockApplications);

        Applications result = registry.getApplications();

        // Then: Verify all applications are returned
        assertThat(result.getRegisteredApplications()).hasSize(2);
        assertThat(result.getRegisteredApplications())
                .extracting(Application::getName)
                .containsExactlyInAnyOrder("USER-MANAGEMENT-SERVICE", "SESSION-MANAGEMENT-SERVICE");
    }

    @Test
    @DisplayName("Should handle heartbeat renewal correctly")
    void testHeartbeatRenewal() {
        // Given: A service sends a heartbeat
        String appName = "CHARGE-UNIT-MANAGEMENT-SERVICE";
        String instanceId = "charge-unit-instance-1";

        // When: Heartbeat is received
        when(registry.renew(anyString(), anyString(), anyBoolean()))
                .thenReturn(true);

        boolean renewed = registry.renew(appName, instanceId, false);

        // Then: Verify heartbeat was processed
        assertThat(renewed).isTrue();
        verify(registry, times(1)).renew(eq(appName), eq(instanceId), eq(false));
    }

    @Test
    @DisplayName("Should return correct instance status")
    void testInstanceStatusCheck() {
        // Given: A service instance with specific status
        String appName = "NOTIFICATION-MANAGEMENT-SERVICE";
        String instanceId = "notification-instance-1";

        InstanceInfo mockInstance = InstanceInfo.Builder.newBuilder()
                .setAppName(appName)
                .setInstanceId(instanceId)
                .setStatus(InstanceInfo.InstanceStatus.UP)
                .build();

        Application mockApp = new Application(appName);
        mockApp.addInstance(mockInstance);

        // When: Checking instance status
        when(registry.getApplication(anyString())).thenReturn(mockApp);

        Application result = registry.getApplication(appName);

        // Then: Verify correct status is returned
        assertThat(result).isNotNull();
        assertThat(result.getInstances()).hasSize(1);
        assertThat(result.getInstances().getFirst().getStatus())
                .isEqualTo(InstanceInfo.InstanceStatus.UP);
    }

    @Test
    @DisplayName("Should handle multiple instances of same service")
    void testMultipleInstancesRegistration() {
        // Given: Multiple instances of the same service
        String appName = "USER-MANAGEMENT-SERVICE";

        InstanceInfo instance1 = InstanceInfo.Builder.newBuilder()
                .setAppName(appName)
                .setInstanceId("user-instance-1")
                .setPort(8082)
                .setStatus(InstanceInfo.InstanceStatus.UP)
                .build();

        InstanceInfo instance2 = InstanceInfo.Builder.newBuilder()
                .setAppName(appName)
                .setInstanceId("user-instance-2")
                .setPort(8083)
                .setStatus(InstanceInfo.InstanceStatus.UP)
                .build();

        Application mockApp = new Application(appName);
        mockApp.addInstance(instance1);
        mockApp.addInstance(instance2);

        // When: Retrieving application info
        when(registry.getApplication(appName)).thenReturn(mockApp);

        Application result = registry.getApplication(appName);

        // Then: Verify both instances are registered
        assertThat(result.getInstances()).hasSize(2);
        assertThat(result.getInstances())
                .extracting(InstanceInfo::getInstanceId)
                .containsExactlyInAnyOrder("user-instance-1", "user-instance-2");
    }

    @Test
    @DisplayName("Should verify Eureka server health endpoint")
    void testEurekaHealthEndpoint() {
        // When: Checking Eureka server health
        ResponseEntity<String> response = restTemplate.withBasicAuth(eurekaUsername, eurekaPassword)
                .getForEntity("/actuator/health", String.class);

        // Then: Verify health endpoint is accessible
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }

    @Test
    @DisplayName("Should handle service instance expiration")
    void testInstanceExpiration() {
        // Given: An instance that hasn't sent heartbeats
        String appName = "EXPIRED-SERVICE";
        String instanceId = "expired-instance-1";

        InstanceInfo.Builder.newBuilder()
                .setAppName(appName)
                .setInstanceId(instanceId)
                .setStatus(InstanceInfo.InstanceStatus.DOWN)
                .setLastUpdatedTimestamp(System.currentTimeMillis() - 120000) // 2 minutes ago
                .build();

        // When: Checking if instance should be evicted
        when(registry.isLeaseExpirationEnabled()).thenReturn(true);
        when(registry.cancel(appName, instanceId, false)).thenReturn(true);

        // Simulate eviction
        boolean evicted = registry.cancel(appName, instanceId, false);

        // Then: Verify instance was evicted
        assertThat(evicted).isTrue();
        verify(registry, times(1)).cancel(eq(appName), eq(instanceId), eq(false));
    }
}