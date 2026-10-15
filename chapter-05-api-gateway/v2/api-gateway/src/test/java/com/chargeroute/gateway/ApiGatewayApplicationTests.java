package com.chargeroute.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
        // This test verifies that the API Gateway starts up correctly
        // with all Spring Cloud Gateway and Eureka configurations
    }
}