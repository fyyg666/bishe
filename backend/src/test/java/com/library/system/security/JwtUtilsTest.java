package com.library.system.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT工具类单元测试
 *
 * @author Library Team
 * @version 2.0.0
 */
@SpringBootTest(classes = JwtUtils.class)
class JwtUtilsTest {

    @Autowired
    private JwtUtils jwtUtils;

    private static final String TEST_SECRET = "test-secret-key-that-is-at-least-32-characters-long";
    private static final long ACCESS_EXPIRATION = 7200000L; // 2小时
    private static final long REFRESH_EXPIRATION = 604800000L; // 7天

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtils, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtils, "accessExpiration", ACCESS_EXPIRATION);
        ReflectionTestUtils.setField(jwtUtils, "refreshExpiration", REFRESH_EXPIRATION);
        ReflectionTestUtils.setField(jwtUtils, "issuer", "library-system");
    }

    @Test
    void testGenerateAccessToken() {
        String token = jwtUtils.generateAccessToken(1L, "testuser", "ADMIN");

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void testGenerateRefreshToken() {
        String token = jwtUtils.generateRefreshToken(1L, "testuser", "ADMIN");

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void testValidateToken_ValidToken() {
        String token = jwtUtils.generateAccessToken(1L, "testuser", "ADMIN");

        assertTrue(jwtUtils.validateToken(token));
    }

    @Test
    void testValidateToken_InvalidToken() {
        assertFalse(jwtUtils.validateToken("invalid.token.here"));
    }

    @Test
    void testParseToken() {
        String token = jwtUtils.generateAccessToken(1L, "testuser", "ADMIN");
        
        Claims claims = jwtUtils.parseToken(token);

        assertNotNull(claims);
        assertEquals("1", claims.getSubject());
        assertEquals("testuser", claims.get("username"));
        assertEquals("ADMIN", claims.get("role"));
    }

    @Test
    void testGetUserIdFromToken() {
        String token = jwtUtils.generateAccessToken(123L, "testuser", "READER");

        Long userId = jwtUtils.getUserIdFromToken(token);

        assertEquals(123L, userId);
    }

    @Test
    void testGetUsernameFromToken() {
        String token = jwtUtils.generateAccessToken(1L, "testuser", "ADMIN");

        String username = jwtUtils.getUsernameFromToken(token);

        assertEquals("testuser", username);
    }

    @Test
    void testGetRoleFromToken() {
        String token = jwtUtils.generateAccessToken(1L, "testuser", "LIBRARIAN");

        String role = jwtUtils.getRoleFromToken(token);

        assertEquals("LIBRARIAN", role);
    }

    @Test
    void testGetAccessExpirationSeconds() {
        long seconds = jwtUtils.getAccessExpirationSeconds();

        assertEquals(ACCESS_EXPIRATION / 1000, seconds);
    }

    @Test
    void testDifferentRoles() {
        // 测试不同角色
        String adminToken = jwtUtils.generateAccessToken(1L, "admin", "ADMIN");
        String librarianToken = jwtUtils.generateAccessToken(2L, "librarian", "LIBRARIAN");
        String readerToken = jwtUtils.generateAccessToken(3L, "reader", "READER");
        String volunteerToken = jwtUtils.generateAccessToken(4L, "volunteer", "VOLUNTEER");

        assertEquals("ADMIN", jwtUtils.getRoleFromToken(adminToken));
        assertEquals("LIBRARIAN", jwtUtils.getRoleFromToken(librarianToken));
        assertEquals("READER", jwtUtils.getRoleFromToken(readerToken));
        assertEquals("VOLUNTEER", jwtUtils.getRoleFromToken(volunteerToken));
    }
}
