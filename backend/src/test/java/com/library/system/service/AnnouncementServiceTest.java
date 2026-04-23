package com.library.system.service;

import com.library.system.dto.AnnouncementRequest;
import com.library.system.entity.Announcement;
import com.library.system.mapper.AnnouncementMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 公告服务单元测试
 *
 * @author Library Team
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementMapper announcementMapper;

    @InjectMocks
    private AnnouncementServiceImpl announcementService;

    private Announcement testAnnouncement;
    private AnnouncementRequest testRequest;

    @BeforeEach
    void setUp() {
        testAnnouncement = new Announcement();
        testAnnouncement.setId(1L);
        testAnnouncement.setTitle("系统维护通知");
        testAnnouncement.setContent("系统将于今晚凌晨2点进行维护");
        testAnnouncement.setType(1);
        testAnnouncement.setPriority(2);
        testAnnouncement.setPublishTime(LocalDateTime.now());
        testAnnouncement.setPublisherId(1L);
        testAnnouncement.setPublisherName("管理员");
        testAnnouncement.setStatus(1);

        testRequest = new AnnouncementRequest();
        testRequest.setTitle("新公告");
        testRequest.setContent("这是一条新公告");
        testRequest.setType(1);
        testRequest.setPriority(1);
    }

    @Test
    void testGetAllAnnouncements_Success() {
        List<Announcement> announcements = Arrays.asList(testAnnouncement);
        when(announcementMapper.selectList(null)).thenReturn(announcements);

        List<Announcement> result = announcementService.getAllAnnouncements();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("系统维护通知", result.get(0).getTitle());
        verify(announcementMapper).selectList(null);
    }

    @Test
    void testGetAnnouncementById_Success() {
        when(announcementMapper.selectById(1L)).thenReturn(testAnnouncement);

        Announcement result = announcementService.getAnnouncementById(1L);

        assertNotNull(result);
        assertEquals("系统维护通知", result.getTitle());
        assertEquals(1, result.getType());
        verify(announcementMapper).selectById(1L);
    }

    @Test
    void testGetAnnouncementById_NotFound() {
        when(announcementMapper.selectById(999L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> announcementService.getAnnouncementById(999L));
        verify(announcementMapper).selectById(999L);
    }

    @Test
    void testCreateAnnouncement_Success() {
        when(announcementMapper.insert(any(Announcement.class))).thenReturn(1);

        Announcement result = announcementService.createAnnouncement(testRequest, 1L, "管理员");

        assertNotNull(result);
        assertEquals("新公告", result.getTitle());
        assertEquals(1, result.getType());
        assertEquals(1L, result.getPublisherId());
        verify(announcementMapper).insert(any(Announcement.class));
    }

    @Test
    void testUpdateAnnouncement_Success() {
        when(announcementMapper.selectById(1L)).thenReturn(testAnnouncement);
        when(announcementMapper.updateById(any(Announcement.class))).thenReturn(1);

        Announcement result = announcementService.updateAnnouncement(1L, testRequest);

        assertNotNull(result);
        verify(announcementMapper).updateById(any(Announcement.class));
    }

    @Test
    void testUpdateAnnouncement_NotFound() {
        when(announcementMapper.selectById(999L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> announcementService.updateAnnouncement(999L, testRequest));
        verify(announcementMapper, never()).updateById(any(Announcement.class));
    }

    @Test
    void testDeleteAnnouncement_Success() {
        when(announcementMapper.selectById(1L)).thenReturn(testAnnouncement);
        doNothing().when(announcementMapper).deleteById(anyLong());

        assertDoesNotThrow(() -> announcementService.deleteAnnouncement(1L));
        verify(announcementMapper).deleteById(1L);
    }

    @Test
    void testDeleteAnnouncement_NotFound() {
        when(announcementMapper.selectById(999L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> announcementService.deleteAnnouncement(999L));
        verify(announcementMapper, never()).deleteById(anyLong());
    }

    @Test
    void testGetAnnouncementsByType_Success() {
        List<Announcement> announcements = Arrays.asList(testAnnouncement);
        when(announcementMapper.selectByType(1)).thenReturn(announcements);

        List<Announcement> result = announcementService.getAnnouncementsByType(1);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getType());
        verify(announcementMapper).selectByType(1);
    }

    @Test
    void testPublishAnnouncement_Success() {
        when(announcementMapper.selectById(1L)).thenReturn(testAnnouncement);
        when(announcementMapper.updateById(any(Announcement.class))).thenReturn(1);

        Announcement result = announcementService.publishAnnouncement(1L);

        assertNotNull(result);
        assertEquals(1, result.getStatus());
        verify(announcementMapper).updateById(any(Announcement.class));
    }

    @Test
    void testUnpublishAnnouncement_Success() {
        testAnnouncement.setStatus(1);
        when(announcementMapper.selectById(1L)).thenReturn(testAnnouncement);
        when(announcementMapper.updateById(any(Announcement.class))).thenReturn(1);

        Announcement result = announcementService.unpublishAnnouncement(1L);

        assertNotNull(result);
        assertEquals(0, result.getStatus());
        verify(announcementMapper).updateById(any(Announcement.class));
    }

    @Test
    void testGetTopAnnouncements_Success() {
        List<Announcement> announcements = Arrays.asList(testAnnouncement);
        when(announcementMapper.selectTopAnnouncements(5)).thenReturn(announcements);

        List<Announcement> result = announcementService.getTopAnnouncements(5);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(announcementMapper).selectTopAnnouncements(5);
    }

    @Test
    void testCreateAnnouncement_ValidationError_EmptyTitle() {
        AnnouncementRequest invalidRequest = new AnnouncementRequest();
        invalidRequest.setTitle("");  // Empty title
        invalidRequest.setContent("Content");
        invalidRequest.setType(1);

        // This should trigger validation error
        assertThrows(Exception.class, () -> announcementService.createAnnouncement(invalidRequest, 1L, "管理员"));
    }
}
