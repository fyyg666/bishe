package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.common.Constants;
import com.library.system.dto.*;
import com.library.system.entity.Book;
import com.library.system.entity.BorrowRecord;
import com.library.system.entity.BorrowRule;
import com.library.system.entity.User;
import com.library.system.enums.ErrorCode;
import com.library.system.event.BorrowEvent;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ForbiddenException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.BookMapper;
import com.library.system.mapper.BorrowRecordMapper;
import com.library.system.mapper.UserMapper;
import com.library.system.service.BorrowService;
import com.library.system.service.BookReservationService;
import com.library.system.service.BorrowRuleService;
import com.library.system.service.CreditService;
import com.library.system.service.NotificationService;
import com.library.system.util.HolidayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 借阅服务实现类
 * 使用分布式锁保证并发安全
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowServiceImpl implements BorrowService {

    private final BorrowRecordMapper borrowRecordMapper;
    private final BookMapper bookMapper;
    private final UserMapper userMapper;
    private final CreditService creditService;
    private final RedissonClient redissonClient;
    private final HolidayUtil holidayUtil;
    private final TransactionTemplate transactionTemplate;
    private final NotificationService notificationService;
    private final BookReservationService bookReservationService;
    private final BorrowRuleService borrowRuleService;
    private final ApplicationEventPublisher eventPublisher;

    // 逾期罚款每天金额
    private static final BigDecimal DAILY_FINE = new BigDecimal("0.10");

    @Override
    public BorrowResponse borrowBook(Long userId, BorrowRequest request) {
        String lockKey = "borrow:book:" + request.getBookId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            acquireLockOrThrow(lock);

            return transactionTemplate.execute(status -> {
                User user = validateAndGetUser(userId);
                validateCreditScore(user);
                validateNoOverdueBooks(userId);

                validateBorrowLimit(user);

                Book book = validateAndGetBook(request.getBookId());
                validateBookAvailable(book);

                int borrowDays = validateAndGetBorrowDays(request.getBorrowDays());

                performBorrowOperation(book, user, borrowDays);

                BorrowRecord record = createBorrowRecord(userId, user, book, borrowDays);

                log.info("图书借阅成功: user={}, book={}", user.getUsername(), book.getTitle());

                eventPublisher.publishEvent(BorrowEvent.builder()
                        .type("BORROW")
                        .userId(userId)
                        .borrowId(record.getId())
                        .bookTitle(book.getTitle())
                        .dueDate(record.getDueDate())
                        .build());
                return convertToResponse(record);
            });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "操作被中断");
        } finally {
            releaseLock(lock);
        }
    }

    @Override
    public BorrowResponse returnBook(Long userId, Long borrowId) {
        String lockKey = "borrow:return:" + borrowId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            acquireLockOrThrow(lock);

            return transactionTemplate.execute(status -> {
                BorrowRecord record = validateAndGetRecord(borrowId, userId);
                Book book = validateAndGetBook(record.getBookId());
                User user = validateAndGetUser(userId);

                LocalDateTime returnDateTime = LocalDateTime.now();
                int overdueDays = calculateOverdueDays(record.getDueDate(), returnDateTime);
                BigDecimal fineAmount = calculateFine(overdueDays);

                updateBorrowRecordForReturn(record, returnDateTime, overdueDays, fineAmount);
                updateBookStock(book);
                updateUserBorrowCount(user);

                log.info("图书归还成功: user={}, book={}, overdueDays={}",
                        user.getUsername(), book.getTitle(), overdueDays);

                eventPublisher.publishEvent(BorrowEvent.builder()
                        .type("RETURN")
                        .userId(userId)
                        .borrowId(borrowId)
                        .bookTitle(book.getTitle())
                        .dueDate(record.getDueDate())
                        .overdueDays(overdueDays)
                        .fineAmount(fineAmount)
                        .returnDate(returnDateTime)
                        .build());

                try {
                    bookReservationService.notifyNextInQueue(record.getBookId());
                } catch (Exception e) {
                    log.warn("归还后通知预约排队用户失败: bookId={}", record.getBookId(), e);
                }

                return convertToResponse(record);
            });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "操作被中断");
        } finally {
            releaseLock(lock);
        }
    }

    @Override
    public BorrowResponse renewBook(Long userId, Long borrowId, Integer days) {
        String lockKey = "borrow:renew:" + borrowId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            acquireLockOrThrow(lock);

            return transactionTemplate.execute(status -> {
                BorrowRecord record = validateAndGetRecord(borrowId, userId);
                validateRecordNotOverdue(record);
                validateRenewCount(record);

                int renewDays = days != null ? days : Constants.BorrowLimit.RENEW_DAYS;
                performRenewOperation(record, renewDays);

                log.info("图书续借成功: userId={}, borrowId={}, newDueDate={}",
                        userId, borrowId, record.getDueDate());

                eventPublisher.publishEvent(BorrowEvent.builder()
                        .type("RENEW")
                        .userId(userId)
                        .borrowId(record.getId())
                        .bookTitle(record.getBookTitle())
                        .dueDate(record.getDueDate())
                        .build());

                return convertToResponse(record);
            });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "操作被中断");
        } finally {
            releaseLock(lock);
        }
    }

    @Override
    public PageResult<BorrowResponse> getMyBorrows(Long userId, Long current, Long size, String status) {
        Page<BorrowRecord> page = new Page<>(current, size);
        String statusParam = (status != null && !status.isEmpty()) ? status : null;
        Page<BorrowRecord> recordPage = borrowRecordMapper.selectMyBorrowsWithJoin(page, userId, statusParam);

        List<BorrowResponse> records = recordPage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return PageResult.of(recordPage.getCurrent(), recordPage.getSize(),
                recordPage.getTotal(), records);
    }

    @Override
    public PageResult<BorrowResponse> getAllBorrows(Long current, Long size, String status) {
        Page<BorrowRecord> page = new Page<>(current, size);
        String statusParam = (status != null && !status.isEmpty()) ? status : null;
        Page<BorrowRecord> recordPage = borrowRecordMapper.selectAllBorrowsWithJoin(page, statusParam);

        List<BorrowResponse> records = recordPage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return PageResult.of(recordPage.getCurrent(), recordPage.getSize(),
                recordPage.getTotal(), records);
    }

    @Override
    public BorrowResponse getBorrowById(Long borrowId) {
        BorrowRecord record = borrowRecordMapper.selectBorrowWithJoinById(borrowId);
        if (record == null || record.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.BORROW_RECORD_NOT_FOUND, "借阅记录不存在");
        }
        return convertToResponse(record);
    }

    @Override
    public BorrowResponse getBorrowByIdWithOwnershipCheck(Long borrowId, Long currentUserId, String currentRole) {
        BorrowRecord record = borrowRecordMapper.selectBorrowWithJoinById(borrowId);
        if (record == null || record.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.BORROW_RECORD_NOT_FOUND, "借阅记录不存在");
        }

        boolean isAdmin = Constants.Role.ADMIN.equals(currentRole) || Constants.Role.LIBRARIAN.equals(currentRole);
        boolean isOwner = record.getUserId().equals(currentUserId);

        if (!isAdmin && !isOwner) {
            log.warn("水平越权尝试: userId={} 尝试访问 borrowId={} (属于 userId={})",
                    currentUserId, borrowId, record.getUserId());
            throw new ForbiddenException(ErrorCode.INSUFFICIENT_PERMISSION, "无权访问此借阅记录");
        }

        return convertToResponse(record);
    }

    @Override
    public List<BorrowExportDTO> getExportData(String status) {
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(BorrowRecord::getStatus, status);
        }
        wrapper.eq(BorrowRecord::getDeleted, 0);
        wrapper.orderByDesc(BorrowRecord::getCreateTime);
        List<BorrowRecord> records = borrowRecordMapper.selectList(wrapper);
        return records.stream().map(record -> BorrowExportDTO.builder()
                .id(record.getId())
                .username(record.getUsername())
                .bookTitle(record.getBookTitle())
                .bookIsbn(record.getBookIsbn())
                .borrowDate(record.getBorrowDate() != null ? record.getBorrowDate().toLocalDate().toString() : "")
                .dueDate(record.getDueDate() != null ? record.getDueDate().toLocalDate().toString() : "")
                .returnDate(record.getReturnDate() != null ? record.getReturnDate().toLocalDate().toString() : "")
                .status(record.getStatus())
                .renewCount(record.getRenewCount())
                .overdueDays(record.getOverdueDays())
                .fineAmount(record.getFineAmount())
                .build())
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasOverdueBooks(Long userId) {
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowRecord::getUserId, userId);
        wrapper.eq(BorrowRecord::getStatus, Constants.BorrowStatus.BORROWING);
        wrapper.lt(BorrowRecord::getDueDate, LocalDateTime.now());
        wrapper.eq(BorrowRecord::getDeleted, 0);

        return borrowRecordMapper.selectCount(wrapper) > 0;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取分布式锁，失败则抛出异常
     */
    private void acquireLockOrThrow(RLock lock) throws InterruptedException {
        boolean locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
        if (!locked) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "系统繁忙，请稍后重试");
        }
    }

    /**
     * 释放分布式锁
     */
    private void releaseLock(RLock lock) {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 验证并获取用户
     */
    private User validateAndGetUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || Constants.UserStatus.DISABLED.equals(user.getStatus())) {
            throw new ResourceNotFoundException(ErrorCode.READER_NOT_FOUND, "用户不存在或已被禁用");
        }
        return user;
    }

    /**
     * 验证用户信用积分是否满足借阅门槛
     * 论文§3.2(2): 信用积分需达到60分的最低要求
     */
    private void validateCreditScore(User user) {
        if (user.getCreditScore() == null || user.getCreditScore() < Constants.Credit.BRONZE_THRESHOLD) {
            throw new BusinessException(ErrorCode.CREDIT_SCORE_LOW, "信用积分不足60分，无法借阅");
        }
    }

    /**
     * 验证用户是否有逾期图书
     */
    private void validateNoOverdueBooks(Long userId) {
        if (hasOverdueBooks(userId)) {
            throw new BusinessException(ErrorCode.BORROW_OVERDUE, "您有逾期未还的图书，请先归还");
        }
    }

    /**
     * 验证借阅数量限制
     */
    private void validateBorrowLimit(User user) {
        int currentBorrowCount = user.getBorrowCount() != null ? user.getBorrowCount() : 0;
        BorrowRule rule = borrowRuleService.getRuleEntity(user.getRole(), "NORMAL");
        int maxBorrow = rule.getMaxBorrow();
        if (user.getMaxBorrowCount() != null && user.getMaxBorrowCount() < maxBorrow) {
            maxBorrow = user.getMaxBorrowCount();
        }
        if (currentBorrowCount >= maxBorrow) {
            throw new BusinessException(ErrorCode.BORROW_LIMIT_EXCEEDED, "已达到最大借阅数量限制(" + maxBorrow + "本)");
        }
    }

    /**
     * 验证并获取图书
     */
    private Book validateAndGetBook(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.BOOK_NOT_FOUND, "图书不存在");
        }
        return book;
    }

    /**
     * 验证图书可借状态
     */
    private void validateBookAvailable(Book book) {
        if (!Constants.BookStatus.NORMAL.equals(book.getStatus())) {
            throw new BusinessException(ErrorCode.BOOK_NOT_AVAILABLE, "该图书当前不可借阅");
        }
        if (book.getAvailableCount() <= 0) {
            throw new BusinessException(ErrorCode.BOOK_STOCK_ERROR, "图书库存不足");
        }
    }

    /**
     * 验证并获取借阅天数
     */
    private int validateAndGetBorrowDays(Integer requestDays) {
        BorrowRule defaultRule = borrowRuleService.getRuleEntity(Constants.Role.READER, "NORMAL");
        int borrowDays = requestDays != null ? requestDays : defaultRule.getMaxDays();
        if (borrowDays < 1 || borrowDays > defaultRule.getMaxDays()) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "借阅天数必须在1-" + defaultRule.getMaxDays() + "天之间");
        }
        return borrowDays;
    }

    /**
     * 执行借阅操作（更新图书和用户数据）
     */
    private void performBorrowOperation(Book book, User user, int borrowDays) {
        int updated = bookMapper.updateAvailableCount(book.getId(), -1, book.getVersion(), 1);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BOOK_STOCK_ERROR, "图书库存更新失败，请重试");
        }

        int borrowCountResult = userMapper.updateBorrowCount(user.getId(), 1, user.getVersion());
        if (borrowCountResult == 0) {
            throw new BusinessException(ErrorCode.BORROW_LIMIT_EXCEEDED, "借阅数量已变更，请刷新后重试");
        }
    }

    /**
     * 创建借阅记录
     */
    private BorrowRecord createBorrowRecord(Long userId, User user, Book book, int borrowDays) {
        LocalDateTime borrowDateTime = LocalDateTime.now();
        LocalDateTime dueDateTime = borrowDateTime.plusDays(borrowDays);

        BorrowRecord record = BorrowRecord.builder()
                .userId(userId)
                .username(user.getUsername())
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .bookIsbn(book.getIsbn())
                .borrowDate(borrowDateTime)
                .dueDate(dueDateTime)
                .status(Constants.BorrowStatus.BORROWING)
                .renewCount(0)
                .overdueDays(0)
                .fineAmount(BigDecimal.ZERO)
                .build();

        borrowRecordMapper.insert(record);
        return record;
    }

    /**
     * 验证并获取借阅记录
     */
    private BorrowRecord validateAndGetRecord(Long borrowId, Long userId) {
        BorrowRecord record = borrowRecordMapper.selectBorrowWithJoinById(borrowId);
        if (record == null || record.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.BORROW_RECORD_NOT_FOUND, "借阅记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            throw new ForbiddenException(ErrorCode.INSUFFICIENT_PERMISSION, "无权操作此借阅记录");
        }
        if (!Constants.BorrowStatus.BORROWING.equals(record.getStatus())) {
            throw new BusinessException(ErrorCode.BORROW_ALREADY_RETURNED, "该图书已归还或状态异常");
        }
        return record;
    }

    /**
     * 计算逾期天数（排除寒暑假）
     */
    private int calculateOverdueDays(LocalDateTime dueDate, LocalDateTime returnDate) {
        if (returnDate.isAfter(dueDate)) {
            long totalDays = ChronoUnit.DAYS.between(dueDate, returnDate);
            long holidayDays = holidayUtil.countHolidayDaysBetween(
                    dueDate.toLocalDate(), returnDate.toLocalDate());
            return (int) Math.max(0, totalDays - holidayDays);
        }
        return 0;
    }

    /**
     * 计算罚款金额
     */
    private BigDecimal calculateFine(int overdueDays) {
        if (overdueDays <= 0) {
            return BigDecimal.ZERO;
        }
        return DAILY_FINE.multiply(new BigDecimal(overdueDays));
    }

    /**
     * 更新借阅记录（归还）
     */
    private void updateBorrowRecordForReturn(BorrowRecord record, LocalDateTime returnDate,
                                            int overdueDays, BigDecimal fineAmount) {
        record.setReturnDate(returnDate);
        record.setStatus(Constants.BorrowStatus.RETURNED);
        record.setOverdueDays(overdueDays);
        record.setFineAmount(fineAmount);
        borrowRecordMapper.updateById(record);
    }

    /**
     * 更新图书库存
     * FIXED: MEDIUM-FIX 检查updateAvailableCount返回值，防止库存不一致
     */
    private void updateBookStock(Book book) {
        int updated = bookMapper.updateAvailableCount(book.getId(), 1, book.getVersion(), 0);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_OPERATION, "图书库存更新失败，请重试");
        }
    }

    /**
     * 更新用户借阅数量
     */
    private void updateUserBorrowCount(User user) {
        int returnCountResult = userMapper.updateBorrowCount(user.getId(), -1, user.getVersion());
        if (returnCountResult == 0) {
            throw new BusinessException(ErrorCode.CONCURRENT_OPERATION, "借阅数量更新失败，请重试");
        }
    }

    /**
     * 验证记录未逾期
     */
    private void validateRecordNotOverdue(BorrowRecord record) {
        if (LocalDateTime.now().isAfter(record.getDueDate())) {
            throw new BusinessException(ErrorCode.BORROW_OVERDUE, "图书已逾期，无法续借");
        }
    }

    /**
     * 验证续借次数
     */
    private void validateRenewCount(BorrowRecord record) {
        BorrowRule rule = borrowRuleService.getRuleEntity(Constants.Role.READER, "NORMAL");
        if (record.getRenewCount() >= rule.getMaxRenew()) {
            throw new BusinessException(ErrorCode.RENEW_LIMIT_EXCEEDED, "已达到最大续借次数限制(" + rule.getMaxRenew() + "次)");
        }
    }

    /**
     * 执行续借操作
     */
    private void performRenewOperation(BorrowRecord record, int renewDays) {
        BorrowRule rule = borrowRuleService.getRuleEntity(Constants.Role.READER, "NORMAL");
        int actualRenewDays = renewDays > 0 ? renewDays : rule.getRenewDays();
        record.setDueDate(record.getDueDate().plusDays(actualRenewDays));
        record.setRenewCount(record.getRenewCount() + 1);
        borrowRecordMapper.updateById(record);
    }

    /**
     * 将BorrowRecord实体转换为BorrowResponse DTO
     */
    private BorrowResponse convertToResponse(BorrowRecord record) {
        return BorrowResponse.builder()
                .id(record.getId())
                .username(record.getUsername())
                .bookId(record.getBookId())
                .bookTitle(record.getBookTitle())
                .bookIsbn(record.getBookIsbn())
                .borrowDate(record.getBorrowDate())
                .dueDate(record.getDueDate())
                .returnDate(record.getReturnDate())
                .status(record.getStatus())
                .renewCount(record.getRenewCount())
                .overdueDays(record.getOverdueDays())
                .fineAmount(record.getFineAmount())
                .createTime(record.getCreateTime())
                .build();
    }
}
