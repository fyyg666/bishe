package com.library.system.controller;

import com.library.system.dto.AnnouncementRequest;
import com.library.system.dto.AnnouncementResponse;
import com.library.system.dto.PageResult;
import com.library.system.entity.Announcement;
import com.library.system.entity.User;
import com.library.system.mapper.AnnouncementMapper;
import com.library.system.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 公告控制器集成测试
 *
 * @author Library Team
 * @version 2.0.0
 */
@WebMvcTest(AnnouncementController.class)
class AnnouncementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnnouncementMapper announcementMapper;

    @MockBean
    private UserMapper userMapper;

    private Announcement testAnnouncement;
    private AnnouncementResponse testAnnouncementResponse;
    private User testUser;

    @BeforeEach
    void setUp() {
        // 初始化测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("admin");
        testUser.setRealName("管理员");

        // 初始化测试公告
        testAnnouncement = new Announcement();
        testAnnouncement.setId(1L);
        testAnnouncement.setTitle("图书馆开放时间调整通知");
        testAnnouncement.setContent("自2024年1月1日起，图书馆开放时间调整为9:00-21:00");
        testAnnouncement.setType("NOTICE");
        testAnnouncement.setPriority(1);
        testAnnouncement.setPublisherId(1L);
        testAnnouncement.setStatus("PUBLISHED");
        testAnnouncement.setPublishTime(LocalDateTime.now());
        testAnnouncement.setDeleted(0);

        // 初始化测试公告响应
        testAnnouncementResponse = AnnouncementResponse.builder()
                .id(1L)
                .title("图书馆开放时间调整通知")
                .content("自2024年1月1日起，图书馆开放时间调整为9:00-21:00")
                .type("NOTICE")
                .priority(1)
                .publisherId(1L)
                .publisherName("管理员")
                .status("PUBLISHED")
                .publishTime(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    void testListAnnouncements_Success() throws Exception {
        List<AnnouncementResponse> announcements = Arrays.asList(testAnnouncementResponse);
        PageResult<AnnouncementResponse> pageResult = PageResult.of(1L, 10L, 1L, announcements);

        mockMvc.perform(get("/announcements")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetAnnouncementById_Success() throws Exception {
        when(announcementMapper.selectById(1L)).thenReturn(testAnnouncement);
        when(userMapper.selectById(1L)).thenReturn(testUser);

        mockMvc.perform(get("/announcements/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("图书馆开放时间调整通知"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetAnnouncementById_NotFound() throws Exception {
        when(announcementMapper.selectById(999L)).thenReturn(null);

        mockMvc.perform(get("/announcements/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetLatestAnnouncements_Success() throws Exception {
        when(announcementMapper.selectList(any())).thenReturn(Arrays.asList(testAnnouncement));

        mockMvc.perform(get("/announcements/latest")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testCreateAnnouncement_Success() throws Exception {
        AnnouncementRequest request = AnnouncementRequest.builder()
                .title("新公告")
                .content("公告内容")
                .type("NOTICE")
                .priority(0)
                .build();

        when(userMapper.selectByUsername("librarian")).thenReturn(testUser);
        when(announcementMapper.insert(any(Announcement.class))).thenReturn(1);

        mockMvc.perform(post("/announcements")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"新公告\",\"content\":\"公告内容\",\"type\":\"NOTICE\",\"priority\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testCreateAnnouncement_Forbidden() throws Exception {
        mockMvc.perform(post("/announcements")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"新公告\",\"content\":\"公告内容\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testUpdateAnnouncement_Success() throws Exception {
        when(announcementMapper.selectById(1L)).thenReturn(testAnnouncement);
        when(announcementMapper.updateById(any(Announcement.class))).thenReturn(1);

        mockMvc.perform(put("/announcements/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"更新后的标题\",\"content\":\"更新后的内容\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testPublishAnnouncement_Success() throws Exception {
        when(announcementMapper.selectById(1L)).thenReturn(testAnnouncement);
        when(announcementMapper.updateById(any(Announcement.class))).thenReturn(1);

        mockMvc.perform(post("/announcements/1/publish")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testDeleteAnnouncement_Success() throws Exception {
        when(announcementMapper.selectById(1L)).thenReturn(testAnnouncement);
        when(announcementMapper.deleteById(1L)).thenReturn(1);

        mockMvc.perform(delete("/announcements/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
