package com.system.booking.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String testSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long testExpirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(testSecret, testExpirationMs);
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
        boolean isValid = jwtTokenProvider.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should return false for invalid token signature")
    void validateToken_InvalidSignature_ReturnsFalse() {
        JwtTokenProvider anotherProvider = new JwtTokenProvider(
                "884E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
                testExpirationMs
        );

        String token = anotherProvider.generateToken("test_user", "ROLE_USER");
        boolean isValid = jwtTokenProvider.validateToken(token);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should return false for malformed token string")
    void validateToken_MalformedToken_ReturnsFalse() {
        boolean isValid = jwtTokenProvider.validateToken("not.a.valid.jwt.token");
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should return false for empty or null token")
    void validateToken_EmptyToken_ReturnsFalse() {
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken(null));
    }
}
