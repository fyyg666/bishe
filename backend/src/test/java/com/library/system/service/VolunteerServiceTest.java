package com.library.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.base.BaseTest;
import com.library.system.common.Constants;
import com.library.system.dto.PageResult;
import com.library.system.dto.VolunteerRequest;
import com.library.system.dto.VolunteerResponse;
import com.library.system.dto.VolunteerStatsDto;
import com.library.system.entity.User;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ForbiddenException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.UserMapper;
import com.library.system.mapper.VolunteerServiceMapper;
import com.library.system.service.impl.VolunteerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("VolunteerService 单元测试")
class VolunteerServiceTest extends BaseTest {

    @Mock
    private VolunteerServiceMapper volunteerServiceMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CreditService creditService;

    @InjectMocks
    private VolunteerServiceImpl volunteerService;

    private com.library.system.entity.VolunteerService testVolunteer;
    private User testUser;
    private VolunteerRequest testRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("volunteer1");
        testUser.setRealName("志愿者张三");
        testUser.setRole(Constants.Role.VOLUNTEER);

        testVolunteer = new com.library.system.entity.VolunteerService();
        testVolunteer.setId(100L);
        testVolunteer.setUserId(1L);
        testVolunteer.setServiceType("整理书籍");
        testVolunteer.setServiceHours(BigDecimal.valueOf(3));
        testVolunteer.setDescription("整理文学区图书");
        testVolunteer.setStatus("PENDING");
        testVolunteer.setDeleted(0);
        testVolunteer.setCreateTime(LocalDateTime.now().minusDays(1));
        testVolunteer.setServiceDate(LocalDateTime.now());
        testVolunteer.setStartTime(LocalDateTime.now().minusHours(3));
        testVolunteer.setEndTime(LocalDateTime.now());

