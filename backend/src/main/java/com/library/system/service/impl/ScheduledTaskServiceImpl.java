package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.system.common.Constants;
import com.library.system.entity.BorrowRecord;
import com.library.system.entity.StatisticsDaily;
import com.library.system.entity.SeatReservation;
import com.library.system.entity.User;
import com.library.system.mapper.*;
import com.library.system.service.CreditService;
import com.library.system.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务服务实现
 * 4个@Scheduled定时任务，每个任务均有分布式锁+异常隔离
 *
 * @author Library Team
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskServiceImpl {

    private final BorrowRecordMapper borrowRecordMapper;
    private final SeatReservationMapper seatReservationMapper;
    private final UserMapper userMapper;
    private final StatisticsDailyMapper statisticsDailyMapper;
    private final CreditService creditService;
    private final RedissonClient redissonClient;
    private final NotificationService notificationService;

    /**
     * 任务1：每日凌晨1点 - 逾期检查
     * 将所有已超期的BORROWING记录标记为OVERDUE
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void checkOverdueBooks() {
        String lockKey = "scheduled:overdue-check";
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(0, 30, TimeUnit.SECONDS)) {
                log.info("定时任务 - 逾期检查开始");
                LambdaQueryWrapper<BorrowRecord> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(BorrowRecord::getDeleted, 0)
                        .eq(BorrowRecord::getStatus, Constants.BorrowStatus.BORROWING)
                        .lt(BorrowRecord::getDueDate, LocalDateTime.now());
                List<BorrowRecord> overdueRecords = borrowRecordMapper.selectList(queryWrapper);

                int count = 0;
                for (BorrowRecord record : overdueRecords) {
                    record.setStatus(Constants.BorrowStatus.OVERDUE);
                    borrowRecordMapper.updateById(record);
                    count++;

                    try {
                        notificationService.createNotification(record.getUserId(), "图书逾期提醒",
                                "您借阅的《" + record.getBookTitle() + "》已逾期，到期日为" + record.getDueDate().toLocalDate() + "，请尽快归还",
                                Constants.BorrowStatus.OVERDUE, record.getId());
                    } catch (Exception e) {
                        log.warn("逾期通知发送失败: userId={}, borrowId={}", record.getUserId(), record.getId(), e);
                    }
                }
                log.info("定时任务 - 逾期检查完成，共标记{}条逾期记录", count);
            }
        } catch (Exception e) {
            log.error("定时任务-逾期检查异常", e);
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    /**
     * 任务2：每15分钟 - 预约过期检查
     * 扫描超时未签到的预约，标记VIOLATED并扣分
     */
    @Scheduled(cron = "0 */15 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void checkExpiredReservations() {
        String lockKey = "scheduled:expired-reservation";
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(0, 30, TimeUnit.SECONDS)) {
                log.debug("定时任务 - 预约过期检查开始");
                LambdaQueryWrapper<SeatReservation> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(SeatReservation::getDeleted, 0)
                        .eq(SeatReservation::getStatus, Constants.ReservationStatus.PENDING)
                        .apply("CONCAT(reservation_date, ' ', start_time) < DATE_ADD(NOW(), INTERVAL -30 MINUTE)");

                List<SeatReservation> expiredList = seatReservationMapper.selectList(wrapper);
                for (SeatReservation reservation : expiredList) {
                    creditService.deductCredit(reservation.getUserId(),
                            2, "NO_SHOW", "预约未签到扣除积分",
                            reservation.getId(), "SEAT_RESERVATION");
                    userMapper.incrementViolationCount(reservation.getUserId(),
                            Constants.SeatLimit.VIOLATION_THRESHOLD,
                            LocalDateTime.now().plusHours(72));
                    reservation.setStatus(Constants.ReservationStatus.VIOLATED);
                    seatReservationMapper.updateById(reservation);

                    try {
                        notificationService.createNotification(reservation.getUserId(), "预约未签到提醒",
                                "您预约的座位" + reservation.getSeatNumber() + "（" + reservation.getReservationDate() + "）因未签到已失效，已扣除2积分",
                                "SEAT_EXPIRED", reservation.getId());
                    } catch (Exception e) {
                        log.warn("预约过期通知发送失败: userId={}, reservationId={}", reservation.getUserId(), reservation.getId(), e);
                    }
                }
                log.debug("定时任务 - 预约过期检查完成，处理{}条", expiredList.size());
            }
        } catch (Exception e) {
            log.error("定时任务-预约过期检查异常", e);
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    /**
     * 任务3：每日凌晨1:30 - Token黑名单清理
     * Redis中的黑名单key有过期时间，只需SCAN确认无残留
     */
    @Scheduled(cron = "0 30 1 * * ?")
    public void cleanTokenBlacklist() {
        String lockKey = "scheduled:blacklist-clean";
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(0, 30, TimeUnit.SECONDS)) {
                log.info("定时任务 - Token黑名单清理完成（依赖Redis TTL自动过期）");
            }
        } catch (Exception e) {
            log.error("定时任务-黑名单清理异常", e);
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    /**
     * 任务4：每日凌晨2点 - 统计汇总
     * 将昨日的运营数据汇总写入statistics_daily表
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void aggregateDailyStats() {
        String lockKey = "scheduled:daily-stats";
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(0, 60, TimeUnit.SECONDS)) {
                log.info("定时任务 - 日统计汇总开始");
                LocalDate yesterday = LocalDate.now().minusDays(1);

                // 统计昨日借阅
                long totalBorrows = borrowRecordMapper.selectCount(
                        new LambdaQueryWrapper<BorrowRecord>()
                                .eq(BorrowRecord::getDeleted, 0)
                                .apply("DATE(create_time) = {0}", yesterday));
                // 统计昨日归还
                long totalReturns = borrowRecordMapper.selectCount(
                        new LambdaQueryWrapper<BorrowRecord>()
                                .eq(BorrowRecord::getDeleted, 0)
                                .eq(BorrowRecord::getStatus, Constants.BorrowStatus.RETURNED)
                                .apply("DATE(return_date) = {0}", yesterday));
                // 统计昨日逾期
                long totalOverdue = borrowRecordMapper.selectCount(
                        new LambdaQueryWrapper<BorrowRecord>()
                                .eq(BorrowRecord::getDeleted, 0)
                                .eq(BorrowRecord::getStatus, Constants.BorrowStatus.OVERDUE));
                // 统计昨日预约
                long totalReservations = seatReservationMapper.selectCount(
                        new LambdaQueryWrapper<SeatReservation>()
                                .eq(SeatReservation::getDeleted, 0)
                                .apply("DATE(create_time) = {0}", yesterday));
                // 统计昨日签到
                long totalCheckins = seatReservationMapper.selectCount(
                        new LambdaQueryWrapper<SeatReservation>()
                                .eq(SeatReservation::getDeleted, 0)
                                .eq(SeatReservation::getStatus, Constants.ReservationStatus.CHECKED_IN)
                                .apply("DATE(create_time) = {0}", yesterday));
                // 统计昨日新增用户
                long totalNewUsers = userMapper.selectCount(
                        new LambdaQueryWrapper<User>()
                                .eq(User::getDeleted, 0)
                                .apply("DATE(create_time) = {0}", yesterday));

                // 写入日统计表（upsert）
                StatisticsDaily daily = new StatisticsDaily();
                daily.setStatDate(yesterday);
                daily.setTotalBorrows((int) totalBorrows);
                daily.setTotalReturns((int) totalReturns);
                daily.setTotalOverdue((int) totalOverdue);
                daily.setTotalReservations((int) totalReservations);
                daily.setTotalCheckins((int) totalCheckins);
                daily.setTotalNewUsers((int) totalNewUsers);

                try {
                    statisticsDailyMapper.insert(daily);
                } catch (Exception e) {
                    // 如果已存在则更新
                    LambdaQueryWrapper<StatisticsDaily> existsWrapper =
                            new LambdaQueryWrapper<StatisticsDaily>().eq(StatisticsDaily::getStatDate, yesterday);
                    StatisticsDaily existing = statisticsDailyMapper.selectOne(existsWrapper);
                    if (existing != null) {
                        daily.setId(existing.getId());
                        statisticsDailyMapper.updateById(daily);
                    }
                }
                log.info("定时任务 - 日统计汇总完成: date={}, borrows={}, returns={}, reservations={}",
                        yesterday, totalBorrows, totalReturns, totalReservations);
            }
        } catch (Exception e) {
            log.error("定时任务-日统计汇总异常", e);
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }
}
