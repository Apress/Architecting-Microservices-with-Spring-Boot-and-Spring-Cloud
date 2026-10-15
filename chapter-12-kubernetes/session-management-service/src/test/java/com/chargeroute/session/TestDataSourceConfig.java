package com.chargeroute.session;

import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;

/**
 * Test configuration that handles Flyway migrations for Testcontainers.
 *
 * This configuration creates a separate JDBC DataSource for Flyway migrations
 * while allowing R2DBC to be used for reactive repository operations.
 *
 * Required because:
 * - Flyway only works with JDBC, not R2DBC
 * - Testcontainers require dynamic connection configuration
 * - We need migrations to run before tests start
 *
 * The @FlywayDataSource annotation tells Spring Boot to use this DataSource
 * specifically for Flyway auto-configuration.
 */
@TestConfiguration
@Profile("test")
public class TestDataSourceConfig {

    /**
     * Creates a JDBC DataSource specifically for Flyway migrations.
     * Uses the PostgreSQLContainer that's injected as a bean from the test class.
     *
     * @FlywayDataSource tells Spring Boot to use this for Flyway migrations
     */
    @Bean
    @FlywayDataSource
    public DataSource flywayDataSource(PostgreSQLContainer<?> postgresContainer) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(postgresContainer.getJdbcUrl());
        dataSource.setUsername(postgresContainer.getUsername());
        dataSource.setPassword(postgresContainer.getPassword());
        return dataSource;
    }
}
