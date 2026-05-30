package com.library.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.system.base.BaseTest;
import com.library.system.common.Constants;
import com.library.system.entity.BorrowRecord;
import com.library.system.entity.SeatReservation;
import com.library.system.entity.StatisticsDaily;
import com.library.system.entity.User;
import com.library.system.mapper.BorrowRecordMapper;
import com.library.system.mapper.SeatReservationMapper;
import com.library.system.mapper.StatisticsDailyMapper;
import com.library.system.mapper.UserMapper;
import com.library.system.service.impl.ScheduledTaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ScheduledTaskService 单元测试")
class ScheduledTaskServiceTest extends BaseTest {

    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @Mock
    private SeatReservationMapper seatReservationMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private StatisticsDailyMapper statisticsDailyMapper;

    @Mock
    private CreditService creditService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @InjectMocks
    private ScheduledTaskServiceImpl scheduledTaskService;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(redissonClient.getLock(anyString())).thenReturn(rLock);
        lenient().when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        lenient().when(rLock.isHeldByCurrentThread()).thenReturn(true);
        lenient().doNothing().when(rLock).unlock();
    }

    @Nested
    @DisplayName("逾期检查定时任务")
    class CheckOverdueBooksTests {

        @Test
        @DisplayName("checkOverdueBooks - 有逾期记录应标记为OVERDUE")
        void checkOverdueBooks_withOverdueRecords_shouldMarkAsOverdue() {
            BorrowRecord overdue1 = BorrowRecord.builder()
                    .id(1L).userId(1L).bookId(1L)
                    .status(Constants.BorrowStatus.BORROWING)
                    .dueDate(LocalDateTime.now().minusDays(3))
                    .deleted(0).build();
            BorrowRecord overdue2 = BorrowRecord.builder()
                    .id(2L).userId(2L).bookId(2L)
                    .status(Constants.BorrowStatus.BORROWING)
                    .dueDate(LocalDateTime.now().minusDays(1))
                    .deleted(0).build();
            when(borrowRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(overdue1, overdue2));

            scheduledTaskService.checkOverdueBooks();

            assertEquals(Constants.BorrowStatus.OVERDUE, overdue1.getStatus());
            assertEquals(Constants.BorrowStatus.OVERDUE, overdue2.getStatus());
            verify(borrowRecordMapper, times(2)).updateById(any(BorrowRecord.class));
        }

        @Test
        @DisplayName("checkOverdueBooks - 无逾期记录不执行更新")
        void checkOverdueBooks_noOverdueRecords_shouldNotUpdate() {
            when(borrowRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            scheduledTaskService.checkOverdueBooks();

            verify(borrowRecordMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("checkOverdueBooks - 未获取到锁时不应执行")
        void checkOverdueBooks_lockNotAcquired_shouldSkip() throws Exception {
            when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

            scheduledTaskService.checkOverdueBooks();

            verify(borrowRecordMapper, never()).selectList(any());
        }

        @Test
        @DisplayName("checkOverdueBooks - 异常应被捕获不抛出")
        void checkOverdueBooks_exception_shouldBeCaught() {
            when(borrowRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenThrow(new RuntimeException("DB error"));

            // 不应抛出异常
            assertDoesNotThrow(() -> scheduledTaskService.checkOverdueBooks());
        }
    }

    @Nested
    @DisplayName("预约过期检查定时任务")
    class CheckExpiredReservationsTests {

        @Test
        @DisplayName("checkExpiredReservations - 有过期预约应标记VIOLATED并扣分")
        void checkExpiredReservations_withExpired_shouldMarkViolatedAndDeductCredit() {
            SeatReservation expired = SeatReservation.builder()
                    .id(100L).userId(1L).seatId(101L)
                    .status(Constants.ReservationStatus.PENDING)
                    .deleted(0).build();
            User testUser = new User();
            testUser.setId(1L);
            testUser.setViolationCount(0);

            when(seatReservationMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(expired));
            when(userMapper.selectById(1L)).thenReturn(testUser);
            doNothing().when(creditService).deductCredit(any(), anyInt(), anyString(), anyString(), any(), anyString());

            scheduledTaskService.checkExpiredReservations();

            assertEquals(Constants.ReservationStatus.VIOLATED, expired.getStatus());
            verify(creditService).deductCredit(eq(1L), eq(2), eq("NO_SHOW"), anyString(), eq(100L), eq("SEAT_RESERVATION"));
            verify(userMapper).updateById(any(User.class));
        }

        @Test
        @DisplayName("checkExpiredReservations - 多次违约达到阈值应封禁72小时")
        void checkExpiredReservations_violationThreshold_shouldBanUser() {
            SeatReservation expired = SeatReservation.builder()
                    .id(100L).userId(1L).seatId(101L)
                    .status(Constants.ReservationStatus.PENDING)
                    .deleted(0).build();
            User testUser = new User();
            testUser.setId(1L);
            testUser.setViolationCount(Constants.SeatLimit.VIOLATION_THRESHOLD - 1);

            when(seatReservationMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(expired));
            when(userMapper.selectById(1L)).thenReturn(testUser);

            scheduledTaskService.checkExpiredReservations();

            assertEquals(Constants.SeatLimit.VIOLATION_THRESHOLD, testUser.getViolationCount().intValue());
            assertNotNull(testUser.getBanUntil());
            verify(userMapper).updateById(any(User.class));
        }

        @Test
        @DisplayName("checkExpiredReservations - 扣分失败不应影响主流程")
        void checkExpiredReservations_creditDeductionFails_shouldNotAffectMainFlow() {
            SeatReservation expired = SeatReservation.builder()
                    .id(100L).userId(1L).seatId(101L)
                    .status(Constants.ReservationStatus.PENDING)
                    .deleted(0).build();

            when(seatReservationMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(expired));
            doThrow(new RuntimeException("Credit service down"))
                    .when(creditService).deductCredit(any(), anyInt(), anyString(), anyString(), any(), anyString());

            assertDoesNotThrow(() -> scheduledTaskService.checkExpiredReservations());
            // 预约状态仍应被标记为违约（主流程不受影响）
            assertEquals(Constants.ReservationStatus.VIOLATED, expired.getStatus());
        }

        @Test
        @DisplayName("checkExpiredReservations - 用户不存在时不更新违约次数")
        void checkExpiredReservations_userNotFound_shouldSkipViolationCount() {
            SeatReservation expired = SeatReservation.builder()
                    .id(100L).userId(1L).seatId(101L)
                    .status(Constants.ReservationStatus.PENDING)
                    .deleted(0).build();

            when(seatReservationMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(expired));
            when(userMapper.selectById(1L)).thenReturn(null);

            assertDoesNotThrow(() -> scheduledTaskService.checkExpiredReservations());
            verify(userMapper, never()).updateById(any());
        }
    }

    @Nested
    @DisplayName("Token黑名单清理定时任务")
    class CleanTokenBlacklistTests {

        @Test
        @DisplayName("cleanTokenBlacklist - 获取锁后应正常执行")
        void cleanTokenBlacklist_lockAcquired_shouldExecute() {
            assertDoesNotThrow(() -> scheduledTaskService.cleanTokenBlacklist());
            verify(rLock).unlock();
        }

        @Test
        @DisplayName("cleanTokenBlacklist - 未获取锁则跳过")
        void cleanTokenBlacklist_lockNotAcquired_shouldSkip() throws Exception {
            when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);
            when(rLock.isHeldByCurrentThread()).thenReturn(false);

            scheduledTaskService.cleanTokenBlacklist();

            verify(rLock, never()).unlock();
        }
    }

    @Nested
    @DisplayName("日统计汇总定时任务")
    class AggregateDailyStatsTests {

        @Test
        @DisplayName("aggregateDailyStats - 应正确汇总昨日数据并写入")
        void aggregateDailyStats_shouldAggregateAndInsert() {
            when(borrowRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L, 8L, 2L);
            when(seatReservationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L, 3L);
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(4L);
            when(statisticsDailyMapper.insert(any(StatisticsDaily.class))).thenReturn(1);

            assertDoesNotThrow(() -> scheduledTaskService.aggregateDailyStats());

            verify(statisticsDailyMapper).insert(any(StatisticsDaily.class));
            verify(statisticsDailyMapper, never()).selectOne(any());
            verify(statisticsDailyMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("aggregateDailyStats - 插入冲突时应更新已有记录")
        void aggregateDailyStats_whenInsertConflict_shouldUpdateExisting() {
            when(borrowRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L, 8L, 2L);
            when(seatReservationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L, 3L);
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(4L);
            when(statisticsDailyMapper.insert(any(StatisticsDaily.class)))
                    .thenThrow(new RuntimeException("Duplicate key"));
            StatisticsDaily existing = new StatisticsDaily();
            existing.setId(1L);
            when(statisticsDailyMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

            assertDoesNotThrow(() -> scheduledTaskService.aggregateDailyStats());

            verify(statisticsDailyMapper).updateById(any(StatisticsDaily.class));
        }

        @Test
        @DisplayName("aggregateDailyStats - 插入冲突但查不到旧记录时应跳过更新")
        void aggregateDailyStats_whenInsertConflict_noExisting_shouldSkipUpdate() {
            when(borrowRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L, 8L, 2L);
            when(seatReservationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L, 3L);
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(4L);
            when(statisticsDailyMapper.insert(any(StatisticsDaily.class)))
                    .thenThrow(new RuntimeException("Duplicate key"));
            when(statisticsDailyMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            assertDoesNotThrow(() -> scheduledTaskService.aggregateDailyStats());

            verify(statisticsDailyMapper, never()).updateById(any());
        }
    }
}
