package com.chargeroute.session.config;

import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for OAuth2 client credentials flow with service discovery.
 *
 * <p>Provides a {@link WebClient} bean that automatically acquires OAuth2 access
 * tokens via client credentials grant and resolves service names through Eureka
 * load balancing. This enables secure service-to-service communication where
 * each outbound request carries a valid JWT token.</p>
 */
@Configuration
public class OAuth2ClientConfig {

    /**
     * Configures the OAuth2AuthorizedClientManager for client credentials flow.
     * Uses AuthorizedClientServiceOAuth2AuthorizedClientManager (not the default
     * web-based one) because service-to-service calls happen outside of a user's
     * HTTP request context.
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
     * Creates a WebClient with OAuth2 token injection and load-balanced service discovery.
     *
     * <p>The {@link ServletOAuth2AuthorizedClientExchangeFilterFunction} automatically
     * obtains an access token using client credentials and attaches it as a Bearer
     * token to every outbound request. The {@link LoadBalancedExchangeFilterFunction}
     * resolves logical service names (e.g., {@code http://user-management-service})
     * to actual instance URLs via Eureka.</p>
     *
     * @param authorizedClientManager manages OAuth2 token lifecycle
     * @param lbFunction load-balanced filter for Eureka service discovery
     * @return WebClient configured for secure, load-balanced service calls
     */
    @Bean
    public WebClient webClient(OAuth2AuthorizedClientManager authorizedClientManager,
                               LoadBalancedExchangeFilterFunction lbFunction) {

        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Filter =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        oauth2Filter.setDefaultClientRegistrationId("session-service-client");

        return WebClient.builder()
                .filter(lbFunction)
                .apply(oauth2Filter.oauth2Configuration())
                .build();
    }
}
