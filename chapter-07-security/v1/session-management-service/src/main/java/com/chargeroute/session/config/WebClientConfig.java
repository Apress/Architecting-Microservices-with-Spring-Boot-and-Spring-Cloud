package com.chargeroute.session.config;

import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for OAuth2-enabled WebClient with service discovery.
 * Uses ServletOAuth2AuthorizedClientExchangeFilterFunction to automatically
 * acquire and attach OAuth2 access tokens to outgoing service-to-service requests.
 */
@Configuration
public class WebClientConfig {

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
     * Creates a WebClient bean configured with:
     * - OAuth2 client credentials filter for automatic token injection
     * - Load balancer filter for Eureka service discovery
     *
     * The default client registration is set to "session-service-client",
     * so all outgoing requests automatically acquire a token using that registration.
     */
    @Bean
    public WebClient webClient(OAuth2AuthorizedClientManager authorizedClientManager,
                               LoadBalancedExchangeFilterFunction lbFunction) {

        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Filter =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        oauth2Filter.setDefaultClientRegistrationId("session-service-client");

        return WebClient.builder()
                .filter(oauth2Filter)
                .filter(lbFunction)
                .build();
    }
}
