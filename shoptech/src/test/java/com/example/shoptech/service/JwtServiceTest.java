package com.example.shoptech.service;

import java.util.Base64;

import com.example.shoptech.config.JwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String TEST_SECRET = Base64.getEncoder()
            .encodeToString("test-secret-that-is-at-least-32-bytes-long".getBytes());

    @Test
    void createsTokenThatCanBeDecodedForTheSameUser() {
        JwtConfig config = new JwtConfig();
        var key = config.jwtSecretKey(TEST_SECRET);
        JwtEncoder encoder = config.jwtEncoder(key);
        JwtDecoder decoder = config.jwtDecoder(key);
        JwtService service = new JwtService(encoder, 20);

        String token = service.createToken("test-user");

        assertEquals("test-user", decoder.decode(token).getSubject());
        assertEquals(1_200, service.getValiditySeconds());
    }

    @Test
    void rejectsWeakJwtSecrets() {
        JwtConfig config = new JwtConfig();

        assertThrows(IllegalStateException.class, () -> config.jwtSecretKey(
                Base64.getEncoder().encodeToString("too-short".getBytes())));
    }

    @Test
    void seededDemoPasswordsMatchTheirBcryptHashes() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        assertTrue(passwordEncoder.matches(
                "admin123",
                "$2b$10$MrcJ0Gq.4NkI0GaKRFjHYOYhvqw3AMRT1r4S7ps35Jq.UFdNdzlb6"));
        assertTrue(passwordEncoder.matches(
                "123456",
                "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi"));
    }
}
