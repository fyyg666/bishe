package com.library.system.controller;

import com.library.system.base.ControllerTestBase;
import com.library.system.dto.LoginRequest;
import com.library.system.dto.RefreshTokenRequest;
import com.library.system.dto.RegisterRequest;
import com.library.system.dto.LoginResponse;
import com.library.system.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@DisplayName("AuthController 安全测试")
class AuthControllerTest extends ControllerTestBase {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        initMockMvc(authController);
    }

    @Nested
    @DisplayName("登录接口")
    class LoginEndpoint {

        @Test
        @DisplayName("正常登录 - 返回 200 和 Token")
        void login_valid_shouldReturn200() throws Exception {
            LoginResponse response = new LoginResponse();
            response.setAccessToken("access-token");
            response.setRefreshToken("refresh-token");
            when(authService.login(any(LoginRequest.class))).thenReturn(response);

            String body = objectMapper.writeValueAsString(new LoginRequest() {{
                setUsername("admin");
                setPassword("admin123");
            }});

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andDo(print());
        }

        @Test
        @DisplayName("空用户名 - 返回 400")
        void login_emptyUsername_shouldReturn400() throws Exception {
            String body = objectMapper.writeValueAsString(new LoginRequest() {{
                setUsername("");
                setPassword("admin123");
            }});

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("空密码 - 返回 400")
        void login_emptyPassword_shouldReturn400() throws Exception {
            String body = objectMapper.writeValueAsString(new LoginRequest() {{
                setUsername("admin");
                setPassword("");
            }});

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("注册接口")
    class RegisterEndpoint {

        @Test
        @DisplayName("正常注册 - 返回 200")
        void register_valid_shouldReturn200() throws Exception {
            LoginResponse.UserInfo info = new LoginResponse.UserInfo();
            info.setUsername("newuser");
            when(authService.register(any(RegisterRequest.class))).thenReturn(info);

            String body = objectMapper.writeValueAsString(new RegisterRequest() {{
                setUsername("newuser");
                setPassword("Pass@1234");
                setRealName("新用户");
                setEmail("newuser@example.com");
                setConfirmPassword("Pass@1234");
            }});

            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Token 刷新接口")
    class RefreshEndpoint {

        @Test
        @DisplayName("携带有效 RefreshToken - 返回新 Token")
        void refresh_valid_shouldReturnNewToken() throws Exception {
            LoginResponse response = new LoginResponse();
            response.setAccessToken("new-access-token");
            when(authService.refreshToken("valid-refresh-token")).thenReturn(response);

            String body = objectMapper.writeValueAsString(new RefreshTokenRequest("valid-refresh-token"));
            mockMvc.perform(post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("空请求体 - 返回 400")
        void refresh_noToken_shouldReturn400() throws Exception {
            mockMvc.perform(post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
