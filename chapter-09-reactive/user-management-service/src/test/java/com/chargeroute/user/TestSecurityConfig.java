package com.chargeroute.user;

import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;

import static org.mockito.Mockito.mock;

/**
 * Test configuration that disables OAuth2 security and provides
 * a JDBC DataSource for Flyway migrations in a reactive test environment.
 */
@TestConfiguration
@EnableWebFluxSecurity
public class TestSecurityConfig {

    @Bean
    @Order(-1)
    public SecurityWebFilterChain testSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        return mock(ReactiveJwtDecoder.class);
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
