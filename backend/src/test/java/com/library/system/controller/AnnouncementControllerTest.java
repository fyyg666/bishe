package com.library.system.controller;

import com.library.system.base.ControllerTestBase;
import com.library.system.dto.AnnouncementRequest;
import com.library.system.service.AnnouncementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AnnouncementController 测试")
class AnnouncementControllerTest extends ControllerTestBase {

    @Mock
    private AnnouncementService announcementService;

    @InjectMocks
    private AnnouncementController announcementController;

    @BeforeEach
    void setUp() {
        initMockMvc(announcementController);
    }

    @Nested
    @DisplayName("公开接口")
    class PublicEndpoints {
        @Test
        void getLatestAnnouncements_shouldReturn200() throws Exception {
            mockMvc.perform(get("/announcements/latest"))
                    .andExpect(status().isOk());
        }

        @Test
        void getAnnouncementById_shouldReturn200() throws Exception {
            mockMvc.perform(get("/announcements/1"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("管理接口")
    class AdminEndpoints {
        @Test
        void createAnnouncement_shouldReturn200() throws Exception {
            String body = objectMapper.writeValueAsString(new AnnouncementRequest() {{
                setTitle("测试公告");
                setContent("测试内容");
                setType("NOTICE");
                setPriority(1);
            }});

            mockMvc.perform(post("/announcements")
                    .with(adminAuth())
                    .header("Authorization", ADMIN_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isOk());
        }

        @Test
        void deleteAnnouncement_shouldReturn200() throws Exception {
            mockMvc.perform(delete("/announcements/1"))
                    .andExpect(status().isOk());
        }
    }
}