        testRequest = VolunteerRequest.builder()
                .serviceDate(LocalDateTime.now())
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(2))
                .serviceHours(BigDecimal.valueOf(2))
                .serviceType("整理书籍")
                .description("志愿者服务测试")
                .build();
    }

    @Nested
    @DisplayName("志愿服务查询用例")
    class QueryTests {

        @Test
        @DisplayName("listVolunteers - 应返回分页列表（全部状态）")
        void listVolunteers_shouldReturnPage() {
            Page<com.library.system.entity.VolunteerService> page = new Page<>(1, 10);
            page.setRecords(List.of(testVolunteer));
            page.setTotal(1);
            when(volunteerServiceMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(page);
            doReturn(List.of(testUser)).when(userMapper).selectBatchIds(anyCollection());

            PageResult<VolunteerResponse> result = volunteerService.listVolunteers(1L, 10L, null);

            assertNotNull(result);
            assertEquals(1, result.getTotal());
            assertEquals(1, result.getRecords().size());
            assertEquals("volunteer1", result.getRecords().get(0).getUsername());
        }

        @Test
        @DisplayName("listVolunteers - 按状态过滤应传递状态参数")
        void listVolunteers_filterByStatus_shouldIncludeStatusInQuery() {
            Page<com.library.system.entity.VolunteerService> mockPage = new Page<>(1, 10);
            mockPage.setRecords(List.of());
            when(volunteerServiceMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);

            PageResult<VolunteerResponse> result = volunteerService.listVolunteers(1L, 10L, "PENDING");

            assertNotNull(result);
            assertEquals(0, result.getTotal());
        }

        @Test
        @DisplayName("getVolunteerById - 存在时返回详情")
        void getVolunteerById_whenExists_shouldReturnDetail() {
            when(volunteerServiceMapper.selectById(100L)).thenReturn(testVolunteer);
            when(userMapper.selectById(1L)).thenReturn(testUser);

            VolunteerResponse result = volunteerService.getVolunteerById(100L);

            assertNotNull(result);
            assertEquals("整理书籍", result.getServiceType());
            assertEquals("volunteer1", result.getUsername());
        }

        @Test
        @DisplayName("getVolunteerById - 不存在时应抛异常")
        void getVolunteerById_whenNotExists_shouldThrowException() {
            when(volunteerServiceMapper.selectById(999L)).thenReturn(null);

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> volunteerService.getVolunteerById(999L));
            assertEquals(ErrorCode.VOLUNTEER_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("getVolunteerById - 已删除时应抛异常")
        void getVolunteerById_whenDeleted_shouldThrowException() {
            testVolunteer.setDeleted(1);
            when(volunteerServiceMapper.selectById(100L)).thenReturn(testVolunteer);

            assertThrows(ResourceNotFoundException.class,
                    () -> volunteerService.getVolunteerById(100L));
        }

        @Test
        @DisplayName("getMyVolunteers - 应只返回当前用户记录")
        void getMyVolunteers_shouldReturnUserRecords() {
            Page<com.library.system.entity.VolunteerService> mockPage = new Page<>(1, 10);
            mockPage.setRecords(List.of(testVolunteer));
            mockPage.setTotal(1);
            when(volunteerServiceMapper.selectByUserId(any(Page.class), eq(1L))).thenReturn(mockPage);
            doReturn(List.of(testUser)).when(userMapper).selectBatchIds(anyCollection());

            PageResult<VolunteerResponse> result = volunteerService.getMyVolunteers(1L, 10L, 1L);

            assertNotNull(result);
            assertEquals(1, result.getTotal());
        }

        @Test
        @DisplayName("getMyVolunteers - 无记录时应返回空列表")
        void getMyVolunteers_whenNoRecords_shouldReturnEmpty() {
            Page<com.library.system.entity.VolunteerService> mockPage = new Page<>(1, 10);
            mockPage.setRecords(List.of());
            when(volunteerServiceMapper.selectByUserId(any(Page.class), eq(1L))).thenReturn(mockPage);

            PageResult<VolunteerResponse> result = volunteerService.getMyVolunteers(1L, 10L, 1L);

            assertTrue(result.getRecords().isEmpty());
        }

        @Test
        @DisplayName("getPendingVolunteers - 应返回待审核列表")
        void getPendingVolunteers_shouldReturnPendingList() {
            Page<com.library.system.entity.VolunteerService> mockPage = new Page<>(1, 10);
            mockPage.setRecords(List.of(testVolunteer));
            mockPage.setTotal(1);
            when(volunteerServiceMapper.selectPendingReview(any(Page.class))).thenReturn(mockPage);
            doReturn(List.of(testUser)).when(userMapper).selectBatchIds(anyCollection());

            PageResult<VolunteerResponse> result = volunteerService.getPendingVolunteers(1L, 10L);

            assertNotNull(result);
            assertEquals(1, result.getTotal());
            assertEquals("PENDING", result.getRecords().get(0).getStatus());
        }

        @Test
        @DisplayName("getPendingVolunteers - 无待审核记录应返回空列表")
        void getPendingVolunteers_whenNoPending_shouldReturnEmpty() {
            Page<com.library.system.entity.VolunteerService> mockPage = new Page<>(1, 10);
            mockPage.setRecords(List.of());
            when(volunteerServiceMapper.selectPendingReview(any(Page.class))).thenReturn(mockPage);

            PageResult<VolunteerResponse> result = volunteerService.getPendingVolunteers(1L, 10L);

            assertTrue(result.getRecords().isEmpty());
        }
    }

    @Nested
    @DisplayName("志愿服务创建用例")
    class CreateTests {

        @Test
        @DisplayName("createVolunteer - 成功创建待审核记录")
        void createVolunteer_shouldCreatePendingRecord() {
            when(volunteerServiceMapper.insert(any(com.library.system.entity.VolunteerService.class)))
                    .thenAnswer(invocation -> {
                        com.library.system.entity.VolunteerService v = invocation.getArgument(0);
                        v.setId(200L);
                        return 1;
                    });

            VolunteerResponse result = volunteerService.createVolunteer(1L, testRequest);

            assertNotNull(result);
            assertEquals("PENDING", result.getStatus());
            ArgumentCaptor<com.library.system.entity.VolunteerService> captor =
                    ArgumentCaptor.forClass(com.library.system.entity.VolunteerService.class);
            verify(volunteerServiceMapper).insert(captor.capture());
            assertEquals(1L, captor.getValue().getUserId());
            assertEquals(BigDecimal.valueOf(2), captor.getValue().getServiceHours());
        }

        @Test
        @DisplayName("createVolunteer - 未设serviceHours时通过起止时间自动计算")
        void createVolunteer_whenHoursNotSet_shouldCalculateFromTimeRange() {
            testRequest.setServiceHours(null);
            testRequest.setStartTime(LocalDateTime.now().minusHours(3));
            testRequest.setEndTime(LocalDateTime.now());
            when(volunteerServiceMapper.insert(any())).thenReturn(1);

            VolunteerResponse result = volunteerService.createVolunteer(1L, testRequest);

            assertNotNull(result);
        }

        @Test
        @DisplayName("createVolunteer - 超过12小时应抛异常")
        void createVolunteer_whenHoursExceedMax_shouldThrowException() {
            testRequest.setServiceHours(BigDecimal.valueOf(13));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> volunteerService.createVolunteer(1L, testRequest));
            assertEquals(ErrorCode.VOLUNTEER_HOURS_EXCEEDED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("志愿服务更新用例")
    class UpdateTests {

        @Test
        @DisplayName("updateVolunteer - 本人更新待审核记录应成功")
        void updateVolunteer_byOwnerAndPending_shouldSucceed() {
            when(volunteerServiceMapper.selectById(100L)).thenReturn(testVolunteer);
            testRequest.setDescription("更新后的描述");
            when(volunteerServiceMapper.updateById(any())).thenReturn(1);

            VolunteerResponse result = volunteerService.updateVolunteer(100L, 1L, testRequest);

            assertNotNull(result);
            verify(volunteerServiceMapper).updateById(any());
        }

        @Test
        @DisplayName("updateVolunteer - 非本人修改应抛ForbiddenException")
        void updateVolunteer_byOtherUser_shouldThrowForbidden() {
            when(volunteerServiceMapper.selectById(100L)).thenReturn(testVolunteer);

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> volunteerService.updateVolunteer(100L, 2L, testRequest));
            assertEquals(ErrorCode.INSUFFICIENT_PERMISSION, ex.getErrorCode());
        }

        @Test
        @DisplayName("updateVolunteer - 非待审核状态抛异常")
        void updateVolunteer_whenNotPending_shouldThrowException() {
            testVolunteer.setStatus("APPROVED");
            when(volunteerServiceMapper.selectById(100L)).thenReturn(testVolunteer);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> volunteerService.updateVolunteer(100L, 1L, testRequest));
            assertEquals(ErrorCode.VOLUNTEER_STATUS_ERROR, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("志愿服务取消用例")
    class CancelTests {

        @Test
        @DisplayName("cancelVolunteer - 本人取消待审核记录应成功")
        void cancelVolunteer_byOwnerAndPending_shouldSucceed() {
            when(volunteerServiceMapper.selectById(100L)).thenReturn(testVolunteer);

            volunteerService.cancelVolunteer(100L, 1L);

            verify(volunteerServiceMapper).updateById(any());
            assertEquals("CANCELLED", testVolunteer.getStatus());
        }

        @Test
        @DisplayName("cancelVolunteer - 非本人取消抛ForbiddenException")
        void cancelVolunteer_byOtherUser_shouldThrowForbidden() {
            when(volunteerServiceMapper.selectById(100L)).thenReturn(testVolunteer);

            assertThrows(ForbiddenException.class,
                    () -> volunteerService.cancelVolunteer(100L, 2L));
        }

        @Test
        @DisplayName("cancelVolunteer - 非待审核状态取消抛异常")
        void cancelVolunteer_whenNotPending_shouldThrowException() {
            testVolunteer.setStatus("APPROVED");
            when(volunteerServiceMapper.selectById(100L)).thenReturn(testVolunteer);

            assertThrows(BusinessException.class,
                    () -> volunteerService.cancelVolunteer(100L, 1L));
        }
    }

    @Nested
    @DisplayName("志愿服务审核用例")
    class ReviewTests {

        @Test
        @DisplayName("reviewVolunteer - 审核通过应更新状态并添加积分")
        void reviewVolunteer_approved_shouldUpdateStatusAndAddCredit() {
            when(volunteerServiceMapper.selectById(100L)).thenReturn(testVolunteer);

            VolunteerResponse result = volunteerService.reviewVolunteer(100L, 2L, true, "表现良好");

            assertEquals("APPROVED", result.getStatus());
            // 3小时 * 10分/小时 = 30分，不超过50分上限
            verify(creditService).addCredit(eq(1L), eq(30), eq("VOLUNTEER"), anyString(), eq(100L), eq("VOLUNTEER_SERVICE"));
        }

        @Test
        @DisplayName("reviewVolunteer - 审核拒绝不应添加积分")
        void reviewVolunteer_rejected_shouldNotAddCredit() {
            when(volunteerServiceMapper.selectById(100L)).thenReturn(testVolunteer);

            VolunteerResponse result = volunteerService.reviewVolunteer(100L, 2L, false, "不符合要求");

            assertEquals("REJECTED", result.getStatus());
            verify(creditService, never()).addCredit(any(), anyInt(), anyString(), anyString(), any(), anyString());
        }

        @Test
        @DisplayName("reviewVolunteer - 审核通过时积分上限为50分")
        void reviewVolunteer_approved_creditCappedAt50() {
            testVolunteer.setServiceHours(BigDecimal.valueOf(10));
            when(volunteerServiceMapper.selectById(100L)).thenReturn(testVolunteer);

            volunteerService.reviewVolunteer(100L, 2L, true, null);

            // 10小时 * 10分 = 100分，但上限50分
            verify(creditService).addCredit(eq(1L), eq(50), eq("VOLUNTEER"), anyString(), eq(100L), eq("VOLUNTEER_SERVICE"));
        }

        @Test
        @DisplayName("reviewVolunteer - 审核通过但服务时长为null不增加积分")
        void reviewVolunteer_approvedButNoHours_shouldNotAddCredit() {
            testVolunteer.setServiceHours(null);
            when(volunteerServiceMapper.selectById(100L)).thenReturn(testVolunteer);

            volunteerService.reviewVolunteer(100L, 2L, true, null);

            verify(creditService, never()).addCredit(any(), anyInt(), anyString(), anyString(), any(), anyString());
        }

        @Test
        @DisplayName("reviewVolunteer - 非待审核状态抛异常")
        void reviewVolunteer_whenNotPending_shouldThrowException() {
            testVolunteer.setStatus("APPROVED");
            when(volunteerServiceMapper.selectById(100L)).thenReturn(testVolunteer);

            assertThrows(BusinessException.class,
                    () -> volunteerService.reviewVolunteer(100L, 2L, true, null));
        }
    }

    @Nested
    @DisplayName("志愿服务删除和统计用例")
    class DeleteAndStatsTests {

        @Test
        @DisplayName("deleteVolunteer - 存在时成功删除")
        void deleteVolunteer_whenExists_shouldDelete() {
            when(volunteerServiceMapper.selectById(100L)).thenReturn(testVolunteer);

            volunteerService.deleteVolunteer(100L);

            verify(volunteerServiceMapper).deleteById(100L);
        }

        @Test
        @DisplayName("deleteVolunteer - 不存在时抛异常")
        void deleteVolunteer_whenNotExists_shouldThrowException() {
            when(volunteerServiceMapper.selectById(999L)).thenReturn(null);

            assertThrows(ResourceNotFoundException.class,
                    () -> volunteerService.deleteVolunteer(999L));
        }

        @Test
        @DisplayName("getVolunteerStats - 应返回正确统计")
        void getVolunteerStats_shouldReturnCorrectStats() {
            testVolunteer.setStatus("APPROVED");
            com.library.system.entity.VolunteerService v2 = new com.library.system.entity.VolunteerService();
            v2.setId(101L);
            v2.setUserId(1L);
            v2.setStatus("APPROVED");
            v2.setServiceHours(BigDecimal.valueOf(2));
            v2.setDeleted(0);

            when(volunteerServiceMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(testVolunteer, v2));
            when(volunteerServiceMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            VolunteerStatsDto stats =
                    volunteerService.getVolunteerStats(1L);

            assertEquals(2L, stats.getTotalRecords());
            assertEquals(BigDecimal.valueOf(5), stats.getTotalHours());
            assertEquals(1L, stats.getPendingCount());
        }

        @Test
        @DisplayName("getVolunteerStats - 无数据时应返回全0统计")
        void getVolunteerStats_whenNoData_shouldReturnZeroStats() {
            when(volunteerServiceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
            when(volunteerServiceMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            VolunteerStatsDto stats =
                    volunteerService.getVolunteerStats(1L);

            assertEquals(0L, stats.getTotalRecords());
            assertEquals(BigDecimal.ZERO, stats.getTotalHours());
            assertEquals(0L, stats.getPendingCount());
        }
    }
}
