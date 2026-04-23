package com.library.system.service;

import com.library.system.dto.VolunteerRequest;
import com.library.system.dto.VolunteerResponse;
import com.library.system.entity.Volunteer;
import com.library.system.mapper.VolunteerMapper;
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
 * 志愿服务服务单元测试
 *
 * @author Library Team
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class VolunteerServiceTest {

    @Mock
    private VolunteerMapper volunteerMapper;

    @InjectMocks
    private VolunteerServiceImpl volunteerService;

    private Volunteer testVolunteer;
    private VolunteerRequest testRequest;

    @BeforeEach
    void setUp() {
        testVolunteer = new Volunteer();
        testVolunteer.setId(1L);
        testVolunteer.setReaderId(1L);
        testVolunteer.setReaderName("张三");
        testVolunteer.setActivityName("图书整理");
        testVolunteer.setActivityDesc("整理图书馆书架");
        testVolunteer.setStartTime(LocalDateTime.now().plusDays(1));
        testVolunteer.setEndTime(LocalDateTime.now().plusDays(1).plusHours(3));
        testVolunteer.setStatus(0);  // 待审核
        testVolunteer.setServiceHours(3.0);

        testRequest = new VolunteerRequest();
        testRequest.setActivityName("新志愿活动");
        testRequest.setActivityDesc("协助图书馆管理");
        testRequest.setStartTime(LocalDateTime.now().plusDays(2));
        testRequest.setEndTime(LocalDateTime.now().plusDays(2).plusHours(4));
    }

    @Test
    void testApplyVolunteer_Success() {
        when(volunteerMapper.insert(any(Volunteer.class))).thenReturn(1);

        VolunteerResponse result = volunteerService.applyVolunteer(1L, testRequest);

        assertNotNull(result);
        assertEquals("新志愿活动", result.getActivityName());
        assertEquals(0, result.getStatus());  // 待审核
        verify(volunteerMapper).insert(any(Volunteer.class));
    }

    @Test
    void testGetVolunteerById_Success() {
        when(volunteerMapper.selectById(1L)).thenReturn(testVolunteer);

        VolunteerResponse result = volunteerService.getVolunteerById(1L);

        assertNotNull(result);
        assertEquals("张三", result.getReaderName());
        assertEquals("图书整理", result.getActivityName());
        verify(volunteerMapper).selectById(1L);
    }

    @Test
    void testGetVolunteerById_NotFound() {
        when(volunteerMapper.selectById(999L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> volunteerService.getVolunteerById(999L));
        verify(volunteerMapper).selectById(999L);
    }

    @Test
    void testGetMyApplications_Success() {
        List<Volunteer> applications = Arrays.asList(testVolunteer);
        when(volunteerMapper.selectByReaderId(1L)).thenReturn(applications);

        List<VolunteerResponse> result = volunteerService.getMyApplications(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("图书整理", result.get(0).getActivityName());
        verify(volunteerMapper).selectByReaderId(1L);
    }

    @Test
    void testGetPendingApplications_Success() {
        List<Volunteer> pendingList = Arrays.asList(testVolunteer);
        when(volunteerMapper.selectByStatus(0)).thenReturn(pendingList);

        List<VolunteerResponse> result = volunteerService.getPendingApplications();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getStatus());
        verify(volunteerMapper).selectByStatus(0);
    }

    @Test
    void testApproveApplication_Success() {
        testVolunteer.setStatus(0);
        when(volunteerMapper.selectById(1L)).thenReturn(testVolunteer);
        when(volunteerMapper.updateById(any(Volunteer.class))).thenReturn(1);

        VolunteerResponse result = volunteerService.approveApplication(1L, 2L, "管理员");

        assertNotNull(result);
        assertEquals(1, result.getStatus());  // 已通过
        verify(volunteerMapper).updateById(any(Volunteer.class));
    }

    @Test
    void testApproveApplication_NotFound() {
        when(volunteerMapper.selectById(999L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> volunteerService.approveApplication(999L, 2L, "管理员"));
        verify(volunteerMapper, never()).updateById(any(Volunteer.class));
    }

    @Test
    void testRejectApplication_Success() {
        testVolunteer.setStatus(0);
        when(volunteerMapper.selectById(1L)).thenReturn(testVolunteer);
        when(volunteerMapper.updateById(any(Volunteer.class))).thenReturn(1);

        VolunteerResponse result = volunteerService.rejectApplication(1L, 2L, "管理员", "名额已满");

        assertNotNull(result);
        assertEquals(2, result.getStatus());  // 已拒绝
        verify(volunteerMapper).updateById(any(Volunteer.class));
    }

    @Test
    void testCompleteVolunteer_Success() {
        testVolunteer.setStatus(1);  // 已通过
        testVolunteer.setServiceHours(3.0);
        when(volunteerMapper.selectById(1L)).thenReturn(testVolunteer);
        when(volunteerMapper.updateById(any(Volunteer.class))).thenReturn(1);

        VolunteerResponse result = volunteerService.completeVolunteer(1L);

        assertNotNull(result);
        assertEquals(3, result.getStatus());  // 已完成
        verify(volunteerMapper).updateById(any(Volunteer.class));
    }

    @Test
    void testDeleteApplication_Success() {
        testVolunteer.setStatus(0);  // 只有待审核可以删除
        when(volunteerMapper.selectById(1L)).thenReturn(testVolunteer);
        doNothing().when(volunteerMapper).deleteById(anyLong());

        assertDoesNotThrow(() -> volunteerService.deleteApplication(1L, 1L));
        verify(volunteerMapper).deleteById(1L);
    }

    @Test
    void testDeleteApplication_NotOwner() {
        testVolunteer.setReaderId(2L);  // 不是当前用户
        when(volunteerMapper.selectById(1L)).thenReturn(testVolunteer);

        assertThrows(RuntimeException.class, () -> volunteerService.deleteApplication(1L, 1L));
        verify(volunteerMapper, never()).deleteById(anyLong());
    }

    @Test
    void testGetAllApplications_Success() {
        List<Volunteer> applications = Arrays.asList(testVolunteer);
        when(volunteerMapper.selectList(null)).thenReturn(applications);

        List<VolunteerResponse> result = volunteerService.getAllApplications();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(volunteerMapper).selectList(null);
    }

    @Test
    void testUpdateServiceHours_Success() {
        when(volunteerMapper.selectById(1L)).thenReturn(testVolunteer);
        when(volunteerMapper.updateById(any(Volunteer.class))).thenReturn(1);

        VolunteerResponse result = volunteerService.updateServiceHours(1L, 4.0);

        assertNotNull(result);
        verify(volunteerMapper).updateById(any(Volunteer.class));
    }

    @Test
    void testApplyVolunteer_ValidationError_EmptyActivityName() {
        VolunteerRequest invalidRequest = new VolunteerRequest();
        invalidRequest.setActivityName("");  // Empty name
        invalidRequest.setStartTime(LocalDateTime.now().plusDays(1));
        invalidRequest.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));

        // This should trigger validation error
        assertThrows(Exception.class, () -> volunteerService.applyVolunteer(1L, invalidRequest));
    }
}
