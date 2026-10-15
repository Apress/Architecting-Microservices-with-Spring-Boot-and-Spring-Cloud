package com.chargeroute.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security integration tests for the User Management Service.
 *
 * Demonstrates how to test OAuth2 resource server security using
 * Spring Security's MockMvc test support. The jwt() request post-processor
 * simulates JWT tokens without requiring a running authorization server.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("Request without token returns 401 Unauthorized")
    void requestWithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/users/user123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Request with wrong scope returns 403 Forbidden")
    void requestWithWrongScope_Returns403() throws Exception {
        mockMvc.perform(get("/api/users/user123")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("SCOPE_admin.all"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Request with correct scope returns 200 OK")
    void requestWithCorrectScope_Returns200() throws Exception {
        mockMvc.perform(get("/api/users/user123")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("SCOPE_service.internal"))))
                .andExpect(status().isOk());
    }
}
