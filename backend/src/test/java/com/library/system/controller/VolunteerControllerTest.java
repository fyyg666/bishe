package com.library.system.controller;

import com.library.system.dto.PageResult;
import com.library.system.dto.VolunteerRequest;
import com.library.system.dto.VolunteerResponse;
import com.library.system.entity.User;
import com.library.system.entity.VolunteerService;
import com.library.system.mapper.UserMapper;
import com.library.system.mapper.VolunteerServiceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 志愿服务控制器集成测试
 *
 * @author Library Team
 * @version 2.0.0
 */
@WebMvcTest(VolunteerController.class)
class VolunteerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VolunteerServiceMapper volunteerServiceMapper;

    @MockBean
    private UserMapper userMapper;

    private VolunteerService testVolunteer;
    private VolunteerResponse testVolunteerResponse;
    private User testUser;

    @BeforeEach
    void setUp() {
        // 初始化测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRealName("张三");
        testUser.setCreditScore(100);

        // 初始化测试志愿服务记录
        testVolunteer = new VolunteerService();
        testVolunteer.setId(1L);
        testVolunteer.setUserId(1L);
        testVolunteer.setServiceDate(LocalDateTime.now());
        testVolunteer.setStartTime(LocalDateTime.now());
        testVolunteer.setEndTime(LocalDateTime.now().plusHours(4));
        testVolunteer.setServiceHours(new BigDecimal("4.00"));
        testVolunteer.setServiceType("READING_ROOM");
        testVolunteer.setDescription("协助整理书架");
        testVolunteer.setStatus("PENDING");
        testVolunteer.setDeleted(0);

        // 初始化测试响应
        testVolunteerResponse = VolunteerResponse.builder()
                .id(1L)
                .userId(1L)
                .username("testuser")
                .realName("张三")
                .serviceDate(LocalDateTime.now())
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(4))
                .serviceHours(new BigDecimal("4.00"))
                .serviceType("READING_ROOM")
                .description("协助整理书架")
                .status("PENDING")
                .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    void testListVolunteers_Success() throws Exception {
        List<VolunteerResponse> volunteers = Arrays.asList(testVolunteerResponse);
        PageResult<VolunteerResponse> pageResult = PageResult.of(1L, 10L, 1L, volunteers);

        mockMvc.perform(get("/volunteers")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetMyVolunteers_Success() throws Exception {
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);

        mockMvc.perform(get("/volunteers/my")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetVolunteerById_Success() throws Exception {
        when(volunteerServiceMapper.selectById(1L)).thenReturn(testVolunteer);
        when(userMapper.selectById(1L)).thenReturn(testUser);

        mockMvc.perform(get("/volunteers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetVolunteerById_NotFound() throws Exception {
        when(volunteerServiceMapper.selectById(999L)).thenReturn(null);

        mockMvc.perform(get("/volunteers/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testCreateVolunteer_Success() throws Exception {
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
        when(volunteerServiceMapper.insert(any(VolunteerService.class))).thenReturn(1);

        mockMvc.perform(post("/volunteers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceType\":\"READING_ROOM\",\"description\":\"协助整理书架\",\"serviceHours\":4.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testCreateVolunteer_ServiceHoursExceedLimit() throws Exception {
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);

        mockMvc.perform(post("/volunteers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceType\":\"READING_ROOM\",\"description\":\"协助整理书架\",\"serviceHours\":15.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testCancelVolunteer_Success() throws Exception {
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
        when(volunteerServiceMapper.selectById(1L)).thenReturn(testVolunteer);
        when(volunteerServiceMapper.updateById(any(VolunteerService.class))).thenReturn(1);

        mockMvc.perform(post("/volunteers/1/cancel")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testReviewVolunteer_Approve() throws Exception {
        when(userMapper.selectByUsername("librarian")).thenReturn(testUser);
        when(volunteerServiceMapper.selectById(1L)).thenReturn(testVolunteer);
        when(volunteerServiceMapper.updateById(any(VolunteerService.class))).thenReturn(1);
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        mockMvc.perform(post("/volunteers/1/review")
                        .with(csrf())
                        .param("approved", "true")
                        .param("remark", "表现优秀"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testReviewVolunteer_Reject() throws Exception {
        when(userMapper.selectByUsername("librarian")).thenReturn(testUser);
        when(volunteerServiceMapper.selectById(1L)).thenReturn(testVolunteer);
        when(volunteerServiceMapper.updateById(any(VolunteerService.class))).thenReturn(1);

        mockMvc.perform(post("/volunteers/1/review")
                        .with(csrf())
                        .param("approved", "false")
                        .param("remark", "服务时长不足"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testReviewVolunteer_Forbidden() throws Exception {
        mockMvc.perform(post("/volunteers/1/review")
                        .with(csrf())
                        .param("approved", "true"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testGetPendingVolunteers_Success() throws Exception {
        mockMvc.perform(get("/volunteers/pending")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testDeleteVolunteer_Success() throws Exception {
        when(volunteerServiceMapper.selectById(1L)).thenReturn(testVolunteer);
        when(volunteerServiceMapper.deleteById(1L)).thenReturn(1);

        mockMvc.perform(delete("/volunteers/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetVolunteerStats_Success() throws Exception {
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
        when(volunteerServiceMapper.selectList(any())).thenReturn(Arrays.asList(testVolunteer));
        when(volunteerServiceMapper.selectCount(any())).thenReturn(0L);

        mockMvc.perform(get("/volunteers/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
