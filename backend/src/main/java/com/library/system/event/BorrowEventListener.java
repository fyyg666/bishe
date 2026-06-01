package com.library.system.event;

import com.library.system.service.CreditService;
import com.library.system.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class BorrowEventListener {

    private final NotificationService notificationService;
    private final CreditService creditService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBorrowEvent(BorrowEvent event) {
        try {
            switch (event.getType()) {
                case "BORROW" -> handleBorrow(event);
                case "RETURN" -> handleReturn(event);
                case "RENEW" -> handleRenew(event);
            }
        } catch (Exception e) {
            log.error("处理借阅事件失败: type={}, borrowId={}", event.getType(), event.getBorrowId(), e);
        }
    }

    private void handleBorrow(BorrowEvent event) {
        creditService.processBorrowCredit(event.getUserId(), event.getBorrowId());
        notificationService.createNotification(event.getUserId(), "借阅成功",
                "您已成功借阅《" + event.getBookTitle() + "》，到期日为" + event.getDueDate().toLocalDate(),
                "BORROW", event.getBorrowId());
    }

    private void handleReturn(BorrowEvent event) {
        creditService.processReturnCredit(event.getUserId(), event.getBorrowId(),
                event.getOverdueDays(), event.getDueDate(), event.getReturnDate());
        String content = event.getOverdueDays() > 0
                ? "您已归还《" + event.getBookTitle() + "》，逾期" + event.getOverdueDays() + "天，罚款" + event.getFineAmount() + "元"
                : "您已归还《" + event.getBookTitle() + "》，感谢按时归还";
        notificationService.createNotification(event.getUserId(), "归还成功", content,
                "RETURN", event.getBorrowId());
    }

    private void handleRenew(BorrowEvent event) {
        notificationService.createNotification(event.getUserId(), "续借成功",
                "您已续借《" + event.getBookTitle() + "》，新到期日为" + event.getDueDate().toLocalDate(),
                "RENEW", event.getBorrowId());
    }
}
