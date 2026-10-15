package com.chargeroute.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiGatewayApplicationTests {

    @MockitoBean
    private ReactiveJwtDecoder jwtDecoder;

    @Test
    void contextLoads() {
        // This test verifies that the API Gateway starts up correctly
        // with all Spring Cloud Gateway configurations
    }
}