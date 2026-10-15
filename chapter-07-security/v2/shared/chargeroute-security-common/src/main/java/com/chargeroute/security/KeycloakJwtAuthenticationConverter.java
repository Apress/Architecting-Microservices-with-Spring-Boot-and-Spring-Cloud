package com.chargeroute.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * Auto-configuration for Keycloak JWT authentication in servlet-based microservices.
 *
 * <p>Provides a {@link JwtAuthenticationConverter} bean that uses
 * {@link KeycloakGrantedAuthoritiesConverter} to extract Keycloak realm roles
 * and OAuth2 scopes from JWT tokens.</p>
 *
 * <p>This configuration is automatically activated in servlet-based Spring Boot
 * applications via Spring Boot auto-configuration. It eliminates the need for
 * each microservice to duplicate the Keycloak role-mapping logic.</p>
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(JwtAuthenticationConverter.class)
public class KeycloakJwtAuthenticationConverter {

    @Bean
    @ConditionalOnMissingBean(JwtAuthenticationConverter.class)
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(new KeycloakGrantedAuthoritiesConverter());
        return jwtConverter;
    }
}
