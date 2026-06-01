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
import com.library.system.mapper.ReadingRoomMapper;
import com.library.system.service.CreditService;
import com.library.system.service.NotificationService;
import com.library.system.service.SeatService;
import com.library.system.service.SysConfigService;
import com.library.system.template.DistributedLockTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
 * FIXED: BIZ-001 消除 SeatReservationService 委托，直接实现阅览室和座位查询
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
    private final NotificationService notificationService;
    private final ReadingRoomMapper readingRoomMapper;
    private final SysConfigService sysConfigService;

    // 时间格式化器
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // 座位区域配置
    @Value("${seat.areas:A区-安静区,B区-讨论区,C区-电脑区}")
    private String[] areas;

    @Value("${seat.seats-per-area:50}")
    private int seatsPerArea;

    @Override
    public List<SeatReservationResponse> listSeats(String area, LocalDate date) {
        // FIXED: PERF-002 预先生成座位列表，再批量查询预约数据，避免N+1查询
        List<String> seatNumbers = new ArrayList<>();
        String[] targetAreas = (area != null && !area.isEmpty()) ? new String[]{area} : areas;

        for (String targetArea : targetAreas) {
            for (int i = 1; i <= seatsPerArea; i++) {
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
                            .status(reservations.isEmpty() ? Constants.SeatStatus.AVAILABLE : "PARTIAL")
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 从座位号推断区域
     */
    private String getAreaFromSeatNumber(String seatNumber) {
        if (seatNumber == null || seatNumber.isEmpty()) {
            return areas[0];
        }
        char prefix = seatNumber.charAt(0);
        for (String area : areas) {
            if (area.charAt(0) == prefix) {
                return area;
            }
        }
        return areas[0];
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

            notificationService.createNotification(user.getId(), "座位预约成功",
                    "您已预约座位" + request.getSeatNumber() + "（" + time.date() + " " + time.startTime() + "-" + time.endTime() + "），请按时签到",
                    "SEAT_RESERVATION", reservation.getId());

            return convertToResponse(reservation);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelReservation(Long userId, Long reservationId) {
        String lockKey = "seat:cancel:" + reservationId;

        lockTemplate.executeWithLock(lockKey, () -> {
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

            LocalDateTime reservationStart = LocalDateTime.of(
                    reservation.getReservationDate(), reservation.getStartTime());
            int cancelBeforeHours = sysConfigService.getIntValue("seat.cancel_before_hours", 2);
            if (LocalDateTime.now().plusHours(cancelBeforeHours).isAfter(reservationStart)) {
                throw new BusinessException(ErrorCode.SEAT_CANCEL_TOO_LATE,
                        "预约开始前" + cancelBeforeHours + "小时内无法取消");
            }

            reservation.setStatus(Constants.ReservationStatus.CANCELLED); 
            reservation.setCancelReason("用户主动取消");
            seatReservationMapper.updateById(reservation);

            log.info("座位预约取消成功: userId={}, reservationId={}", userId, reservationId);
        });
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

            notificationService.createNotification(userId, "座位签到成功",
                    "您已成功签到座位" + reservation.getSeatNumber() + "，使用结束后请记得签退",
                    "SEAT_CHECKIN", reservationId);

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
        LambdaQueryWrapper<ReadingRoom> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReadingRoom::getDeleted, 0);
        return readingRoomMapper.selectList(wrapper);
    }

    @Override
    public List<Seat> getSeatsByRoom(Long roomId) {
        return seatMapper.selectByRoomId(roomId);
    }

    @Override
    public SeatDetailResponse getSeatDetail(Long id) {
        Seat seat = seatMapper.selectById(id);
        if (seat == null || seat.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.SEAT_NOT_FOUND, "座位不存在");
        }

        ReadingRoom room = readingRoomMapper.selectById(seat.getRoomId());

        SeatDetailResponse.SeatDetailResponseBuilder builder = SeatDetailResponse.builder()
                .id(seat.getId())
                .seatNumber(seat.getSeatNumber())
                .status(seat.getStatus())
                .location(room != null ? room.getName() + " - " + seat.getSeatNumber() : seat.getSeatNumber())
                .roomId(seat.getRoomId());

        if (room != null) {
            builder.roomName(room.getName()).roomLocation(room.getLocation());
        }

        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<SeatReservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeatReservation::getSeatId, id)
                .eq(SeatReservation::getReservationDate, today)
                .eq(SeatReservation::getDeleted, 0)
                .orderByAsc(SeatReservation::getStartTime);
        List<SeatReservation> reservations = seatReservationMapper.selectList(wrapper);

        List<SeatDetailResponse.TimeSlot> timeSlots = reservations.stream()
                .map(r -> SeatDetailResponse.TimeSlot.builder()
                        .startTime(r.getStartTime())
                        .endTime(r.getEndTime())
                        .status(r.getStatus())
                        .username(r.getUsername())
                        .build())
                .collect(Collectors.toList());

        builder.todayReservations(timeSlots);
        return builder.build();
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

        if (violationCount != null && violationCount >= Constants.SeatLimit.VIOLATION_THRESHOLD
                && banUntil != null && banUntil.isBefore(LocalDateTime.now())) {
            resetExpiredBan(user);
        }
    }

    private void resetExpiredBan(User user) {
        User freshUser = userMapper.selectById(user.getId());
        if (freshUser != null) {
            freshUser.setViolationCount(0);
            freshUser.setBanUntil(null);
            userMapper.updateById(freshUser);
        }
    }

    /**
     * 增加用户违约次数，达到阈值自动封禁72小时
     */
    private void incrementUserViolation(Long userId) {
        LocalDateTime banUntil = LocalDateTime.now().plusHours(72);
        userMapper.incrementViolationCount(userId, Constants.SeatLimit.VIOLATION_THRESHOLD, banUntil);

        User user = userMapper.selectById(userId);
        if (user != null && user.getViolationCount() != null && user.getViolationCount() >= Constants.SeatLimit.VIOLATION_THRESHOLD) {
            log.warn("用户违约次数达{}次，已封禁72小时: userId={}", user.getViolationCount(), userId);
        }
    }

    /**
     * 验证用户存在且未被禁用
     */
    private User validateUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || Constants.UserStatus.DISABLED.equals(user.getStatus())) {
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
        Integer count = seatReservationMapper.countUserDailyReservations(userId, LocalDate.now());
        long todayReservations = count != null ? count : 0;
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
