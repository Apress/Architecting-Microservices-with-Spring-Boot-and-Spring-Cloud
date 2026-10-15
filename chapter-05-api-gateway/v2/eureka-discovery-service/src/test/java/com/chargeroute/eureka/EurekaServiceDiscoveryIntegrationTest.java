package com.chargeroute.eureka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Eureka Discovery Service.
 * Tests the actual REST API endpoints that services use for registration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EurekaServiceDiscoveryIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Value("${spring.security.user.name}")
    private String eurekaUsername;

    @Value("${spring.security.user.password}")
    private String eurekaPassword;

    @Test
    @DisplayName("Should successfully register a service via REST API")
    void testServiceRegistrationViaRestApi() {
        // Given: A service registration request
        String registrationJson = """
                {
                    "instance": {
                        "instanceId": "test-service-1",
                        "app": "TEST-SERVICE",
                        "hostName": "localhost",
                        "ipAddr": "127.0.0.1",
                        "status": "UP",
                        "port": {
                            "$": 8080,
                            "@enabled": "true"
                        },
                        "dataCenterInfo": {
                            "@class": "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo",
                            "name": "MyOwn"
                        }
                    }
                }
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(eurekaUsername, eurekaPassword);

        HttpEntity<String> request = new HttpEntity<>(registrationJson, headers);

        // When: Posting to the Eureka registration endpoint
        ResponseEntity<String> response = restTemplate.exchange(
                "/eureka/apps/TEST-SERVICE",
                HttpMethod.POST,
                request,
                String.class
        );

        // Then: Registration should be successful
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("Should retrieve all registered applications")
    void testGetAllApplications() {
        // When: Requesting all applications
        ResponseEntity<String> response = restTemplate
                .withBasicAuth(eurekaUsername, eurekaPassword)
                .getForEntity("/eureka/apps", String.class);

        // Then: Should return applications
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("applications");
    }

    @Test
    @DisplayName("Should handle heartbeat renewal")
    void testHeartbeatRenewal() {
        // First register a service
        registerTestService();

        // When: Sending a heartbeat
        ResponseEntity<String> response = restTemplate
                .withBasicAuth(eurekaUsername, eurekaPassword)
                .exchange(
                        "/eureka/apps/TEST-SERVICE/test-service-1",
                        HttpMethod.PUT,
                        null,
                        String.class
                );

        // Then: Heartbeat should be accepted (or 404 if instance doesn't exist)
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should handle service deregistration")
    void testServiceDeregistration() {
        // First register a service
        registerTestService();

        // When: Deregistering the service
        ResponseEntity<String> response = restTemplate
                .withBasicAuth(eurekaUsername, eurekaPassword)
                .exchange(
                        "/eureka/apps/TEST-SERVICE/test-service-1",
                        HttpMethod.DELETE,
                        null,
                        String.class
                );

        // Then: Deregistration should be successful (or 404 if not found)
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NOT_FOUND);
    }

    private void registerTestService() {
        String registrationJson = """
                {
                    "instance": {
                        "instanceId": "test-service-1",
                        "app": "TEST-SERVICE",
                        "hostName": "localhost",
                        "ipAddr": "127.0.0.1",
                        "status": "UP",
                        "port": {
                            "$": 8080,
                            "@enabled": "true"
                        },
                        "dataCenterInfo": {
                            "@class": "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo",
                            "name": "MyOwn"
                        }
                    }
                }
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(eurekaUsername, eurekaPassword);

        HttpEntity<String> request = new HttpEntity<>(registrationJson, headers);

        restTemplate.exchange(
                "/eureka/apps/TEST-SERVICE",
                HttpMethod.POST,
                request,
                String.class
        );
    }
}