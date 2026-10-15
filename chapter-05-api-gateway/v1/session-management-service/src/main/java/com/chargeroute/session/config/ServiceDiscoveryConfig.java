package com.chargeroute.session.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for service discovery and load balancing.
 * The @LoadBalanced annotation enables client-side load balancing
 * and service discovery through Eureka.
 */
@Configuration
public class ServiceDiscoveryConfig {

    /**
     * Creates a RestTemplate bean with load balancing capabilities.
     * This RestTemplate can resolve service names to actual URLs
     * using Eureka service registry.
     * 
     * @return RestTemplate configured for service discovery
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}