package com.chargeroute.session;

import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;

import static org.mockito.Mockito.mock;

/**
 * Test configuration that provides necessary beans for reactive tests.
 * <p>
 * Key aspects:
 * 1. Mock ReactiveJwtDecoder to bypass OAuth2 resource server security
 * 2. Mock ReactiveClientRegistrationRepository for OAuth2 client (needed by ReactiveServiceClient)
 * 3. JDBC DataSource for Flyway migrations (Flyway requires blocking JDBC)
 * 4. R2DBC connection configured via @DynamicPropertySource in test class
 * <p>
 * This solves the common problem: "Flyway migrations aren't running in reactive test environment"
 * Solution: Create a dedicated JDBC DataSource bean for Flyway, separate from R2DBC.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        return mock(ReactiveJwtDecoder.class);
    }

    /**
     * Mock ReactiveClientRegistrationRepository for OAuth2 client support in tests.
     * This is needed by ReactiveServiceClient which uses WebClient with OAuth2.
     */
    @Bean
    @Primary
    public ReactiveClientRegistrationRepository reactiveClientRegistrationRepository() {
        // Create a minimal test client registration
        ClientRegistration testRegistration = ClientRegistration
                .withRegistrationId("test-client")
                .clientId("test-client-id")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("http://localhost:9999/token")
                .build();

        return new InMemoryReactiveClientRegistrationRepository(testRegistration);
    }

    /**
     * Create a dedicated JDBC DataSource for Flyway migrations.
     * This is separate from the R2DBC ConnectionFactory used at runtime.
     * <p>
     * Using DriverManagerDataSource (simple, non-pooling) for tests.
     *
     * @FlywayDataSource tells Spring Boot to use this DataSource specifically for Flyway.
     */
    @Bean
    @FlywayDataSource
    public DataSource flywayDataSource(PostgreSQLContainer<?> postgres) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(postgres.getJdbcUrl());
        dataSource.setUsername(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        return dataSource;
    }
}
