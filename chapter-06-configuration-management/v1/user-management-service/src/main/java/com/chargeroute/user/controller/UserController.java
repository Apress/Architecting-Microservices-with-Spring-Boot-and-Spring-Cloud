package com.chargeroute.user.controller;

import com.chargeroute.user.dto.UserValidationResponse;
import com.chargeroute.user.model.User;
import com.chargeroute.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@PathVariable String userId) {
        log.info("Getting user: {}", userId);
        
        User user = userService.getUser(userId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{userId}/validate")
    public ResponseEntity<UserValidationResponse> validateUser(@PathVariable String userId) {
        log.info("Validating user: {}", userId);
        
        UserValidationResponse response = userService.validateUser(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfiguration() {
        log.info("Getting current configuration");
        return ResponseEntity.ok(userService.getConfiguration());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("User Management Service is running!");
    }
}