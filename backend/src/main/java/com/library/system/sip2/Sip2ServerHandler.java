package com.library.system.sip2;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.system.dto.BorrowRequest;
import com.library.system.entity.Book;
import com.library.system.entity.BorrowRecord;
import com.library.system.entity.User;
import com.library.system.mapper.BookMapper;
import com.library.system.mapper.BorrowRecordMapper;
import com.library.system.mapper.UserMapper;
import com.library.system.service.BorrowService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class Sip2ServerHandler extends SimpleChannelInboundHandler<String> {

    private final UserMapper userMapper;
    private final BookMapper bookMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final BorrowService borrowService;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        msg = msg.trim();
        log.info("SIP2收到消息: {}", msg.length() > 100 ? msg.substring(0, 100) + "..." : msg);

        try {
            Sip2Message request = Sip2Message.parse(msg);
            String response = handleRequest(request);
            ctx.writeAndFlush(response);
        } catch (Exception e) {
            log.error("SIP2消息处理异常: {}", e.getMessage());
            ctx.writeAndFlush("96" + "\r");
        }
    }

    private String handleRequest(Sip2Message request) {
        return switch (request.getCode()) {
            case "93" -> request.buildAcsStatus();
            case "29" -> handleCheckout(request);
            case "09" -> handleCheckin(request);
            case "23" -> handlePatronStatus(request);
            case "63" -> handlePatronStatus(request);
            case "17" -> handleItemInformation(request);
            case "35" -> "36" + request.getTransactionDate() + "AO" + request.getInstitutionId() + "|BIY|\r";
            default -> "96" + "\r";
        };
    }

    private String handleCheckout(Sip2Message request) {
        try {
            User user = findUserByCard(request.getPatronIdentifier());
            Book book = findBookByBarcode(request.getItemIdentifier());

            if (user == null || book == null) {
                return request.buildCheckoutResponse(false, null, "");
            }

            BorrowRequest borrowRequest = BorrowRequest.builder()
                    .bookId(book.getId())
                    .borrowDays(30)
                    .build();
            borrowService.borrowBook(user.getId(), borrowRequest);

            String dueDate = java.time.LocalDateTime.now().plusDays(30)
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd    HHmmss"));
            return request.buildCheckoutResponse(true, book.getTitle(), dueDate);
        } catch (Exception e) {
            log.warn("SIP2借出失败: {}", e.getMessage());
            return request.buildCheckoutResponse(false, null, "");
        }
    }

    private String handleCheckin(Sip2Message request) {
        try {
            Book book = findBookByBarcode(request.getItemIdentifier());
            if (book == null) {
                return request.buildCheckinResponse(false, null);
            }

            BorrowRecord record = borrowRecordMapper.selectOne(
                    new LambdaQueryWrapper<BorrowRecord>()
                            .eq(BorrowRecord::getBookId, book.getId())
                            .eq(BorrowRecord::getStatus, "BORROWING")
                            .eq(BorrowRecord::getDeleted, 0)
                            .last("LIMIT 1"));
            if (record == null) {
                return request.buildCheckinResponse(false, null);
            }

            borrowService.returnBook(record.getUserId(), record.getId());
            return request.buildCheckinResponse(true, book.getTitle());
        } catch (Exception e) {
            log.warn("SIP2归还失败: {}", e.getMessage());
            return request.buildCheckinResponse(false, null);
        }
    }

    private String handlePatronStatus(Sip2Message request) {
        User user = findUserByCard(request.getPatronIdentifier());
        if (user == null) {
            return request.buildPatronStatusResponse(false, null, 0);
        }
        return request.buildPatronStatusResponse(true, user.getRealName(),
                user.getBorrowCount() != null ? user.getBorrowCount() : 0);
    }

    private String handleItemInformation(Sip2Message request) {
        Book book = findBookByBarcode(request.getItemIdentifier());
        if (book == null) {
            return request.buildItemInformationResponse(false, null, null);
        }
        return request.buildItemInformationResponse(
                book.getAvailableCount() != null && book.getAvailableCount() > 0,
                book.getTitle(), book.getAuthor());
    }

    private User findUserByCard(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) return null;
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getCardNumber, cardNumber)
                .eq(User::getDeleted, 0)
                .last("LIMIT 1"));
    }

    private Book findBookByBarcode(String barcode) {
        if (barcode == null || barcode.isEmpty()) return null;
        return bookMapper.selectOne(new LambdaQueryWrapper<Book>()
                .eq(Book::getIsbn, barcode)
                .eq(Book::getDeleted, 0)
                .last("LIMIT 1"));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("SIP2连接异常: {}", cause.getMessage());
        ctx.close();
    }
}
