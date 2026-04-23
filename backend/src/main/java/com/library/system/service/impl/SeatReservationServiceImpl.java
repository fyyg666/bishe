package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.common.Constants;
import com.library.system.dto.PageResult;
import com.library.system.dto.SeatReservationRequest;
import com.library.system.entity.*;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ForbiddenException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.*;
import com.library.system.service.CreditService;
import com.library.system.service.SeatReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 座位预约服务实现类
 *
 * @author Library Team
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatReservationServiceImpl implements SeatReservationService {

    private final ReadingRoomMapper readingRoomMapper;
    private final SeatMapper seatMapper;
    private final SeatReservationMapper seatReservationMapper;
    private final UserMapper userMapper;
    private final RedissonClient redissonClient;
    private final CreditService creditService;

    @Override
    public List<ReadingRoom> getReadingRooms() {
        return readingRoomMapper.selectList(null);
    }

    @Override
    public List<Seat> getSeatsByRoom(Long roomId) {
        return seatMapper.selectByRoomId(roomId);
    }

    @Override
    @Transactional
    public SeatReservation reserveSeat(Long userId, SeatReservationRequest request) {
        Long seatId = request.getSeatId();

        // 使用分布式锁
        String lockKey = "reserve:seat:" + seatId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                try {
                    // 1. 检查用户是否已有违约记录
                    User user = userMapper.selectById(userId);
                    if (user == null) {
                        throw new ResourceNotFoundException(ErrorCode.READER_NOT_FOUND, "用户不存在"); 
                    }

                    // 检查违约次数
                    LambdaQueryWrapper<SeatReservation> violationWrapper =
                        new LambdaQueryWrapper<>();
                    violationWrapper.eq(SeatReservation::getUserId, userId)
                            .eq(SeatReservation::getStatus, Constants.ReservationStatus.VIOLATED);
                    long violations = seatReservationMapper.selectCount(violationWrapper);
                    if (violations >= Constants.SeatLimit.VIOLATION_THRESHOLD) {
                        throw new BusinessException(ErrorCode.SEAT_RESERVATION_CONFLICT, "违约次数过多，暂时无法预约"); 
                    }

                    // 2. 检查每日预约次数限制
                    Integer todayCount = seatReservationMapper.countUserDailyReservations(
                        userId, LocalDate.now());
                    if (todayCount != null && todayCount >= Constants.SeatLimit.DAILY_MAX_RESERVATIONS) {
                        throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED, "今日预约次数已达上限"); 
                    }

                    // 3. 检查座位状态
                    Seat seat = seatMapper.selectById(seatId);
                    if (seat == null) {
                        throw new ResourceNotFoundException(ErrorCode.SEAT_NOT_FOUND, "座位不存在"); 
                    }
                    if (!Constants.SeatStatus.AVAILABLE.equals(seat.getStatus())) {
                        throw new BusinessException(ErrorCode.SEAT_NOT_AVAILABLE, "座位不可用"); 
                    }

                    // 4. 检查座位是否已被预约
                    List<SeatReservation> conflicts = seatReservationMapper.selectActiveBySeatId(
                        seat.getSeatNumber(), request.getReservationDate());
                    if (!conflicts.isEmpty()) {
                        throw new BusinessException(ErrorCode.SEAT_RESERVATION_CONFLICT, "该时间段座位已被预约"); 
                    }

                    // 5. 创建预约记录
                    SeatReservation reservation = new SeatReservation();
                    reservation.setUserId(userId);
                    reservation.setRoomId(request.getRoomId() != null ? request.getRoomId() : seat.getRoomId());
                    reservation.setSeatId(seatId);
                    reservation.setReservationDate(request.getReservationDate() != null ?
                        request.getReservationDate() : LocalDate.now());
                    reservation.setStartTime(request.getStartTime());
                    reservation.setEndTime(request.getEndTime());
                    reservation.setStatus(Constants.ReservationStatus.PENDING);
                    reservation.setViolationCount(0);
                    seatReservationMapper.insert(reservation);

                    // 6. 更新座位状态
                    seat.setStatus(Constants.SeatStatus.RESERVED);
                    seatMapper.updateById(seat);

                    log.info("用户 {} 预约座位 {} 成功", userId, seatId);
                    return reservation;
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "系统繁忙，请稍后再试"); 
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "预约操作被中断"); 
        }
    }

    @Override
    @Transactional
    public SeatReservation checkIn(Long userId, Long reservationId) {
        SeatReservation reservation = seatReservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new ResourceNotFoundException(ErrorCode.SEAT_RESERVATION_NOT_FOUND, "预约记录不存在"); 
        }
        if (!reservation.getUserId().equals(userId)) {
            throw new ForbiddenException(ErrorCode.INSUFFICIENT_PERMISSION, "无权操作此预约"); 
        }
        if (!Constants.ReservationStatus.PENDING.equals(reservation.getStatus())) {
            throw new BusinessException(ErrorCode.SEAT_RESERVATION_CONFLICT, "当前状态不支持签到"); 
        }

        // 检查是否超时
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reservationStart = LocalDateTime.of(reservation.getReservationDate(), reservation.getStartTime());
        if (now.isAfter(reservationStart.plusMinutes(15))) {
            reservation.setStatus(Constants.ReservationStatus.VIOLATED);
            reservation.setViolationCount(reservation.getViolationCount() + 1);
            seatReservationMapper.updateById(reservation);

            // 扣除积分
            creditService.deductCredit(userId, Constants.Credit.NO_SHOW, "NO_SHOW",
                "座位未签到", reservationId, "SEAT");
            throw new BusinessException(ErrorCode.SEAT_CHECK_IN_EXPIRED, "签到超时，已记录违约"); 
        }

        reservation.setStatus(Constants.ReservationStatus.CHECKED_IN);
        reservation.setCheckInTime(now);
        seatReservationMapper.updateById(reservation);

        // 更新座位状态为使用中
        Seat seat = seatMapper.selectById(reservation.getSeatId());
        if (seat != null) {
            seat.setStatus(Constants.SeatStatus.OCCUPIED);
            seatMapper.updateById(seat);
        }

        log.info("用户 {} 签到成功", userId);
        return reservation;
    }

    @Override
    @Transactional
    public SeatReservation checkOut(Long userId, Long reservationId) {
        SeatReservation reservation = seatReservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new ResourceNotFoundException(ErrorCode.SEAT_RESERVATION_NOT_FOUND, "预约记录不存在"); 
        }
        if (!reservation.getUserId().equals(userId)) {
            throw new ForbiddenException(ErrorCode.INSUFFICIENT_PERMISSION, "无权操作此预约"); 
        }
        if (!Constants.ReservationStatus.CHECKED_IN.equals(reservation.getStatus())) {
            throw new BusinessException(ErrorCode.SEAT_RESERVATION_CONFLICT, "当前状态不支持签退"); 
        }

        LocalDateTime now = LocalDateTime.now();
        reservation.setStatus(Constants.ReservationStatus.COMPLETED);
        reservation.setCheckOutTime(now);
        seatReservationMapper.updateById(reservation);

        // 恢复座位状态
        Seat seat = seatMapper.selectById(reservation.getSeatId());
        if (seat != null) {
            seat.setStatus(Constants.SeatStatus.AVAILABLE);
            seatMapper.updateById(seat);
        }

        log.info("用户 {} 签退成功", userId);
        return reservation;
    }

    @Override
    @Transactional
    public void cancelReservation(Long userId, Long reservationId) {
        SeatReservation reservation = seatReservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new ResourceNotFoundException(ErrorCode.SEAT_RESERVATION_NOT_FOUND, "预约记录不存在"); 
        }
        if (!reservation.getUserId().equals(userId)) {
            throw new ForbiddenException(ErrorCode.INSUFFICIENT_PERMISSION, "无权操作此预约"); 
        }
        if (Constants.ReservationStatus.CHECKED_IN.equals(reservation.getStatus())) {
            throw new BusinessException(ErrorCode.SEAT_RESERVATION_CONFLICT, "已开始使用，无法取消"); 
        }

        reservation.setStatus(Constants.ReservationStatus.CANCELLED);
        seatReservationMapper.updateById(reservation);

        // 恢复座位状态
        Seat seat = seatMapper.selectById(reservation.getSeatId());
        if (seat != null) {
            seat.setStatus(Constants.SeatStatus.AVAILABLE);
            seatMapper.updateById(seat);
        }

        log.info("用户 {} 取消预约 {}", userId, reservationId);
    }

    @Override
    public PageResult<SeatReservation> getMyReservations(Long userId, int pageNum, int pageSize) {
        Page<SeatReservation> page = new Page<>(pageNum, pageSize);
        IPage<SeatReservation> result = seatReservationMapper.selectByUserId(page, userId);

        // FIXED: PERF-009 批量查询关联数据，避免N+1查询
        List<SeatReservation> records = result.getRecords();
        if (!records.isEmpty()) {
            // 收集所有需要查询的ID
            Set<Long> roomIds = records.stream()
                    .map(SeatReservation::getRoomId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());
            Set<Long> seatIds = records.stream()
                    .map(SeatReservation::getSeatId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());
            Set<Long> userIds = records.stream()
                    .map(SeatReservation::getUserId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());
            
            // 批量查询
            Map<Long, ReadingRoom> roomMap = roomIds.isEmpty() ?
                Map.of() :
                readingRoomMapper.selectBatchIds(roomIds).stream()
                    .collect(Collectors.toMap(ReadingRoom::getId, r -> r));
            Map<Long, Seat> seatMap = seatIds.isEmpty() ?
                Map.of() :
                seatMapper.selectBatchIds(seatIds).stream()
                    .collect(Collectors.toMap(Seat::getId, s -> s));
            Map<Long, User> userMap = userIds.isEmpty() ?
                Map.of() :
                userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
            
            // 填充数据
            for (SeatReservation record : records) {
                ReadingRoom room = roomMap.get(record.getRoomId());
                if (room != null) {
                    record.setRoomName(room.getName());
                }
                Seat seat = seatMap.get(record.getSeatId());
                if (seat != null) {
                    record.setSeatNumber(seat.getSeatNumber());
                }
                User user = userMap.get(record.getUserId());
                if (user != null) {
                    record.setUsername(user.getUsername());
                    record.setRealName(user.getRealName());
                }
            }
        }

        return PageResult.of(
            result.getCurrent(),
            result.getSize(),
            result.getTotal(),
            records
        );
    }
}
