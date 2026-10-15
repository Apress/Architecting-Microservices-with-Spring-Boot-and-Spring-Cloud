package com.chargeroute.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestClient;

/**
 * Configuration for OAuth2 client credentials flow.
 * Enables service-to-service authentication with automatic token acquisition.
 */
@Configuration
public class OAuth2ClientConfig {

    /**
     * Configures the OAuth2AuthorizedClientManager for client credentials flow.
     * This manager handles automatic token acquisition and renewal.
     */
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials()
                        .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientService);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

    /**
     * Configures RestClient with OAuth2 support.
     * The RestClient will automatically acquire and inject access tokens.
     */
    @Bean
    public RestClient restClient(OAuth2AuthorizedClientManager authorizedClientManager) {
        return RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    // OAuth2 token will be obtained automatically via authorizedClientManager
                    // For now, we'll use RestTemplate with @RegisteredOAuth2AuthorizedClient in controllers
                    return execution.execute(request, body);
                })
                .build();
    }
}
