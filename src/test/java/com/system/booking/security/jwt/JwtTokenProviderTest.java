package com.system.booking.security.jwt;

import com.system.booking.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    private JwtProperties buildProperties(String secret, long expirationMs) {
        JwtProperties props = new JwtProperties();
        props.setSecret(secret);
        props.setExpirationMs(expirationMs);
        return props;
    }

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                buildProperties("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970", 3600000L)
        );
    }

    @Test
    @DisplayName("Should generate token and extract username correctly")
    void generateToken_And_GetUsername_Success() {
        String token = jwtTokenProvider.generateToken("test_user", "ROLE_USER");

        assertNotNull(token);
        assertFalse(token.isEmpty());

        String username = jwtTokenProvider.getUsernameFromToken(token);
        assertEquals("test_user", username);
    }

    @Test
    @DisplayName("Should validate valid token successfully")
    void validateToken_ValidToken_ReturnsTrue() {
        String token = jwtTokenProvider.generateToken("test_user", "ROLE_USER");
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("Should return false for invalid token signature")
    void validateToken_InvalidSignature_ReturnsFalse() {
        JwtTokenProvider anotherProvider = new JwtTokenProvider(
                buildProperties("884E635266556A586E3272357538782F413F4428472B4B6250645367566B5970", 3600000L)
        );

        String token = anotherProvider.generateToken("test_user", "ROLE_USER");
        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("Should return false for malformed token string")
    void validateToken_MalformedToken_ReturnsFalse() {
        assertFalse(jwtTokenProvider.validateToken("not.a.valid.jwt.token"));
    }

    @Test
    @DisplayName("Should return false for empty or null token")
    void validateToken_EmptyToken_ReturnsFalse() {
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken(null));
    }
}
