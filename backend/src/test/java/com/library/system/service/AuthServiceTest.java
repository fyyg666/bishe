package com.library.system.service;

import com.library.system.common.Constants;
import com.library.system.dto.LoginRequest;
import com.library.system.dto.LoginResponse;
import com.library.system.dto.RegisterRequest;
import com.library.system.entity.User;
import com.library.system.mapper.UserMapper;
import com.library.system.security.JwtUtils;
import com.library.system.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 认证服务单元测试
 *
 * @author Library Team
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private LoginRequest testLoginRequest;
    private RegisterRequest testRegisterRequest;

    @BeforeEach
    void setUp() {
        // 初始化测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encoded_password");
        testUser.setRealName("张三");
        testUser.setPhone("13800138000");
        testUser.setEmail("test@example.com");
        testUser.setRole("READER");
        testUser.setStatus("NORMAL");
        testUser.setCreditScore(100);
        testUser.setBorrowCount(0);
        testUser.setMaxBorrowCount(5);
        testUser.setCardNumber("R2024010001");
        testUser.setAvatar("/avatars/default.png");

        // 初始化登录请求
        testLoginRequest = new LoginRequest();
        testLoginRequest.setUsername("testuser");
        testLoginRequest.setPassword("password123");

        // 初始化注册请求
        testRegisterRequest = new RegisterRequest();
        testRegisterRequest.setUsername("newuser");
        testRegisterRequest.setPassword("password123");
        testRegisterRequest.setRealName("李四");
        testRegisterRequest.setPhone("13900139000");
        testRegisterRequest.setEmail("new@example.com");

        // Mock Redis
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testLogin_Success() {
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
        when(jwtUtils.generateAccessToken(anyLong(), anyString(), anyString()))
                .thenReturn("access_token");
        when(jwtUtils.generateRefreshToken(anyLong(), anyString(), anyString()))
                .thenReturn("refresh_token");
        when(jwtUtils.getAccessExpirationSeconds()).thenReturn(3600L);

        LoginResponse result = authService.login(testLoginRequest);

        assertNotNull(result);
        assertEquals("access_token", result.getAccessToken());
        assertEquals("refresh_token", result.getRefreshToken());
        assertEquals("Bearer", result.getTokenType());
        assertNotNull(result.getUserInfo());
        assertEquals("testuser", result.getUserInfo().getUsername());
        assertEquals("张三", result.getUserInfo().getRealName());

        verify(redisTemplate).delete("login:fail:1");
    }

    @Test
    void testLogin_UserNotFound() {
        when(userMapper.selectByUsername("testuser")).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.login(testLoginRequest));
        assertEquals("用户名或密码错误", exception.getMessage());
    }

    @Test
    void testLogin_WrongPassword() {
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.login(testLoginRequest));
        assertEquals("用户名或密码错误", exception.getMessage());

        verify(valueOperations).increment("login:fail:1");
    }

    @Test
    void testLogin_UserDisabled() {
        testUser.setStatus("DISABLED");
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.login(testLoginRequest));
        assertEquals("账户已被禁用", exception.getMessage());
    }

    @Test
    void testLogin_UserLocked() {
        testUser.setStatus("LOCKED");
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.login(testLoginRequest));
        assertEquals("账户已被锁定，请稍后再试", exception.getMessage());
    }

    @Test
    void testRegister_Success() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        LoginResponse.UserInfo result = authService.register(testRegisterRequest);

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals("李四", result.getRealName());
        assertEquals("READER", result.getRole());
        assertEquals(100, result.getCreditScore());

        verify(userMapper).insert(any(User.class));
    }

    @Test
    void testRegister_UsernameExists() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.register(testRegisterRequest));
        assertEquals("用户名已存在", exception.getMessage());
    }

    @Test
    void testRefreshToken_Success() {
        String refreshToken = "valid_refresh_token";
        when(jwtUtils.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtils.getUserIdFromToken(refreshToken)).thenReturn(1L);
        when(jwtUtils.getUsernameFromToken(refreshToken)).thenReturn("testuser");
        when(jwtUtils.getRoleFromToken(refreshToken)).thenReturn("READER");
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(jwtUtils.generateAccessToken(1L, "testuser", "READER"))
                .thenReturn("new_access_token");
        when(jwtUtils.generateRefreshToken(1L, "testuser", "READER"))
                .thenReturn("new_refresh_token");
        when(jwtUtils.getAccessExpirationSeconds()).thenReturn(3600L);

        LoginResponse result = authService.refreshToken(refreshToken);

        assertNotNull(result);
        assertEquals("new_access_token", result.getAccessToken());
        assertEquals("new_refresh_token", result.getRefreshToken());
    }

    @Test
    void testRefreshToken_InvalidToken() {
        when(jwtUtils.validateToken("invalid_token")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.refreshToken("invalid_token"));
        assertEquals("Refresh Token无效", exception.getMessage());
    }

    @Test
    void testRefreshToken_UserNotFound() {
        String refreshToken = "valid_refresh_token";
        when(jwtUtils.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtils.getUserIdFromToken(refreshToken)).thenReturn(999L);
        when(jwtUtils.getUsernameFromToken(refreshToken)).thenReturn("unknown");
        when(jwtUtils.getRoleFromToken(refreshToken)).thenReturn("READER");
        when(userMapper.selectById(999L)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.refreshToken(refreshToken));
        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    void testLogout_Success() {
        String token = "valid_token";
        
        // 使用 Mockito mock Claims
        io.jsonwebtoken.Claims mockedClaims = mock(io.jsonwebtoken.Claims.class);
        when(mockedClaims.getSubject()).thenReturn("testuser");
        when(mockedClaims.getExpiration()).thenReturn(
                new java.util.Date(System.currentTimeMillis() + 3600000));
        when(jwtUtils.parseToken(token)).thenReturn(mockedClaims);
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        assertDoesNotThrow(() -> authService.logout(token));

        verify(valueOperations).set(eq("token:blacklist:" + token), eq("1"), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    void testGetCurrentUser_Success() {
        when(userMapper.selectById(1L)).thenReturn(testUser);

        LoginResponse.UserInfo result = authService.getCurrentUser(1L);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("张三", result.getRealName());
    }

    @Test
    void testGetCurrentUser_NotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.getCurrentUser(999L));
        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    void testValidateToken_Valid() {
        when(jwtUtils.validateToken("valid_token")).thenReturn(true);

        boolean result = authService.validateToken("valid_token");

        assertTrue(result);
    }

    @Test
    void testValidateToken_Invalid() {
        when(jwtUtils.validateToken("invalid_token")).thenReturn(false);

        boolean result = authService.validateToken("invalid_token");

        assertFalse(result);
    }

    @Test
    void testValidateToken_Exception() {
        when(jwtUtils.validateToken("bad_token")).thenThrow(new RuntimeException("Token解析失败"));

        boolean result = authService.validateToken("bad_token");

        assertFalse(result);
    }

    @Test
    void testChangePassword_Success() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(passwordEncoder.matches("old_password", "encoded_password")).thenReturn(true);
        when(passwordEncoder.encode("new_password")).thenReturn("new_encoded_password");

        assertDoesNotThrow(() -> authService.changePassword(1L, "old_password", "new_password"));

        verify(userMapper).updateById(any(User.class));
    }

    @Test
    void testChangePassword_UserNotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.changePassword(999L, "old_password", "new_password"));
        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    void testChangePassword_WrongOldPassword() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.changePassword(1L, "wrong_password", "new_password"));
        assertEquals("原密码错误", exception.getMessage());
    }

    @Test
    void testLogin_AccountLockedAfterFailedAttempts() {
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(false);
        when(valueOperations.increment("login:fail:1")).thenReturn(5L);
        when(userMapper.selectById(1L)).thenReturn(testUser);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> authService.login(testLoginRequest));
        
        assertEquals("用户名或密码错误", exception.getMessage());
        
        // 验证账户被锁定
        verify(userMapper).updateById(any(User.class));
    }
}
