package com.chargeroute.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converts Keycloak realm roles and OAuth2 scopes to Spring Security GrantedAuthority.
 *
 * <p>Keycloak stores realm roles in the JWT under {@code realm_access.roles}.
 * This converter extracts those roles and maps them to {@code ROLE_*} authorities
 * for use with {@code @PreAuthorize("hasRole('DRIVER')")}.</p>
 *
 * <p>Additionally, any {@code scope} claim values are mapped to {@code SCOPE_*}
 * authorities for OAuth2 scope-based authorization.</p>
 */
public class KeycloakGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

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
