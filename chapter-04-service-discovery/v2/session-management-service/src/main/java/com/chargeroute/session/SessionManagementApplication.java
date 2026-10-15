package com.chargeroute.session;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class SessionManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(SessionManagementApplication.class, args);
    }
}
