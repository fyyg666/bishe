package com.library.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.system.dto.PageResult;
import com.library.system.dto.ReaderResponse;
import com.library.system.entity.User;
import com.library.system.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 读者控制器集成测试
 *
 * @author Library Team
 * @version 2.0.0
 */
@WebMvcTest(ReaderController.class)
class ReaderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private User testReader;
    private ReaderResponse testReaderResponse;

    @BeforeEach
    void setUp() {
        // 初始化测试读者
        testReader = new User();
        testReader.setId(1L);
        testReader.setUsername("reader1");
        testReader.setPassword("encoded_password");
        testReader.setRealName("张三");
        testReader.setPhone("13800138000");
        testReader.setEmail("reader@example.com");
        testReader.setRole("READER");
        testReader.setStatus("NORMAL");
        testReader.setCreditScore(100);
        testReader.setBorrowCount(2);
        testReader.setMaxBorrowCount(5);
        testReader.setCardNumber("RD2024010001");
        testReader.setDeleted(0);

        // 初始化测试读者响应
        testReaderResponse = ReaderResponse.builder()
                .id(1L)
                .username("reader1")
                .realName("张三")
                .phone("13800138000")
                .email("reader@example.com")
                .role("READER")
                .status("NORMAL")
                .creditScore(100)
                .cardNumber("RD2024010001")
                .borrowCount(2)
                .maxBorrowCount(5)
                .build();
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testListReaders_Success() throws Exception {
        List<ReaderResponse> readers = Arrays.asList(testReaderResponse);
        PageResult<ReaderResponse> pageResult = PageResult.of(1L, 10L, 1L, readers);

        mockMvc.perform(get("/readers")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testListReaders_WithKeyword() throws Exception {
        List<ReaderResponse> readers = Arrays.asList(testReaderResponse);
        PageResult<ReaderResponse> pageResult = PageResult.of(1L, 10L, 1L, readers);

        mockMvc.perform(get("/readers")
                        .param("current", "1")
                        .param("size", "10")
                        .param("keyword", "张三"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testGetReaderById_Success() throws Exception {
        when(userMapper.selectById(1L)).thenReturn(testReader);

        mockMvc.perform(get("/readers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("reader1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetReaderById_NotFound() throws Exception {
        when(userMapper.selectById(999L)).thenReturn(null);

        mockMvc.perform(get("/readers/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetCurrentReader_Success() throws Exception {
        when(userMapper.selectByUsername("user1")).thenReturn(testReader);

        mockMvc.perform(get("/readers/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testRegisterReader_Success() throws Exception {
        when(userMapper.selectByUsername("newreader")).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        mockMvc.perform(post("/readers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newreader\",\"password\":\"password123\",\"realName\":\"新用户\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testRegisterReader_UsernameExists() throws Exception {
        when(userMapper.selectByUsername("existinguser")).thenReturn(testReader);

        mockMvc.perform(post("/readers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"existinguser\",\"password\":\"password123\",\"realName\":\"新用户\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testUpdateReader_Success() throws Exception {
        when(userMapper.selectByUsername("user1")).thenReturn(testReader);
        when(userMapper.selectById(1L)).thenReturn(testReader);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        mockMvc.perform(put("/readers/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"realName\":\"李四\",\"phone\":\"13900139000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testUpdateReader_AdminCanUpdateRole() throws Exception {
        when(userMapper.selectByUsername("librarian")).thenReturn(testReader);
        when(userMapper.selectById(1L)).thenReturn(testReader);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        mockMvc.perform(put("/readers/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"VOLUNTEER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testChangePassword_Success() throws Exception {
        when(userMapper.selectByUsername("user1")).thenReturn(testReader);
        when(userMapper.selectById(1L)).thenReturn(testReader);
        when(passwordEncoder.matches("old_password", "encoded_password")).thenReturn(true);
        when(passwordEncoder.encode("new_password")).thenReturn("new_encoded");
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        mockMvc.perform(post("/readers/1/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"old_password\",\"newPassword\":\"new_password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testChangePassword_WrongOldPassword() throws Exception {
        when(userMapper.selectByUsername("user1")).thenReturn(testReader);
        when(userMapper.selectById(1L)).thenReturn(testReader);
        when(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false);

        mockMvc.perform(post("/readers/1/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"wrong_password\",\"newPassword\":\"new_password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testDeleteReader_Success() throws Exception {
        testReader.setBorrowCount(0);
        when(userMapper.selectById(1L)).thenReturn(testReader);
        when(userMapper.deleteById(1L)).thenReturn(1);

        mockMvc.perform(delete("/readers/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testDeleteReader_HasUnreturnedBooks() throws Exception {
        testReader.setBorrowCount(2);
        when(userMapper.selectById(1L)).thenReturn(testReader);

        mockMvc.perform(delete("/readers/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testResetPassword_Success() throws Exception {
        when(userMapper.selectById(1L)).thenReturn(testReader);
        when(passwordEncoder.encode("123456")).thenReturn("encoded_123456");
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        mockMvc.perform(post("/readers/1/reset-password")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testUpdateReaderStatus_Disable() throws Exception {
        when(userMapper.selectById(1L)).thenReturn(testReader);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        mockMvc.perform(post("/readers/1/status")
                        .with(csrf())
                        .param("disabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
