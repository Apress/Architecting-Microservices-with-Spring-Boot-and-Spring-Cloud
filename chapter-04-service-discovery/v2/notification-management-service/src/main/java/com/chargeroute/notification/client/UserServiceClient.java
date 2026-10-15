package com.chargeroute.notification.client;

import com.chargeroute.notification.dto.client.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-management-service", path = "/api/users")
public interface UserServiceClient {
    
    @GetMapping("/{userId}")
    User getUserById(@PathVariable("userId") String userId);
}