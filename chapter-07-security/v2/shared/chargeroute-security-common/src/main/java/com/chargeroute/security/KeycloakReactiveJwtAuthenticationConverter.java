package com.chargeroute.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import reactor.core.publisher.Mono;

/**
 * Auto-configuration for Keycloak JWT authentication in reactive microservices.
 *
 * <p>Provides a reactive {@link Converter} bean that wraps the
 * {@link KeycloakGrantedAuthoritiesConverter} in a
 * {@link ReactiveJwtAuthenticationConverterAdapter} for use with Spring WebFlux
 * applications such as the API Gateway.</p>
 *
 * <p>This configuration is automatically activated in reactive Spring Boot
 * applications via Spring Boot auto-configuration.</p>
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(ReactiveJwtAuthenticationConverterAdapter.class)
public class KeycloakReactiveJwtAuthenticationConverter {

    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(new KeycloakGrantedAuthoritiesConverter());
        return new ReactiveJwtAuthenticationConverterAdapter(jwtConverter);
    }
}
