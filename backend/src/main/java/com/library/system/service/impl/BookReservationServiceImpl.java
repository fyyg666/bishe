package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.common.Constants;
import com.library.system.dto.BookReservationResponse;
import com.library.system.dto.PageResult;
import com.library.system.entity.Book;
import com.library.system.entity.BookReservation;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ForbiddenException;
import com.library.system.mapper.BookMapper;
import com.library.system.mapper.BookReservationMapper;
import com.library.system.service.BookReservationService;
import com.library.system.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookReservationServiceImpl implements BookReservationService {

    private final BookReservationMapper bookReservationMapper;
    private final BookMapper bookMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookReservationResponse createReservation(Long userId, Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND, "图书不存在");
        }
        if (book.getAvailableCount() > 0) {
            throw new BusinessException(ErrorCode.BOOK_STOCK_ERROR, "图书有库存，无需预约排队");
        }

        LambdaQueryWrapper<BookReservation> existsWrapper = new LambdaQueryWrapper<>();
        existsWrapper.eq(BookReservation::getUserId, userId)
                     .eq(BookReservation::getBookId, bookId)
                     .in(BookReservation::getStatus, List.of(Constants.ReservationStatus.PENDING, "NOTIFIED"))
                     .eq(BookReservation::getDeleted, 0);
        if (bookReservationMapper.selectCount(existsWrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "您已预约该图书，请勿重复预约");
        }

        BookReservation reservation = BookReservation.builder()
                .userId(userId)
                .bookId(bookId)
                .status(Constants.ReservationStatus.PENDING)
                .build();
        bookReservationMapper.insert(reservation);

        int queuePosition = bookReservationMapper.countPendingByBookId(bookId);

        return BookReservationResponse.builder()
                .id(reservation.getId())
                .userId(userId)
                .bookId(bookId)
                .bookTitle(book.getTitle())
                .status(Constants.ReservationStatus.PENDING)
                .queuePosition(queuePosition)
                .createTime(reservation.getCreateTime())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelReservation(Long userId, Long reservationId) {
        BookReservation reservation = bookReservationMapper.selectById(reservationId);
        if (reservation == null || reservation.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "预约记录不存在");
        }
        if (!reservation.getUserId().equals(userId)) {
            throw new ForbiddenException(ErrorCode.INSUFFICIENT_PERMISSION, "无权操作此预约记录");
        }
        if (!Constants.ReservationStatus.PENDING.equals(reservation.getStatus()) && !"NOTIFIED".equals(reservation.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "只能取消排队中或已通知的预约");
        }
        reservation.setStatus(Constants.ReservationStatus.CANCELLED);
        bookReservationMapper.updateById(reservation);
        log.info("图书预约取消: userId={}, reservationId={}", userId, reservationId);
    }

    @Override
    public PageResult<BookReservationResponse> getMyReservations(Long userId, Long current, Long size) {
        Page<BookReservation> page = new Page<>(current, size);
        LambdaQueryWrapper<BookReservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookReservation::getUserId, userId)
               .eq(BookReservation::getDeleted, 0)
               .orderByDesc(BookReservation::getCreateTime);
        Page<BookReservation> resultPage = bookReservationMapper.selectPage(page, wrapper);

        List<BookReservationResponse> records = resultPage.getRecords().stream()
                .map(r -> {
                    Book book = bookMapper.selectById(r.getBookId());
                    int queuePos = Constants.ReservationStatus.PENDING.equals(r.getStatus())
                            ? bookReservationMapper.countPendingByBookId(r.getBookId()) : 0;
                    return BookReservationResponse.builder()
                            .id(r.getId())
                            .userId(r.getUserId())
                            .bookId(r.getBookId())
                            .bookTitle(book != null ? book.getTitle() : "未知")
                            .status(r.getStatus())
                            .queuePosition(queuePos)
                            .createTime(r.getCreateTime())
                            .notifyTime(r.getNotifyTime())
                            .build();
                })
                .collect(Collectors.toList());

        return PageResult.of(resultPage.getCurrent(), resultPage.getSize(),
                resultPage.getTotal(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void notifyNextInQueue(Long bookId) {
        LambdaQueryWrapper<BookReservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookReservation::getBookId, bookId)
               .eq(BookReservation::getStatus, Constants.ReservationStatus.PENDING)
               .eq(BookReservation::getDeleted, 0)
               .orderByAsc(BookReservation::getCreateTime)
               .last("LIMIT 1");
        BookReservation next = bookReservationMapper.selectOne(wrapper);
        if (next == null) {
            return;
        }

        next.setStatus("NOTIFIED");
        next.setNotifyTime(LocalDateTime.now());
        bookReservationMapper.updateById(next);

        Book book = bookMapper.selectById(bookId);
        String bookTitle = book != null ? book.getTitle() : "未知图书";

        try {
            notificationService.createNotification(next.getUserId(), "图书可借通知",
                    "您预约的《" + bookTitle + "》已有库存，请在48小时内借阅",
                    "BOOK_AVAILABLE", next.getId());
        } catch (Exception e) {
            log.warn("预约通知发送失败: userId={}, bookId={}", next.getUserId(), bookId, e);
        }

        log.info("图书预约通知: userId={}, bookId={}, reservationId={}", next.getUserId(), bookId, next.getId());
    }
}
