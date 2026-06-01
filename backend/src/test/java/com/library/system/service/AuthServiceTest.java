package com.library.system.service;

import com.library.system.base.BaseTest;
import com.library.system.dto.LoginRequest;
import com.library.system.dto.RegisterRequest;
import com.library.system.dto.LoginResponse;
import com.library.system.entity.User;
import com.library.system.exception.BusinessException;
import com.library.system.mapper.UserMapper;
import com.library.system.service.impl.AuthServiceImpl;
import com.library.system.util.JwtUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("AuthService 单元测试")
class AuthServiceTest extends BaseTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AccountLockService accountLockService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        // 准备测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword123");
        testUser.setRealName("测试用户");
        testUser.setRole("READER");
        testUser.setStatus("NORMAL");
        testUser.setCreditScore(100);
        testUser.setDeleted(0);

        lenient().when(redisTemplate.opsForValue()).thenReturn(mock(org.springframework.data.redis.core.ValueOperations.class));
        lenient().when(redisTemplate.opsForValue().get(anyString())).thenReturn("1234");

        // 登录请求
        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");
        loginRequest.setCaptchaKey("test-captcha-key");
        loginRequest.setCaptchaCode("1234");

        // 注册请求
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setPassword("Pass@1234");
        registerRequest.setConfirmPassword("Pass@1234");
        registerRequest.setRealName("新用户");
        registerRequest.setPhone("13800138000");
        registerRequest.setEmail("new@test.com");
    }

    @Nested
    @DisplayName("登录用例")
    class LoginTests {

        @Test
        @DisplayName("正常登录 - 应返回 Token")
        void login_success_shouldReturnToken() {
            // 准备
            lenient().when(accountLockService.isLocked(1L)).thenReturn(false);
            when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
            when(passwordEncoder.matches("password123", "encodedPassword123")).thenReturn(true);
            when(jwtUtils.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("access-token");
            when(jwtUtils.generateRefreshToken(anyLong(), anyString())).thenReturn("refresh-token");

            // 执行
            LoginResponse response = authService.login(loginRequest);

            // 验证
            assertNotNull(response);
            assertNotNull(response.getAccessToken());
            assertEquals("access-token", response.getAccessToken());
            assertNotNull(response.getRefreshToken());
            assertEquals("refresh-token", response.getRefreshToken());
            assertNotNull(response.getUserInfo());
            assertEquals("testuser", response.getUserInfo().getUsername());
            assertEquals("READER", response.getUserInfo().getRole());

            verify(accountLockService).clearLoginFailures(1L);
        }

        @Test
        @DisplayName("账户锁定 - 应抛出异常")
        void login_whenAccountLocked_shouldThrowException() {
            when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
            when(accountLockService.isLocked(1L)).thenReturn(true);
            when(accountLockService.getRemainingLockSeconds(1L)).thenReturn(300L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.login(loginRequest));
            assertTrue(ex.getMessage().contains("锁定"));
        }

        @Test
        @DisplayName("密码错误 - 应记录失败次数")
        void login_withWrongPassword_shouldRecordFailure() {
            when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
            when(passwordEncoder.matches("wrongPassword", "encodedPassword123")).thenReturn(false);

            loginRequest.setPassword("wrongPassword");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.login(loginRequest));
            assertTrue(ex.getMessage().contains("密码"));

            verify(accountLockService).recordLoginFailure(1L, "testuser");
        }

        @Test
        @DisplayName("用户不存在 - 应抛出异常")
        void login_withNonExistentUser_shouldThrowException() {
            when(userMapper.selectByUsername("nonexist")).thenReturn(null);

            loginRequest.setUsername("nonexist");

            assertThrows(BusinessException.class, () -> authService.login(loginRequest));
        }

        @Test
        @DisplayName("账户被禁用 - 应抛出异常")
        void login_whenUserDisabled_shouldThrowException() {
            testUser.setStatus("DISABLED");
            when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
            lenient().when(accountLockService.isLocked(1L)).thenReturn(false);

            assertThrows(BusinessException.class, () -> authService.login(loginRequest));
        }

        @Test
        @DisplayName("连续5次密码错误后账户锁定")
        void login_whenMaxFailAttempts_shouldLockAccount() {
            when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
            when(accountLockService.recordLoginFailure(1L, "testuser")).thenReturn(5);
            loginRequest.setPassword("wrongPassword");
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.login(loginRequest));
            assertTrue(ex.getMessage().contains("锁定"));
        }
    }

    @Nested
    @DisplayName("注册用例")
    class RegisterTests {

        @Test
        @DisplayName("正常注册 - 应创建用户并返回信息")
        void register_success_shouldCreateUser() {
            when(userMapper.selectCount(any())).thenReturn(0L);
            when(passwordEncoder.encode("Pass@1234")).thenReturn("encoded-new-password");
            when(userMapper.insert(any(User.class))).thenReturn(1);

            LoginResponse.UserInfo info = authService.register(registerRequest);

            assertNotNull(info);
            assertEquals("newuser", info.getUsername());

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userMapper).insert(captor.capture());
            User savedUser = captor.getValue();
            assertEquals("newuser", savedUser.getUsername());
            assertEquals("encoded-new-password", savedUser.getPassword());
            assertEquals("READER", savedUser.getRole());
            assertEquals(100, savedUser.getCreditScore());
        }

        @Test
        @DisplayName("用户名已存在 - 应抛出异常")
        void register_withDuplicateUsername_shouldThrowException() {
            when(userMapper.selectCount(any())).thenReturn(1L);

            assertThrows(BusinessException.class, () -> authService.register(registerRequest));
            verify(userMapper, never()).insert(any());
        }
    }

    @Nested
    @DisplayName("密码修改用例")
    class ChangePasswordTests {

        @Test
        @DisplayName("修改密码成功")
        void changePassword_success() {
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(passwordEncoder.matches("oldPass", "encodedPassword123")).thenReturn(true);
            when(passwordEncoder.encode("newPass123")).thenReturn("newEncodedPass");

            authService.changePassword(1L, "oldPass", "newPass123");

            verify(userMapper).updateById(any(User.class));
        }

        @Test
        @DisplayName("旧密码错误 - 应抛出异常")
        void changePassword_withWrongOldPassword_shouldThrowException() {
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(passwordEncoder.matches("wrongOld", "encodedPassword123")).thenReturn(false);

            assertThrows(BusinessException.class,
                    () -> authService.changePassword(1L, "wrongOld", "newPass"));
        }
    }

    @Nested
    @DisplayName("Token 验证用例")
    class TokenTests {

        @Test
        @DisplayName("Token 有效 - 应返回 true")
        void validateToken_valid_shouldReturnTrue() {
            when(jwtUtils.validateToken("valid-token")).thenReturn(true);
            assertTrue(authService.validateToken("valid-token"));
        }

        @Test
        @DisplayName("Token 无效 - 应返回 false")
        void validateToken_invalid_shouldReturnFalse() {
            when(jwtUtils.validateToken("invalid-token")).thenReturn(false);
            assertFalse(authService.validateToken("invalid-token"));
        }
    }

    @Nested
    @DisplayName("Token刷新用例")
    class RefreshTokenTests {

        @Test
        @DisplayName("refreshToken - 有效刷新令牌应返回新令牌")
        void refreshToken_valid_shouldReturnNewToken() {
            lenient().when(jwtUtils.getUsernameFromToken("valid-refresh")).thenReturn("testuser");
            lenient().when(jwtUtils.getRoleFromToken("valid-refresh")).thenReturn("READER");
            when(jwtUtils.validateToken("valid-refresh")).thenReturn(true);
            when(jwtUtils.getUserIdFromToken("valid-refresh")).thenReturn(1L);
            when(jwtUtils.generateAccessToken(1L, "testuser", "READER")).thenReturn("new-access-token");
            when(jwtUtils.generateRefreshToken(1L, "testuser")).thenReturn("new-refresh-token");
            when(userMapper.selectById(1L)).thenReturn(testUser);

            LoginResponse response = authService.refreshToken("valid-refresh");

            assertNotNull(response);
            assertEquals("new-access-token", response.getAccessToken());
            assertEquals("new-refresh-token", response.getRefreshToken());
        }

        @Test
        @DisplayName("refreshToken - 无效刷新令牌抛异常")
        void refreshToken_invalid_shouldThrow() {
            when(jwtUtils.validateToken("invalid-refresh")).thenReturn(false);

            assertThrows(BusinessException.class,
                    () -> authService.refreshToken("invalid-refresh"));
        }

        @Test
        @DisplayName("refreshToken - 用户不存在抛异常")
        void refreshToken_userNotFound_shouldThrow() {
            when(jwtUtils.validateToken("valid-refresh")).thenReturn(true);
            when(jwtUtils.getUserIdFromToken("valid-refresh")).thenReturn(999L);
            when(userMapper.selectById(999L)).thenReturn(null);

            assertThrows(BusinessException.class,
                    () -> authService.refreshToken("valid-refresh"));
        }
    }

    @Nested
    @DisplayName("登出用例")
    class LogoutTests {

        @Test
        @DisplayName("logout - 不应抛出异常")
        void logout_shouldNotThrow() {
            assertDoesNotThrow(() -> authService.logout("some-token"));
        }
    }

    @Nested
    @DisplayName("当前用户查询用例")
    class GetCurrentUserTests {

        @Test
        @DisplayName("getCurrentUser - 用户存在应返回用户信息")
        void getCurrentUser_userExists_shouldReturnUserInfo() {
            when(userMapper.selectById(1L)).thenReturn(testUser);

            var userInfo = authService.getCurrentUser(1L);

            assertNotNull(userInfo);
            assertEquals("testuser", userInfo.getUsername());
        }

        @Test
        @DisplayName("getCurrentUser - 用户不存在应抛异常")
        void getCurrentUser_userNotFound_shouldThrow() {
            when(userMapper.selectById(999L)).thenReturn(null);

            assertThrows(BusinessException.class,
                    () -> authService.getCurrentUser(999L));
        }
    }
}
