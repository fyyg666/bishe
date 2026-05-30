package com.library.system.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller 测试基类 - 使用 standalone MockMvc 设置，无需 Spring 上下文
 *
 * 子类在 @BeforeEach 中调用 initMockMvc(controller) 初始化 MockMvc。
 */
@ExtendWith(MockitoExtension.class)
public abstract class ControllerTestBase {

    protected MockMvc mockMvc;

    protected final ObjectMapper objectMapper;

    /** 管理员 JWT Token（测试用） */
    protected static final String ADMIN_TOKEN = "Bearer test-admin-jwt-token";

    /** 读者 JWT Token（测试用） */
    protected static final String READER_TOKEN = "Bearer test-reader-jwt-token";

    public ControllerTestBase() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 子类调用此方法初始化 standalone MockMvc
     */
    protected void initMockMvc(Object controller) {
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @BeforeEach
    void setUpBase() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDownBase() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 创建模拟读者认证的 RequestPostProcessor
     * 在 standalone MockMvc 中，通过设置 request UserPrincipal 来模拟 Authentication 参数
     */
    protected static org.springframework.test.web.servlet.request.RequestPostProcessor readerAuth() {
        return request -> {
            request.setUserPrincipal(new UsernamePasswordAuthenticationToken(
                    "1", null, List.of(new SimpleGrantedAuthority("ROLE_READER"))));
            return request;
        };
    }

    /**
     * 创建模拟管理员认证的 RequestPostProcessor
     */
    protected static org.springframework.test.web.servlet.request.RequestPostProcessor adminAuth() {
        return request -> {
            request.setUserPrincipal(new UsernamePasswordAuthenticationToken(
                    "1", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
            return request;
        };
    }

    // ============ 便捷请求方法 ============

    protected ResultActions performGet(String url) throws Exception {
        return mockMvc.perform(get(url)
                .accept(MediaType.APPLICATION_JSON));
    }

    protected ResultActions performGetWithAuth(String url, String token) throws Exception {
        return mockMvc.perform(get(url)
                .header("Authorization", token)
                .accept(MediaType.APPLICATION_JSON));
    }

    protected ResultActions performPost(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
                .accept(MediaType.APPLICATION_JSON));
    }

    protected ResultActions performPostWithAuth(String url, Object body, String token) throws Exception {
        return mockMvc.perform(post(url)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
                .accept(MediaType.APPLICATION_JSON));
    }

    protected ResultActions performPutWithAuth(String url, Object body, String token) throws Exception {
        return mockMvc.perform(put(url)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
                .accept(MediaType.APPLICATION_JSON));
    }

    protected ResultActions performDeleteWithAuth(String url, String token) throws Exception {
        return mockMvc.perform(delete(url)
                .header("Authorization", token)
                .accept(MediaType.APPLICATION_JSON));
    }

    // ============ 通用断言 ============

    protected void assertOkResponse(ResultActions result) throws Exception {
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    protected void assertUnauthorizedResponse(ResultActions result) throws Exception {
        result.andExpect(status().isUnauthorized());
    }

    protected void assertForbiddenResponse(ResultActions result) throws Exception {
        result.andExpect(status().isForbidden());
    }

    protected void assertBadRequestResponse(ResultActions result) throws Exception {
        result.andExpect(status().isBadRequest());
    }
}
