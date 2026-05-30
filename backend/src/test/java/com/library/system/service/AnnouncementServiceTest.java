package com.library.system.service;

import com.library.system.base.BaseTest;
import com.library.system.dto.AnnouncementRequest;
import com.library.system.dto.AnnouncementResponse;
import com.library.system.dto.PageResult;
import com.library.system.entity.Announcement;
import com.library.system.entity.User;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.AnnouncementMapper;
import com.library.system.mapper.UserMapper;
import com.library.system.service.impl.AnnouncementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("AnnouncementService 单元测试")
class AnnouncementServiceTest extends BaseTest {

    @Mock
    private AnnouncementMapper announcementMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AnnouncementServiceImpl announcementService;

    private Announcement testAnnouncement;
    private User testPublisher;

    @BeforeEach
    void setUp() {
        testPublisher = new User();
        testPublisher.setId(1L);
        testPublisher.setUsername("admin");
        testPublisher.setRealName("管理员");

        testAnnouncement = new Announcement();
        testAnnouncement.setId(1L);
        testAnnouncement.setTitle("系统升级通知");
        testAnnouncement.setContent("系统将于本周日进行升级维护");
        testAnnouncement.setStatus("DRAFT");
        testAnnouncement.setPublisherId(1L);
        testAnnouncement.setDeleted(0);
        testAnnouncement.setCreateTime(LocalDateTime.now());

        lenient().when(userMapper.selectById(1L)).thenReturn(testPublisher);
        lenient().when(userMapper.selectByUsername("admin")).thenReturn(testPublisher);
    }

    @Nested
    @DisplayName("公告查询")
    class QueryTests {
        @Test
        void listAnnouncements_shouldReturnPage() {
            when(announcementMapper.selectPage(any(), any())).thenAnswer(inv -> {
                com.baomidou.mybatisplus.core.metadata.IPage<Announcement> p = inv.getArgument(0);
                p.setRecords(Arrays.asList(testAnnouncement));
                p.setTotal(1);
                return p;
            });
            PageResult<?> result = announcementService.listAnnouncements(1L, 10L, null, null);
            assertEquals(1, result.getTotal());
        }

        @Test
        void getAnnouncementById_exists() {
            when(announcementMapper.selectById(1L)).thenReturn(testAnnouncement);
            assertNotNull(announcementService.getAnnouncementById(1L));
        }

        @Test
        void getAnnouncementById_notExists() {
            when(announcementMapper.selectById(999L)).thenReturn(null);
            assertThrows(ResourceNotFoundException.class, () -> announcementService.getAnnouncementById(999L));
        }

        @Test
        void getLatestAnnouncements() {
            when(announcementMapper.selectList(any())).thenReturn(Arrays.asList(testAnnouncement));
            var list = announcementService.getLatestAnnouncements(5);
            assertFalse(list.isEmpty());
        }
    }

    @Nested
    @DisplayName("公告管理")
    class ManagementTests {
        @Test
        void createAnnouncement_success() {
            when(announcementMapper.insert(any(Announcement.class))).thenReturn(1);

            AnnouncementResponse resp = announcementService.createAnnouncement(new AnnouncementRequest(), "admin");
            assertNotNull(resp);
        }

        @Test
        void deleteAnnouncement_success() {
            when(announcementMapper.selectById(1L)).thenReturn(testAnnouncement);
            announcementService.deleteAnnouncement(1L);
            verify(announcementMapper).deleteById(1L);
        }

        @Test
        void publishAnnouncement_success() {
            when(announcementMapper.selectById(1L)).thenReturn(testAnnouncement);
            announcementService.publishAnnouncement(1L);
            verify(announcementMapper).updateById(argThat(a -> "PUBLISHED".equals(a.getStatus())));
        }
    }
}
