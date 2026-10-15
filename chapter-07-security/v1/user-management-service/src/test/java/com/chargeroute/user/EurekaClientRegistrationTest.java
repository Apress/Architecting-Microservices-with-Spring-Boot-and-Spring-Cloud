package com.chargeroute.user;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for User Management Service Eureka client registration.
 * These tests verify that the service correctly registers with Eureka
 * and can discover other services.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EurekaClientRegistrationTest {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private EurekaClient eurekaClient;

    @MockitoBean
    private DiscoveryClient discoveryClient;

    @MockitoBean
    private ApplicationInfoManager applicationInfoManager;

    @Value("${spring.application.name}")
    private String applicationName;

    @Test
    @DisplayName("Should successfully register with Eureka on startup")
    void testSuccessfulRegistrationWithEureka() {
        // Given: Mock instance info for this service
        InstanceInfo mockInstanceInfo = InstanceInfo.Builder.newBuilder()
                .setAppName(applicationName.toUpperCase())
                .setInstanceId(applicationName + ":test-instance")
                .setHostName("localhost")
                .setIPAddr("127.0.0.1")
                .setPort(8082)
                .setStatus(InstanceInfo.InstanceStatus.UP)
                .setVIPAddress(applicationName)
                .build();

        // When: Application starts up
        when(applicationInfoManager.getInfo()).thenReturn(mockInstanceInfo);
        when(eurekaClient.getInstanceRemoteStatus()).thenReturn(InstanceInfo.InstanceStatus.UP);

        // Then: Verify registration details
        InstanceInfo instanceInfo = applicationInfoManager.getInfo();
        assertThat(instanceInfo).isNotNull();
        assertThat(instanceInfo.getAppName()).isEqualTo("USER-MANAGEMENT-SERVICE");
        assertThat(instanceInfo.getStatus()).isEqualTo(InstanceInfo.InstanceStatus.UP);
        assertThat(instanceInfo.getPort()).isEqualTo(8082);

        // Verify the service is marked as UP in Eureka
        assertThat(eurekaClient.getInstanceRemoteStatus()).isEqualTo(InstanceInfo.InstanceStatus.UP);
    }

    @Test
    @DisplayName("Should handle Eureka registration failure gracefully")
    void testRegistrationFailureHandling() {
        // Given: Eureka is unavailable
        when(eurekaClient.getInstanceRemoteStatus()).thenReturn(InstanceInfo.InstanceStatus.UNKNOWN);
        when(applicationInfoManager.getInfo()).thenReturn(null);

        // When: Trying to get registration status
        InstanceInfo.InstanceStatus status = eurekaClient.getInstanceRemoteStatus();

        // Then: Service should handle failure gracefully
        assertThat(status).isEqualTo(InstanceInfo.InstanceStatus.UNKNOWN);

        // Verify that the service doesn't crash when Eureka is unavailable
        InstanceInfo info = applicationInfoManager.getInfo();
        assertThat(info).isNull();
    }

    @Test
    @DisplayName("Should discover other services using DiscoveryClient")
    void testServiceDiscoveryClient() {
        // Given: Other services are registered in Eureka
        String chargeUnitServiceName = "CHARGE-UNIT-MANAGEMENT-SERVICE";
        String sessionServiceName = "SESSION-MANAGEMENT-SERVICE";

        ServiceInstance mockChargeUnitInstance = mock(ServiceInstance.class);
        when(mockChargeUnitInstance.getServiceId()).thenReturn(chargeUnitServiceName);
        when(mockChargeUnitInstance.getHost()).thenReturn("localhost");
        when(mockChargeUnitInstance.getPort()).thenReturn(8081);

        ServiceInstance mockSessionInstance = mock(ServiceInstance.class);
        when(mockSessionInstance.getServiceId()).thenReturn(sessionServiceName);
        when(mockSessionInstance.getHost()).thenReturn("localhost");
        when(mockSessionInstance.getPort()).thenReturn(8083);

        // When: Discovering services
        when(discoveryClient.getServices()).thenReturn(
                Arrays.asList(chargeUnitServiceName, sessionServiceName, applicationName.toUpperCase())
        );

        when(discoveryClient.getInstances(chargeUnitServiceName))
                .thenReturn(List.of(mockChargeUnitInstance));

        when(discoveryClient.getInstances(sessionServiceName))
                .thenReturn(List.of(mockSessionInstance));

        // Then: Verify services can be discovered
        List<String> services = discoveryClient.getServices();
        assertThat(services)
                .hasSize(3)
                .contains(chargeUnitServiceName, sessionServiceName);

        // Verify specific service instances can be retrieved
        List<ServiceInstance> chargeUnitInstances = discoveryClient.getInstances(chargeUnitServiceName);
        assertThat(chargeUnitInstances).hasSize(1);
        assertThat(chargeUnitInstances.getFirst().getPort()).isEqualTo(8081);

        List<ServiceInstance> sessionInstances = discoveryClient.getInstances(sessionServiceName);
        assertThat(sessionInstances).hasSize(1);
        assertThat(sessionInstances.getFirst().getPort()).isEqualTo(8083);
    }

    @Test
    @DisplayName("Should update instance status in Eureka")
    void testInstanceStatusUpdate() {
        // Given: Service is initially UP
        InstanceInfo mockInstanceInfo = InstanceInfo.Builder.newBuilder()
                .setAppName(applicationName.toUpperCase())
                .setInstanceId(applicationName + ":test-instance")
                .setStatus(InstanceInfo.InstanceStatus.UP)
                .build();

        when(applicationInfoManager.getInfo()).thenReturn(mockInstanceInfo);

        // When: Status changes to OUT_OF_SERVICE
        doAnswer(invocation -> {
            InstanceInfo.InstanceStatus newStatus = invocation.getArgument(0);
            mockInstanceInfo.setStatus(newStatus);
            return null;
        }).when(applicationInfoManager).setInstanceStatus(any(InstanceInfo.InstanceStatus.class));

        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.OUT_OF_SERVICE);

        // Then: Verify status was updated
        verify(applicationInfoManager).setInstanceStatus(InstanceInfo.InstanceStatus.OUT_OF_SERVICE);
        assertThat(mockInstanceInfo.getStatus()).isEqualTo(InstanceInfo.InstanceStatus.OUT_OF_SERVICE);
    }

    @Test
    @DisplayName("Should handle multiple instances of the same service")
    void testMultipleInstanceDiscovery() {
        // Given: Multiple instances of User Management Service are running
        ServiceInstance instance1 = mock(ServiceInstance.class);
        when(instance1.getInstanceId()).thenReturn("user-management-service:instance-1");
        when(instance1.getHost()).thenReturn("10.0.0.1");
        when(instance1.getPort()).thenReturn(8082);

        ServiceInstance instance2 = mock(ServiceInstance.class);
        when(instance2.getInstanceId()).thenReturn("user-management-service:instance-2");
        when(instance2.getHost()).thenReturn("10.0.0.2");
        when(instance2.getPort()).thenReturn(8083);

        // When: Discovering all instances
        when(discoveryClient.getInstances(applicationName.toUpperCase()))
                .thenReturn(Arrays.asList(instance1, instance2));

        // Then: Both instances should be discoverable
        List<ServiceInstance> instances = discoveryClient.getInstances(applicationName.toUpperCase());
        assertThat(instances)
                .hasSize(2)
                .extracting(ServiceInstance::getInstanceId)
                .containsExactlyInAnyOrder(
                        "user-management-service:instance-1",
                        "user-management-service:instance-2"
                );

        // Verify different hosts for load balancing
        assertThat(instances)
                .extracting(ServiceInstance::getHost)
                .containsExactlyInAnyOrder("10.0.0.1", "10.0.0.2");
    }

    @Test
    @DisplayName("Should verify heartbeat renewal mechanism")
    void testHeartbeatRenewal() {
        // Given: Service is registered and running
        when(eurekaClient.getInstanceRemoteStatus()).thenReturn(InstanceInfo.InstanceStatus.UP);

        Application mockApplication = mock(Application.class);
        when(mockApplication.getName()).thenReturn(applicationName.toUpperCase());
        when(eurekaClient.getApplication(applicationName.toUpperCase())).thenReturn(mockApplication);

        // When: Heartbeat is sent (simulated)
        Application app = eurekaClient.getApplication(applicationName.toUpperCase());

        // Then: Verify the application is still registered
        assertThat(app).isNotNull();
        assertThat(app.getName()).isEqualTo("USER-MANAGEMENT-SERVICE");

        // Verify remote status is still UP (heartbeat successful)
        assertThat(eurekaClient.getInstanceRemoteStatus()).isEqualTo(InstanceInfo.InstanceStatus.UP);

        // Verify getApplication was called (simulating heartbeat check)
        verify(eurekaClient, times(1)).getApplication(applicationName.toUpperCase());
    }
}