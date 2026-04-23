package com.library.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.system.dto.LoginRequest;
import com.library.system.dto.LoginResponse;
import com.library.system.dto.RefreshTokenRequest;
import com.library.system.dto.RegisterRequest;
import com.library.system.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 认证控制器集成测试
 *
 * @author Library Team
 * @version 2.0.0
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private LoginResponse testLoginResponse;
    private LoginResponse.UserInfo testUserInfo;

    @BeforeEach
    void setUp() {
        // 初始化测试用户信息
        testUserInfo = LoginResponse.UserInfo.builder()
                .id(1L)
                .username("testuser")
                .realName("张三")
                .role("READER")
                .cardNumber("R2024010001")
                .creditScore(100)
                .currentBorrows(0)
                .avatar("/avatars/default.png")
                .status(1)
                .phone("138****8000")
                .email("t***@example.com")
                .createTime("2024-01-01 10:00:00")
                .build();

        // 初始化测试登录响应
        testLoginResponse = LoginResponse.builder()
                .accessToken("test_access_token")
                .refreshToken("test_refresh_token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .userInfo(testUserInfo)
                .build();
    }

    @Test
    void testLogin_Success() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(testLoginResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data.accessToken").value("test_access_token"))
                .andExpect(jsonPath("$.data.refreshToken").value("test_refresh_token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.userInfo.username").value("testuser"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void testLogin_ValidationError_EmptyUsername() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("")
                .password("password123")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLogin_ValidationError_EmptyPassword() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLogin_WrongCredentials() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("wrongpassword")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("用户名或密码错误"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void testLogin_UserDisabled() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("disableduser")
                .password("password123")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("账户已被禁用"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void testRegister_Success() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("newuser")
                .password("password123")
                .realName("李四")
                .phone("13900139000")
                .email("new@example.com")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(testUserInfo);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("注册成功"))
                .andExpect(jsonPath("$.data.username").value("testuser"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void testRegister_ValidationError_EmptyUsername() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("")
                .password("password123")
                .realName("李四")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRegister_ValidationError_ShortPassword() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("newuser")
                .password("123")
                .realName("李四")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRegister_UsernameExists() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("existinguser")
                .password("password123")
                .realName("李四")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("用户名已存在"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void testRefreshToken_Success() throws Exception {
        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken("valid_refresh_token")
                .build();

        when(authService.refreshToken("valid_refresh_token")).thenReturn(testLoginResponse);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Token刷新成功"))
                .andExpect(jsonPath("$.data.accessToken").value("test_access_token"));

        verify(authService).refreshToken("valid_refresh_token");
    }

    @Test
    void testRefreshToken_InvalidToken() throws Exception {
        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken("invalid_refresh_token")
                .build();

        when(authService.refreshToken("invalid_refresh_token"))
                .thenThrow(new RuntimeException("Refresh Token无效"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void testLogout_Success() throws Exception {
        doNothing().when(authService).logout("valid_token");

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer valid_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("登出成功"));

        verify(authService).logout("valid_token");
    }

    @Test
    void testLogout_InvalidToken() throws Exception {
        doThrow(new RuntimeException("无效的Token")).when(authService).logout("invalid_token");

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "invalid_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("无效的Token"));
    }

    @Test
    void testLogout_NoAuthorizationHeader() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("无效的Token"));

        verify(authService, never()).logout(any());
    }

    @Test
    void testLogout_InvalidAuthorizationFormat() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Basic invalid_format"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("无效的Token"));
    }
}
