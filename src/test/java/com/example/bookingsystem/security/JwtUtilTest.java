package com.example.bookingsystem.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-only-secret-key-that-is-at-least-32-characters-long");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3600000L);
        jwtUtil.init();
    }

    private UserDetails userDetails(String username) {
        return new User(username, "password", Collections.emptyList());
    }

    @Test
    void generateToken_thenExtractUsername_roundTrips() {
        UserDetails user = userDetails("alice");
        String token = jwtUtil.generateToken(user, Map.of("role", "USER"));

        assertNotNull(token);
        assertEquals("alice", jwtUtil.extractUsername(token));
    }

    @Test
    void isTokenValid_forCorrectUser_returnsTrue() {
        UserDetails user = userDetails("alice");
        String token = jwtUtil.generateToken(user, Map.of("role", "USER"));

        assertTrue(jwtUtil.isTokenValid(token, user));
    }

    @Test
    void isTokenValid_forDifferentUser_returnsFalse() {
        UserDetails user = userDetails("alice");
        UserDetails otherUser = userDetails("bob");
        String token = jwtUtil.generateToken(user, Map.of("role", "USER"));

        assertFalse(jwtUtil.isTokenValid(token, otherUser));
    }

    @Test
    void isTokenValid_forExpiredToken_returnsFalse() {
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", -1000L); // already expired
        UserDetails user = userDetails("alice");
        String token = jwtUtil.generateToken(user, Map.of("role", "USER"));

        assertFalse(jwtUtil.isTokenValid(token, user));
    }

    @Test
    void init_withShortSecret_throwsException() {
        JwtUtil shortSecretUtil = new JwtUtil();
        ReflectionTestUtils.setField(shortSecretUtil, "secret", "too-short");
        assertThrows(IllegalStateException.class, shortSecretUtil::init);
    }
}
