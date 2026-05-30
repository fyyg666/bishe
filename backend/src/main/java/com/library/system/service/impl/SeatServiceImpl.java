package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.common.Constants;
import com.library.system.dto.*;
import com.library.system.entity.ReadingRoom;
import com.library.system.entity.Seat;
import com.library.system.entity.SeatReservation;
import com.library.system.entity.User;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ForbiddenException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.SeatMapper;
import com.library.system.mapper.SeatReservationMapper;
import com.library.system.mapper.UserMapper;
import com.library.system.service.CreditService;
import com.library.system.service.SeatReservationService;
import com.library.system.service.SeatService;
import com.library.system.template.DistributedLockTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 座位服务实现类
 * FIXED: BIZ-001 委托 SeatReservationService 的独有方法，消除架构重复
 * FIXED: CODE-003 使用 DistributedLockTemplate 消除分布式锁重复代码
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private final SeatReservationMapper seatReservationMapper;
    private final SeatMapper seatMapper;
    private final UserMapper userMapper;
    private final CreditService creditService;
    private final DistributedLockTemplate lockTemplate;
    private final SeatReservationService seatReservationService; 

    // 时间格式化器
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // 座位区域配置
    private static final String[] AREAS = {"A区-安静区", "B区-讨论区", "C区-电脑区"};
    private static final int SEATS_PER_AREA = 50;

    @Override
    public List<SeatReservationResponse> listSeats(String area, LocalDate date) {
        // FIXED: PERF-002 预先生成座位列表，再批量查询预约数据，避免N+1查询
        List<String> seatNumbers = new ArrayList<>();
        String[] targetAreas = (area != null && !area.isEmpty()) ? new String[]{area} : AREAS;

        for (String targetArea : targetAreas) {
            for (int i = 1; i <= SEATS_PER_AREA; i++) {
                String seatNumber = targetArea.substring(0, 1) + "-" + String.format("%02d", i);
                seatNumbers.add(seatNumber);
            }
        }

        // 批量查询所有座位的预约情况（一次SQL查询）
        Map<String, List<SeatReservation>> reservationMap = 
            seatReservationMapper.selectBySeatNumbersAndDate(seatNumbers, date)
                .stream()
                .collect(Collectors.groupingBy(SeatReservation::getSeatNumber));

        // 构建返回结果
        return seatNumbers.stream()
                .map(seatNumber -> {
                    String targetArea = getAreaFromSeatNumber(seatNumber);
                    List<SeatReservation> reservations = reservationMap.getOrDefault(seatNumber, List.of());
                    return SeatReservationResponse.builder()
                            .seatNumber(seatNumber)
                            .area(targetArea)
                            .reservationDate(date)
                            .status(reservations.isEmpty() ? "AVAILABLE" : "PARTIAL")
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 从座位号推断区域
     */
    private String getAreaFromSeatNumber(String seatNumber) {
        if (seatNumber == null || seatNumber.isEmpty()) {
            return AREAS[0];
        }
        char prefix = seatNumber.charAt(0);
        return switch (prefix) {
            case 'A' -> "A区-安静区";
            case 'B' -> "B区-讨论区";
            case 'C' -> "C区-电脑区";
            default -> AREAS[0];
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeatReservationResponse reserveSeat(Long userId, SeatReservationRequest request) {
        // 座位号归一化：转大写（兼容 a05 → A05 等变体，但不处理横线因为数据库存储带横线的格式如 A-01）
        if (request.getSeatNumber() != null) {
            request.setSeatNumber(request.getSeatNumber().toUpperCase());
        }
        String lockKey = "seat:reserve:" + request.getSeatNumber() + ":" + request.getReservationDate();
        
        return lockTemplate.executeWithLock(lockKey, () -> {
            // 验证用户
            User user = validateUser(userId);

            // 检查违约封禁
            checkViolationBan(user);

            // 解析并验证时间
            ReservationTime time = parseAndValidateTime(request);

            // 检查冲突和限制
            checkConflictsAndLimits(userId, request.getSeatNumber(), time);

            // 创建预约记录
            SeatReservation reservation = createReservation(user, request, time);

            log.info("座位预约成功: user={}, seat={}, date={} {}-{}",
                    user.getUsername(), request.getSeatNumber(), 
                    time.date(), time.startTime(), time.endTime());

            return convertToResponse(reservation);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelReservation(Long userId, Long reservationId) {
        SeatReservation reservation = seatReservationMapper.selectById(reservationId);
        if (reservation == null || reservation.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.SEAT_RESERVATION_NOT_FOUND, "预约记录不存在");
        }
        if (!reservation.getUserId().equals(userId)) {
            throw new ForbiddenException(ErrorCode.INSUFFICIENT_PERMISSION, "无权操作此预约记录");
        }
        if (!Constants.ReservationStatus.PENDING.equals(reservation.getStatus())) { 
            throw new BusinessException(ErrorCode.SEAT_RESERVATION_CONFLICT, "只能取消待使用的预约");
        }

        // 检查是否在预约时间前2小时
        LocalDateTime reservationStart = LocalDateTime.of(
                reservation.getReservationDate(), reservation.getStartTime());
        if (LocalDateTime.now().plusHours(2).isAfter(reservationStart)) {
            throw new BusinessException(ErrorCode.SEAT_CANCEL_TOO_LATE, "预约开始前2小时内无法取消");
        }

        reservation.setStatus(Constants.ReservationStatus.CANCELLED); 
        reservation.setCancelReason("用户主动取消");
        seatReservationMapper.updateById(reservation);

        log.info("座位预约取消成功: userId={}, reservationId={}", userId, reservationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeatReservationResponse checkIn(Long userId, Long reservationId) {
        String lockKey = "seat:checkin:" + reservationId;
        
        return lockTemplate.executeWithLock(lockKey, () -> {
            SeatReservation reservation = seatReservationMapper.selectById(reservationId);
            if (reservation == null || reservation.getDeleted() == 1) {
                throw new ResourceNotFoundException(ErrorCode.SEAT_RESERVATION_NOT_FOUND, "预约记录不存在");
            }
            if (!reservation.getUserId().equals(userId)) {
                throw new ForbiddenException(ErrorCode.INSUFFICIENT_PERMISSION, "无权操作此预约记录");
            }
            if (!Constants.ReservationStatus.PENDING.equals(reservation.getStatus())) { 
                throw new BusinessException(ErrorCode.SEAT_RESERVATION_CONFLICT, "预约状态异常，无法签到");
            }

            // 检查签到时间
            LocalDateTime reservationStart = LocalDateTime.of(
                    reservation.getReservationDate(), reservation.getStartTime());
            LocalDateTime now = LocalDateTime.now();

            if (now.isBefore(reservationStart.minusMinutes(15))) {
                throw new BusinessException(ErrorCode.SEAT_CHECK_IN_EXPIRED, "签到时间未到，请在预约开始前15分钟内签到");
            }
            if (now.isAfter(reservationStart.plusMinutes(30))) {
                reservation.setStatus(Constants.ReservationStatus.VIOLATED); 
                seatReservationMapper.updateById(reservation);
                // 增加用户违约次数并检查封禁
                incrementUserViolation(userId);
                throw new BusinessException(ErrorCode.SEAT_CHECK_IN_EXPIRED, "已超过签到时间，预约已失效");
            }

            reservation.setStatus(Constants.ReservationStatus.CHECKED_IN); 
            reservation.setCheckInTime(now);
            seatReservationMapper.updateById(reservation);

            // 处理签到积分
            creditService.processCheckInCredit(userId, reservationId);

            log.info("座位签到成功: userId={}, reservationId={}", userId, reservationId);

            return convertToResponse(reservation);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeatReservationResponse checkOut(Long userId, Long reservationId) {
        SeatReservation reservation = seatReservationMapper.selectById(reservationId);
        if (reservation == null || reservation.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.SEAT_RESERVATION_NOT_FOUND, "预约记录不存在");
        }
        if (!reservation.getUserId().equals(userId)) {
            throw new ForbiddenException(ErrorCode.INSUFFICIENT_PERMISSION, "无权操作此预约记录");
        }
        if (!Constants.ReservationStatus.CHECKED_IN.equals(reservation.getStatus())) { 
            throw new BusinessException(ErrorCode.SEAT_RESERVATION_CONFLICT, "请先签到后再签退");
        }

        reservation.setStatus(Constants.ReservationStatus.COMPLETED); 
        reservation.setCheckOutTime(LocalDateTime.now());
        seatReservationMapper.updateById(reservation);

        log.info("座位签退成功: userId={}, reservationId={}", userId, reservationId);

        return convertToResponse(reservation);
    }

    @Override
    public PageResult<SeatReservationResponse> getMyReservations(Long userId, Long current, Long size) {
        LambdaQueryWrapper<SeatReservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeatReservation::getUserId, userId);
        wrapper.eq(SeatReservation::getDeleted, 0);
        wrapper.orderByDesc(SeatReservation::getReservationDate)
                .orderByDesc(SeatReservation::getStartTime);

        Page<SeatReservation> page = new Page<>(current, size);
        Page<SeatReservation> reservationPage = seatReservationMapper.selectPage(page, wrapper);

        List<SeatReservationResponse> records = reservationPage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return PageResult.of(reservationPage.getCurrent(), reservationPage.getSize(),
                reservationPage.getTotal(), records);
    }

    @Override
    public boolean isTimeSlotAvailable(String seatNumber, LocalDate date, String startTime, String endTime) {
        LocalTime start = LocalTime.parse(startTime, TIME_FORMATTER);
        LocalTime end = LocalTime.parse(endTime, TIME_FORMATTER);

        int conflicts = seatReservationMapper.countConflictingReservations(seatNumber, date, start, end);
        return conflicts == 0;
    }

    @Override
    public List<ReadingRoom> getReadingRooms() {
        return seatReservationService.getReadingRooms();
    }

    @Override
    public List<Seat> getSeatsByRoom(Long roomId) {
        return seatReservationService.getSeatsByRoom(roomId);
    }

    // =================== 私有辅助方法 ===================

    /**
     * 检查用户违约封禁
     * 累计违约3次暂停72小时预约权限
     */
    private void checkViolationBan(User user) {
        Integer violationCount = user.getViolationCount();
        LocalDateTime banUntil = user.getBanUntil();

        if (violationCount != null && violationCount >= Constants.SeatLimit.VIOLATION_THRESHOLD
                && banUntil != null && banUntil.isAfter(LocalDateTime.now())) {
            long hours = java.time.temporal.ChronoUnit.HOURS.between(LocalDateTime.now(), banUntil);
            throw new BusinessException(ErrorCode.SEAT_RESERVATION_CONFLICT,
                    "因违约次数过多，预约权限已被暂停，剩余" + (hours > 0 ? hours + "小时" : "不到1小时"));
        }

        // 封禁已过期，清零违约次数
        if (violationCount != null && violationCount >= Constants.SeatLimit.VIOLATION_THRESHOLD
                && banUntil != null && banUntil.isBefore(LocalDateTime.now())) {
            user.setViolationCount(0);
            user.setBanUntil(null);
            userMapper.updateById(user);
        }
    }

    /**
     * 增加用户违约次数，达到阈值自动封禁72小时
     */
    private void incrementUserViolation(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return;

        int newCount = (user.getViolationCount() != null ? user.getViolationCount() : 0) + 1;
        user.setViolationCount(newCount);

        if (newCount >= Constants.SeatLimit.VIOLATION_THRESHOLD) {
            user.setBanUntil(LocalDateTime.now().plusHours(72));
            log.warn("用户违约次数达{}次，已封禁72小时: userId={}", newCount, userId);
        }

        userMapper.updateById(user);
    }

    /**
     * 验证用户存在且未被禁用
     */
    private User validateUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || "DISABLED".equals(user.getStatus())) {
            throw new ResourceNotFoundException(ErrorCode.READER_NOT_FOUND, "用户不存在或已被禁用");
        }
        return user;
    }

    /**
     * 预约时间封装类
     */
    private record ReservationTime(LocalDate date, LocalTime startTime, LocalTime endTime) {}

    /**
     * 解析并验证预约时间
     */
    private ReservationTime parseAndValidateTime(SeatReservationRequest request) {
        LocalDate date = request.getReservationDate() != null ?
            request.getReservationDate() : LocalDate.now();
        LocalTime startTime = request.getStartTime() != null ?
            request.getStartTime() : LocalTime.of(9, 0);
        LocalTime endTime = request.getEndTime() != null ?
            request.getEndTime() : LocalTime.of(17, 0);

        validateTime(date, startTime, endTime);

        return new ReservationTime(date, startTime, endTime);
    }

    /**
     * 验证时间合法性
     */
    private void validateTime(LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (date.isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "不能预约过去的日期");
        }
        if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "结束时间必须晚于开始时间");
        }
        if (startTime.isBefore(LocalTime.of(8, 0)) || endTime.isAfter(LocalTime.of(22, 0))) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "预约时间必须在8:00-22:00之间");
        }
    }

    /**
     * 检查时间冲突和预约限制
     */
    private void checkConflictsAndLimits(Long userId, String seatNumber, ReservationTime time) {
        // 检查时间段冲突
        int conflicts = seatReservationMapper.countConflictingReservations(
                seatNumber, time.date(), time.startTime(), time.endTime());
        if (conflicts > 0) {
            throw new BusinessException(ErrorCode.SEAT_RESERVATION_CONFLICT, "该时间段已被预约");
        }

        // 检查用户当天预约数量限制
        checkDailyReservationLimit(userId);
    }

    /**
     * 检查用户当天预约数量限制
     */
    private void checkDailyReservationLimit(Long userId) {
        long todayReservations = seatReservationMapper.selectByUserAndDate(userId, LocalDate.now())
                .stream()
                .filter(r -> !"CANCELLED".equals(r.getStatus()) && !Constants.ReservationStatus.VIOLATED.equals(r.getStatus()))
                .count();
        if (todayReservations >= Constants.SeatLimit.DAILY_MAX_RESERVATIONS) { 
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED, 
                    "每天最多预约" + Constants.SeatLimit.DAILY_MAX_RESERVATIONS + "个时段");
        }
    }

    /**
     * 创建预约记录
     */
    private SeatReservation createReservation(User user, SeatReservationRequest request, ReservationTime time) {
        // 查询座位信息，获取 roomId 和 seatId
        Seat seat = seatMapper.selectOne(
                new LambdaQueryWrapper<Seat>()
                        .eq(Seat::getSeatNumber, request.getSeatNumber())
                        .eq(Seat::getDeleted, 0)
        );
        if (seat == null) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "座位不存在: " + request.getSeatNumber());
        }

        SeatReservation reservation = SeatReservation.builder()
                .seatNumber(request.getSeatNumber())
                .area(request.getArea())
                .userId(user.getId())
                .username(user.getUsername())
                .roomId(seat.getRoomId())
                .seatId(seat.getId())
                .reservationDate(time.date())
                .startTime(time.startTime())
                .endTime(time.endTime())
                .status(Constants.ReservationStatus.PENDING) 
                .source(request.getSource() != null ? request.getSource() : "WEB")
                .build();

        seatReservationMapper.insert(reservation);
        return reservation;
    }

    /**
     * 将SeatReservation实体转换为SeatReservationResponse DTO
     */
    private SeatReservationResponse convertToResponse(SeatReservation reservation) {
        return SeatReservationResponse.builder()
                .id(reservation.getId())
                .seatNumber(reservation.getSeatNumber())
                .area(reservation.getArea())
                .username(reservation.getUsername())
                .reservationDate(reservation.getReservationDate())
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .status(reservation.getStatus())
                .checkInTime(reservation.getCheckInTime())
                .checkOutTime(reservation.getCheckOutTime())
                .source(reservation.getSource())
                .createTime(reservation.getCreateTime())
                .build();
    }
}
