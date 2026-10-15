package com.chargeroute.authserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthorizationServerApplicationTests {

	@Test
	void contextLoads() {
		// This test verifies that the Authorization Server starts up correctly
		// with all OAuth2 configurations
	}
}
