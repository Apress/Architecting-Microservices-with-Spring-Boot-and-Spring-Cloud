package com.chargeroute.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Keycloak JWT Authentication Converter for API Gateway
 * <p>
 * Extracts realm roles from Keycloak JWT structure and converts them to Spring Security authorities.
 * This is the ONLY code change required to migrate from Spring Authorization Server to Keycloak.
 */
@Configuration
public class KeycloakSecurityConfig {

    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(new KeycloakGrantedAuthoritiesConverter());
        return new ReactiveJwtAuthenticationConverterAdapter(jwtConverter);
    }

    /**
     * Converts Keycloak realm roles to Spring Security GrantedAuthority
     */
    static class KeycloakGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            // Extract realm_access.roles from Keycloak JWT
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess == null || !realmAccess.containsKey("roles")) {
                return List.of();
            }

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");

            // Convert to ROLE_* authorities for @PreAuthorize("hasRole('DRIVER')")
            List<GrantedAuthority> authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());

            // Also extract scope authorities for OAuth2 scopes
            String scope = jwt.getClaimAsString("scope");
            if (scope != null && !scope.isEmpty()) {
                String[] scopes = scope.split(" ");
                for (String s : scopes) {
                    authorities.add(new SimpleGrantedAuthority("SCOPE_" + s));
                }
            }

            return authorities;
        }
    }
}
