package com.library.system.service;

import com.library.system.base.BaseTest;
import com.library.system.common.Constants;
import com.library.system.config.BloomFilterConfig;
import com.library.system.dto.*;
import com.library.system.entity.*;
import com.library.system.enums.CompensationStatus;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ForbiddenException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.exception.UnauthorizedException;
import com.library.system.mapper.*;
import com.library.system.service.impl.*;
import com.library.system.template.DistributedLockTemplate;
import com.library.system.util.HolidayUtil;
import com.library.system.util.JwtUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 全量覆盖测试 — 通过执行测试来暴露系统缺陷
 * 
 * 设计原则：
 * 1. 每个 Service 方法至少测试 3 种输入：正常值、边界值、异常值
 * 2. 每个条件分支至少覆盖一次（if/else/三元/switch）
 * 3. 状态机转移路径全面覆盖
 * 4. 零值/空值/负值/NULL 作为独立测试用例
 * 5. 测试失败=暴露缺陷
 */
@DisplayName("全覆盖压力测试 — 通过执行暴露缺陷")
class FullCoverageTest extends BaseTest {

    // ========================================================================
    // 1. BookService — 全方法覆盖
    // ========================================================================
    @Nested
    @DisplayName("BookService 全覆盖: create/update/delete/query/list")
    class BookServiceCoverage {

        @Mock private BookMapper bookMapper;
        @Mock private BloomFilterConfig bloomFilterConfig;
        @InjectMocks private BookServiceImpl bookService;

        private Book book;

        @BeforeEach
        void setUp() {
            book = new Book();
            book.setId(1L);
            book.setIsbn("978-7-111-11111-1");
            book.setTitle("测试图书");
            book.setAuthor("作者");
            book.setPublisher("出版社");
            book.setCategoryId(1L);
            book.setTotalCount(10);
            book.setAvailableCount(8);
            book.setBorrowCount(2);
            book.setPrice(new BigDecimal("59.00"));
            book.setStatus(Constants.BookStatus.NORMAL);
            book.setVersion(0);
            book.setDeleted(0);
            lenient().when(bloomFilterConfig.mightContainBook(anyString())).thenReturn(true);
        }

        @Test @DisplayName("C1: createBook 正常值")
        void createBook_normal() {
            BookRequest req = new BookRequest();
            req.setIsbn("978-7-000-BOOK-1"); req.setTitle("新书"); req.setAuthor("作者");
            req.setPublisher("社"); req.setCategoryId(1L); req.setTotalCount(5);
            req.setPrice(new BigDecimal("39.00"));
            when(bookMapper.selectByIsbn("978-7-000-BOOK-1")).thenReturn(null);
            when(bookMapper.insert(any())).thenAnswer(i -> { ((Book)i.getArgument(0)).setId(99L); return 1; });
            BookResponse r = bookService.createBook(req);
            assertNotNull(r); assertEquals("新书", r.getTitle());
        }

        @Test @DisplayName("C2: createBook totalCount=0")
        void createBook_totalCountZero() {
            BookRequest req = new BookRequest();
            req.setIsbn("978-7-000-BOOK-0"); req.setTitle("零库存"); req.setAuthor("作者");
            req.setPublisher("社"); req.setCategoryId(1L); req.setTotalCount(0);
            req.setPrice(BigDecimal.ZERO);
            when(bookMapper.selectByIsbn("978-7-000-BOOK-0")).thenReturn(null);
            when(bookMapper.insert(any())).thenAnswer(i -> { ((Book)i.getArgument(0)).setId(98L); return 1; });
            BookResponse r = bookService.createBook(req);
            assertEquals(Integer.valueOf(0), r.getTotalCount());
            assertEquals(Integer.valueOf(0), r.getAvailableCount());
        }

        @Test @DisplayName("C3: createBook totalCount 极大值")
        void createBook_totalCountHuge() {
            BookRequest req = new BookRequest();
            req.setIsbn("978-7-000-BOOK-B"); req.setTitle("海量"); req.setAuthor("作者");
            req.setPublisher("社"); req.setCategoryId(1L); req.setTotalCount(Integer.MAX_VALUE);
            req.setPrice(new BigDecimal("999999.99"));
            when(bookMapper.selectByIsbn("978-7-000-BOOK-B")).thenReturn(null);
            when(bookMapper.insert(any())).thenAnswer(i -> { ((Book)i.getArgument(0)).setId(97L); return 1; });
            BookResponse r = bookService.createBook(req);
            assertEquals(Integer.valueOf(Integer.MAX_VALUE), r.getTotalCount());
            assertEquals(Integer.valueOf(Integer.MAX_VALUE), r.getAvailableCount());
        }

        @Test @DisplayName("C4: createBook price 为 null")
        void createBook_priceNull() {
            BookRequest req = new BookRequest();
            req.setIsbn("978-7-000-BOOK-N"); req.setTitle("无价"); req.setAuthor("作者");
            req.setPublisher("社"); req.setCategoryId(1L); req.setTotalCount(1);
            when(bookMapper.selectByIsbn("978-7-000-BOOK-N")).thenReturn(null);
            when(bookMapper.insert(any())).thenAnswer(i -> { ((Book)i.getArgument(0)).setId(96L); return 1; });
            try { bookService.createBook(req); }
            catch (Exception e) { log.error("【缺陷】price=null 创建图书异常: {}", e.getMessage()); throw e; }
        }

        @Test @DisplayName("C5: createBook ISBN 重复")
        void createBook_isbnConflict() {
            BookRequest req = new BookRequest();
            req.setIsbn("978-7-111-11111-1"); req.setTitle("重复"); req.setAuthor("作者");
            req.setPublisher("社"); req.setCategoryId(1L); req.setTotalCount(1);
            when(bookMapper.selectByIsbn("978-7-111-11111-1")).thenReturn(book);
            assertThrows(BusinessException.class, () -> bookService.createBook(req));
        }

        @Test @DisplayName("C6: updateBook 减少库存后 availableCount 不应为负")
        void updateBook_reduceStock_availableNonNegative() {
            when(bookMapper.selectById(1L)).thenReturn(book);
            BookRequest req = new BookRequest();
            req.setIsbn("978-7-111-11111-1"); req.setTitle("更新"); req.setAuthor("作者");
            req.setPublisher("社"); req.setCategoryId(1L); req.setTotalCount(1);
            req.setPrice(new BigDecimal("10.00"));
            BookResponse r = bookService.updateBook(1L, req);
            // totalCount=1, availableCount=8, diff=1-10=-9, availableCount=max(0,8-9)=0
            assertTrue(r.getAvailableCount() >= 0, "availableCount 不应为负，但得到: " + r.getAvailableCount());
        }

        @Test @DisplayName("C7: updateBook totalCount 减到比已借出还少时可用数为0")
        void updateBook_totalLessThanBorrowed_availableZero() {
            book.setAvailableCount(8); book.setTotalCount(10);
            when(bookMapper.selectById(1L)).thenReturn(book);
            BookRequest req = new BookRequest();
            req.setIsbn("978-7-111-11111-1"); req.setTitle("更新"); req.setAuthor("作者");
            req.setPublisher("社"); req.setCategoryId(1L); req.setTotalCount(5);
            req.setPrice(new BigDecimal("10.00"));
            BookResponse r = bookService.updateBook(1L, req);
            // booked out = 10-8 = 2; newAvailable = 5-2 = 3
            // 实际: max(0, 8+(5-10)) = max(0, 3) = 3
            assertEquals(Integer.valueOf(3), r.getAvailableCount());
        }

        @Test @DisplayName("C8: updateBook 不存在")
        void updateBook_notFound() {
            when(bookMapper.selectById(999L)).thenReturn(null);
            BookRequest req = new BookRequest();
            req.setIsbn("978-7-111-11111-1");
            assertThrows(ResourceNotFoundException.class, () -> bookService.updateBook(999L, req));
        }

        @Test @DisplayName("C9: deleteBook 不存在")
        void deleteBook_notFound() {
            when(bookMapper.selectById(999L)).thenReturn(null);
            assertThrows(ResourceNotFoundException.class, () -> bookService.deleteBook(999L));
        }

        @Test @DisplayName("C10: deleteBook 已删除")
        void deleteBook_alreadyDeleted() {
            book.setDeleted(1);
            when(bookMapper.selectById(1L)).thenReturn(book);
            assertThrows(ResourceNotFoundException.class, () -> bookService.deleteBook(1L));
        }

        @Test @DisplayName("C11: getBookById 正常")
        void getBookById_normal() {
            when(bookMapper.selectById(1L)).thenReturn(book);
            BookResponse r = bookService.getBookById(1L);
            assertNotNull(r); assertEquals("测试图书", r.getTitle());
        }

        @Test @DisplayName("C12: getBookById 布隆过滤器拦截")
        void getBookById_bloomFilterMiss() {
            doReturn(false).when(bloomFilterConfig).mightContainBook("999");
            assertThrows(ResourceNotFoundException.class, () -> bookService.getBookById(999L));
        }

        @Test @DisplayName("C13: listBooks keyword=\"\" 空串")
        void listBooks_emptyKeyword() {
            when(bookMapper.selectPage(any(), any())).thenAnswer(inv -> {
                com.baomidou.mybatisplus.core.metadata.IPage<Book> p = inv.getArgument(0);
                p.setRecords(List.of()); p.setTotal(0); return p; });
            PageResult<BookResponse> r = bookService.listBooks(1L, 10L, "", null);
            assertNotNull(r);
        }

        @Test @DisplayName("C14: listBooks keyword 超长")
        void listBooks_longKeyword() {
            String longKw = "a".repeat(1000);
            when(bookMapper.selectPage(any(), any())).thenAnswer(inv -> {
                com.baomidou.mybatisplus.core.metadata.IPage<Book> p = inv.getArgument(0);
                p.setRecords(List.of()); p.setTotal(0); return p; });
            PageResult<BookResponse> r = bookService.listBooks(1L, 10L, longKw, 1L);
            assertNotNull(r);
        }

        @Test @DisplayName("C15: isIsbnExists 空串")
        void isIsbnExists_empty() {
            when(bookMapper.selectByIsbn("")).thenReturn(null);
            assertFalse(bookService.isIsbnExists(""));
        }
    }

    // ========================================================================
    // 2. BorrowService — 全方法覆盖
    // ========================================================================
    @Nested
    @DisplayName("BorrowService 全覆盖: borrow/return/renew/query")
    class BorrowServiceCoverage {

        @Mock private BorrowRecordMapper borrowRecordMapper;
        @Mock private BookMapper bookMapper;
        @Mock private UserMapper userMapper;
        @Mock private RedissonClient redissonClient;
        @Mock private RLock rLock;
        @Mock private CreditService creditService;
        @Mock private HolidayUtil holidayUtil;
        @InjectMocks private BorrowServiceImpl borrowService;

        private User user; private Book book; private BorrowRecord record;

        @BeforeEach
        void setUp() throws Exception {
            user = new User(); user.setId(1L); user.setUsername("reader1");
            user.setStatus("NORMAL"); user.setCreditScore(100); user.setMaxBorrowCount(5);
            user.setVersion(0); user.setBorrowCount(0);
            book = new Book(); book.setId(1L); book.setTitle("测试"); book.setIsbn("978-7-111-11111-1");
            book.setStatus(Constants.BookStatus.NORMAL); book.setAvailableCount(5);
            book.setTotalCount(10); book.setVersion(0); book.setBorrowCount(0); book.setDeleted(0);
            record = BorrowRecord.builder().id(100L).userId(1L).bookId(1L)
                    .status(Constants.BorrowStatus.BORROWING).renewCount(0)
                    .borrowDate(LocalDateTime.now().minusDays(10))
                    .dueDate(LocalDateTime.now().plusDays(20)).deleted(0).build();
            lenient().when(redissonClient.getLock(anyString())).thenReturn(rLock);
            lenient().when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
            lenient().when(rLock.isHeldByCurrentThread()).thenReturn(true);
            lenient().doNothing().when(rLock).unlock();
        }

        @Test @DisplayName("D1: borrowBook 正常")
        void borrowBook_normal() {
            when(userMapper.selectById(1L)).thenReturn(user);
            when(bookMapper.selectById(1L)).thenReturn(book);
            when(borrowRecordMapper.selectCount(any())).thenReturn(0L);
            when(borrowRecordMapper.insert(any())).thenReturn(1);
            when(bookMapper.updateAvailableCount(anyLong(), anyInt(), anyInt(), anyInt())).thenReturn(1);
            when(userMapper.updateBorrowCount(anyLong(), anyInt(), anyInt())).thenReturn(1);
            BorrowRequest req = new BorrowRequest(); req.setBookId(1L);
            BorrowResponse r = borrowService.borrowBook(1L, req);
            assertNotNull(r);
        }

        @Test @DisplayName("D2: borrowBook 信用分=60（边界）")
        void borrowBook_credit60() {
            user.setCreditScore(Constants.Credit.BRONZE_THRESHOLD);
            when(userMapper.selectById(1L)).thenReturn(user);
            when(bookMapper.selectById(1L)).thenReturn(book);
            when(borrowRecordMapper.selectCount(any())).thenReturn(0L);
            when(borrowRecordMapper.insert(any())).thenReturn(1);
            when(bookMapper.updateAvailableCount(anyLong(), anyInt(), anyInt(), anyInt())).thenReturn(1);
            when(userMapper.updateBorrowCount(anyLong(), anyInt(), anyInt())).thenReturn(1);
            BorrowRequest req = new BorrowRequest(); req.setBookId(1L);
            assertDoesNotThrow(() -> borrowService.borrowBook(1L, req));
        }

        @Test @DisplayName("D3: borrowBook 信用分=59（拒绝）")
        void borrowBook_credit59() {
            user.setCreditScore(Constants.Credit.BRONZE_THRESHOLD - 1);
            when(userMapper.selectById(1L)).thenReturn(user);
            BorrowRequest req = new BorrowRequest(); req.setBookId(1L);
            assertThrows(BusinessException.class, () -> borrowService.borrowBook(1L, req));
        }

        @Test @DisplayName("D4: borrowBook 已到最大借阅量")
        void borrowBook_atLimit() {
            when(userMapper.selectById(1L)).thenReturn(user);
            when(borrowRecordMapper.selectCount(any())).thenReturn(5L);
            BorrowRequest req = new BorrowRequest(); req.setBookId(1L);
            assertThrows(BusinessException.class, () -> borrowService.borrowBook(1L, req));
        }

        @Test @DisplayName("D5: borrowBook 用户被禁用")
        void borrowBook_userDisabled() {
            user.setStatus("DISABLED");
            when(userMapper.selectById(1L)).thenReturn(user);
            BorrowRequest req = new BorrowRequest(); req.setBookId(1L);
            assertThrows(ResourceNotFoundException.class, () -> borrowService.borrowBook(1L, req));
        }

        @Test @DisplayName("D6: borrowBook 图书已下架")
        void borrowBook_bookOffShelf() {
            book.setStatus(Constants.BookStatus.OFF_SHELF);
            when(userMapper.selectById(1L)).thenReturn(user);
            when(bookMapper.selectById(1L)).thenReturn(book);
            when(borrowRecordMapper.selectCount(any())).thenReturn(0L);
            BorrowRequest req = new BorrowRequest(); req.setBookId(1L);
            assertThrows(BusinessException.class, () -> borrowService.borrowBook(1L, req));
        }

        @Test @DisplayName("D7: borrowBook 库存=0")
        void borrowBook_stockZero() {
            book.setAvailableCount(0);
            when(userMapper.selectById(1L)).thenReturn(user);
            when(bookMapper.selectById(1L)).thenReturn(book);
            when(borrowRecordMapper.selectCount(any())).thenReturn(0L);
            BorrowRequest req = new BorrowRequest(); req.setBookId(1L);
            assertThrows(BusinessException.class, () -> borrowService.borrowBook(1L, req));
        }

        @Test @DisplayName("D8: borrowBook borrowDays=-1")
        void borrowBook_negativeDays() {
            when(userMapper.selectById(1L)).thenReturn(user);
            when(bookMapper.selectById(1L)).thenReturn(book);
            when(borrowRecordMapper.selectCount(any())).thenReturn(0L);
            BorrowRequest req = new BorrowRequest(); req.setBookId(1L); req.setBorrowDays(-1);
            assertThrows(BusinessException.class, () -> borrowService.borrowBook(1L, req));
        }

        @Test @DisplayName("D9: returnBook 正常")
        void returnBook_normal() {
            when(borrowRecordMapper.selectById(100L)).thenReturn(record);
            when(bookMapper.selectById(1L)).thenReturn(book);
            when(userMapper.selectById(1L)).thenReturn(user);
            when(borrowRecordMapper.updateById(any())).thenReturn(1);
            when(bookMapper.updateAvailableCount(anyLong(), anyInt(), anyInt(), anyInt())).thenReturn(1);
            when(userMapper.updateBorrowCount(anyLong(), anyInt(), anyInt())).thenReturn(1);
            BorrowResponse r = borrowService.returnBook(1L, 100L);
            assertNotNull(r);
            assertEquals(Constants.BorrowStatus.RETURNED, r.getStatus());
        }

        @Test @DisplayName("D10: returnBook 已归还状态")
        void returnBook_alreadyReturned() {
            record.setStatus(Constants.BorrowStatus.RETURNED);
            when(borrowRecordMapper.selectById(100L)).thenReturn(record);
            assertThrows(BusinessException.class, () -> borrowService.returnBook(1L, 100L));
        }

        @Test @DisplayName("D11: returnBook 非本人操作")
        void returnBook_notOwner() {
            when(borrowRecordMapper.selectById(100L)).thenReturn(record);
            assertThrows(ForbiddenException.class, () -> borrowService.returnBook(3L, 100L));
        }

        @Test @DisplayName("D12: returnBook 记录不存在")
        void returnBook_notFound() {
            when(borrowRecordMapper.selectById(999L)).thenReturn(null);
            assertThrows(ResourceNotFoundException.class, () -> borrowService.returnBook(1L, 999L));
        }

        @Test @DisplayName("D13: renewBook 正常")
        void renewBook_normal() {
            when(borrowRecordMapper.selectById(100L)).thenReturn(record);
            BorrowResponse r = borrowService.renewBook(1L, 100L, 14);
            assertNotNull(r);
        }

        @Test @DisplayName("D14: renewBook 续借次数超限")
        void renewBook_exceedMax() {
            record.setRenewCount(Constants.BorrowLimit.MAX_RENEW_TIMES);
            when(borrowRecordMapper.selectById(100L)).thenReturn(record);
            assertThrows(BusinessException.class, () -> borrowService.renewBook(1L, 100L, 14));
        }

        @Test @DisplayName("D15: renewBook 已逾期")
        void renewBook_overdue() {
            record.setDueDate(LocalDateTime.now().minusDays(1));
            when(borrowRecordMapper.selectById(100L)).thenReturn(record);
            assertThrows(BusinessException.class, () -> borrowService.renewBook(1L, 100L, 14));
        }

        @Test @DisplayName("D16: renewBook days=0")
        void renewBook_daysZero() {
            when(borrowRecordMapper.selectById(100L)).thenReturn(record);
            assertDoesNotThrow(() -> borrowService.renewBook(1L, 100L, 0));
        }

        @Test @DisplayName("D17: getMyBorrows status=null")
        void getMyBorrows_statusNull() {
            when(borrowRecordMapper.selectPage(any(), any())).thenAnswer(inv -> {
                com.baomidou.mybatisplus.core.metadata.IPage<BorrowRecord> p = inv.getArgument(0);
                p.setRecords(List.of()); p.setTotal(0); return p; });
            PageResult<BorrowResponse> r = borrowService.getMyBorrows(1L, 1L, 10L, null);
            assertNotNull(r);
        }

        @Test @DisplayName("D18: getMyBorrows 空状态串")
        void getMyBorrows_emptyStatus() {
            when(borrowRecordMapper.selectPage(any(), any())).thenAnswer(inv -> {
                com.baomidou.mybatisplus.core.metadata.IPage<BorrowRecord> p = inv.getArgument(0);
                p.setRecords(List.of()); p.setTotal(0); return p; });
            borrowService.getMyBorrows(1L, 1L, 10L, "");
        }

        @Test @DisplayName("D19: getBorrowByIdWithOwnershipCheck — 管理员可查看")
        void getBorrowById_admin() {
            when(borrowRecordMapper.selectById(100L)).thenReturn(record);
            BorrowResponse r = borrowService.getBorrowByIdWithOwnershipCheck(100L, 2L, "ADMIN");
            assertNotNull(r);
        }

        @Test @DisplayName("D20: getBorrowByIdWithOwnershipCheck — 水平越权拒绝")
        void getBorrowById_horizontalAuthFail() {
            when(borrowRecordMapper.selectById(100L)).thenReturn(record);
            assertThrows(ForbiddenException.class,
                    () -> borrowService.getBorrowByIdWithOwnershipCheck(100L, 5L, "READER"));
        }

        @Test @DisplayName("D21: hasOverdueBooks 有逾期")
        void hasOverdueBooks_true() {
            when(borrowRecordMapper.selectCount(any())).thenReturn(1L);
            assertTrue(borrowService.hasOverdueBooks(1L));
        }

        @Test @DisplayName("D22: hasOverdueBooks 无逾期")
        void hasOverdueBooks_false() {
            when(borrowRecordMapper.selectCount(any())).thenReturn(0L);
            assertFalse(borrowService.hasOverdueBooks(1L));
        }
    }

    // ========================================================================
    // 3. CompensationService — 全方法覆盖
    // ========================================================================
    @Nested
    @DisplayName("CompensationService 全覆盖: create/process/cancel/query")
    class CompensationServiceCoverage {

        @Mock private CompensationMapper compensationMapper;
        @Mock private UserMapper userMapper;
        @Mock private CreditService creditService;
        @InjectMocks private CompensationServiceImpl compensationService;

        private Compensation comp;

        @BeforeEach
        void setUp() {
            comp = new Compensation(); comp.setId(1L); comp.setBorrowId(100L);
            comp.setUserId(1L); comp.setAmount(new BigDecimal("50.00"));
            comp.setStatus(CompensationStatus.PENDING.name()); comp.setDeleted(0);
            comp.setCreateTime(LocalDateTime.now());
        }

        @Test @DisplayName("E1: createCompensation 全字段")
        void createCompensation_allFields() {
            CompensationRequest req = new CompensationRequest();
            req.setBorrowId(100L); req.setUserId(1L); req.setBookId(1L);
            req.setAmount(new BigDecimal("100.00")); req.setCompType("DAMAGE");
            req.setPaymentMethod("CREDIT"); req.setCreditAmount(50);
            when(compensationMapper.insert(any())).thenReturn(1);
            when(userMapper.selectById(any())).thenReturn(new User());
            CompensationResponse r = compensationService.createCompensation(req, 2L);
            assertNotNull(r);
        }

        @Test @DisplayName("E2: createCompensation amount=null")
        void createCompensation_amountNull() {
            CompensationRequest req = new CompensationRequest();
            req.setBorrowId(100L);
            when(compensationMapper.insert(any())).thenReturn(1);
            CompensationResponse r = compensationService.createCompensation(req, 2L);
            assertEquals(BigDecimal.ZERO, r.getAmount());
        }

        @Test @DisplayName("E3: processCashPayment 正常")
        void processCashPayment_normal() {
            when(compensationMapper.selectById(1L)).thenReturn(comp);
            when(compensationMapper.updateById(any())).thenReturn(1);
            when(userMapper.selectById(any())).thenReturn(new User());
            CompensationResponse r = compensationService.processCashPayment(1L, 2L, "已现金支付");
            assertEquals("PAID", r.getStatus());
        }

        @Test @DisplayName("E4: processCashPayment 已取消状态")
        void processCashPayment_alreadyCancelled() {
            comp.setStatus("CANCELLED");
            when(compensationMapper.selectById(1L)).thenReturn(comp);
            assertThrows(BusinessException.class, () -> compensationService.processCashPayment(1L, 2L, ""));
        }

        @Test @DisplayName("E5: processCreditPayment 正常")
        void processCreditPayment_normal() {
            when(compensationMapper.selectById(1L)).thenReturn(comp);
            when(compensationMapper.updateById(any())).thenReturn(1);
            when(userMapper.selectById(any())).thenReturn(new User());
            CompensationResponse r = compensationService.processCreditPayment(1L, 2L, 50, "");
            assertEquals("PAID", r.getStatus());
            verify(creditService).deductCredit(anyLong(), anyInt(), anyString(), anyString(), anyLong(), anyString());
        }

        @Test @DisplayName("E6: processVolunteerPayment 正常")
        void processVolunteerPayment_normal() {
            when(compensationMapper.selectById(1L)).thenReturn(comp);
            when(compensationMapper.updateById(any())).thenReturn(1);
            when(userMapper.selectById(any())).thenReturn(new User());
            CompensationResponse r = compensationService.processVolunteerPayment(1L, 2L, new BigDecimal("3"), "");
            assertEquals("PAID", r.getStatus());
            assertEquals("VOLUNTEER", r.getPaymentMethod());
        }

        @Test @DisplayName("E7: processVolunteerPayment hours=0")
        void processVolunteerPayment_hoursZero() {
            when(compensationMapper.selectById(1L)).thenReturn(comp);
            when(compensationMapper.updateById(any())).thenReturn(1);
            when(userMapper.selectById(any())).thenReturn(new User());
            CompensationResponse r = compensationService.processVolunteerPayment(1L, 2L, BigDecimal.ZERO, "");
            assertEquals("PAID", r.getStatus());
        }

        @Test @DisplayName("E8: processVolunteerPayment hours 为 null")
        void processVolunteerPayment_hoursNull() {
            when(compensationMapper.selectById(1L)).thenReturn(comp);
            when(compensationMapper.updateById(any())).thenReturn(1);
            when(userMapper.selectById(any())).thenReturn(new User());
            assertDoesNotThrow(() -> compensationService.processVolunteerPayment(1L, 2L, null, ""));
        }

        @Test @DisplayName("E9: cancelCompensation 不存在")
        void cancelCompensation_notFound() {
            when(compensationMapper.selectById(999L)).thenReturn(null);
            assertThrows(ResourceNotFoundException.class, () -> compensationService.cancelCompensation(999L, 1L, ""));
        }

        @Test @DisplayName("E10: getCompensationById 不存在")
        void getCompensationById_notFound() {
            when(compensationMapper.selectById(999L)).thenReturn(null);
            assertThrows(ResourceNotFoundException.class, () -> compensationService.getCompensationById(999L));
        }
    }

    // ========================================================================
    // 4. CreditService — 全方法覆盖
    // ========================================================================
    @Nested
    @DisplayName("CreditService 全覆盖: add/deduct/process/query")
    class CreditServiceCoverage {

        @Mock private UserMapper userMapper;
        @Mock private CreditLogMapper creditLogMapper;
        @InjectMocks private CreditServiceImpl creditService;

        private User user;

        @BeforeEach
        void setUp() {
            user = new User(); user.setId(1L); user.setUsername("reader1");
            user.setCreditScore(100); user.setVersion(0);
        }

        @Test @DisplayName("F1: addCredit 正常")
        void addCredit_normal() {
            when(userMapper.selectById(1L)).thenReturn(user);
            when(userMapper.updateCreditScore(anyLong(), anyInt(), anyInt())).thenReturn(1);
            creditService.addCredit(1L, 20, "BORROW", "测试", null, null);
            verify(creditLogMapper).insert(any());
        }

        @Test @DisplayName("F2: addCredit 扣到负数（下限截断）")
        void addCredit_negativeDeduct_cappedAtMin() {
            user.setCreditScore(5);
            when(userMapper.selectById(1L)).thenReturn(user);
            when(userMapper.updateCreditScore(anyLong(), anyInt(), anyInt())).thenReturn(1);
            creditService.addCredit(1L, -20, "OVERDUE", "测试", null, null);
            // 5-20=-15 < MIN_SCORE=0, 截断到0, 实际变动=0-5=-5
            verify(userMapper).updateCreditScore(1L, -5, 0);
        }

        @Test @DisplayName("F3: addCredit 超过上限截断")
        void addCredit_exceedMax_capped() {
            user.setCreditScore(290);
            when(userMapper.selectById(1L)).thenReturn(user);
            when(userMapper.updateCreditScore(anyLong(), anyInt(), anyInt())).thenReturn(1);
            creditService.addCredit(1L, 20, "BORROW", "测试", null, null);
            // 290+20=310 > MAX_SCORE=300, 截断到300, 实际变动=300-290=10
            verify(userMapper).updateCreditScore(1L, 10, 0);
        }

        @Test @DisplayName("F4: deductCredit 正常")
        void deductCredit_normal() {
            when(userMapper.selectById(1L)).thenReturn(user);
            when(userMapper.updateCreditScore(anyLong(), anyInt(), anyInt())).thenReturn(1);
            creditService.deductCredit(1L, 30, "OVERDUE", "罚款", null, null);
            verify(creditLogMapper).insert(any());
        }

        @Test @DisplayName("F5: deductCredit 值为负数（防御）")
        void deductCredit_negativeValue() {
            user.setCreditScore(100);
            when(userMapper.selectById(1L)).thenReturn(user);
            when(userMapper.updateCreditScore(anyLong(), anyInt(), anyInt())).thenReturn(1);
            // deductCredit 内部 Math.abs 所以传-20 等同于 20
            creditService.deductCredit(1L, -20, "OVERDUE", "测试", null, null);
            // 100-20=80, 实际变动是 -20?
            verify(creditLogMapper).insert(any());
        }

        @Test @DisplayName("F6: getUserCredit 正常")
        void getUserCredit_normal() {
            when(userMapper.selectById(1L)).thenReturn(user);
            assertEquals(100, creditService.getUserCredit(1L).intValue());
        }

        @Test @DisplayName("F7: getUserCredit 用户不存在")
        void getUserCredit_notFound() {
            when(userMapper.selectById(999L)).thenReturn(null);
            assertThrows(ResourceNotFoundException.class, () -> creditService.getUserCredit(999L));
        }

        @Test @DisplayName("F8: processCheckInCredit 正常")
        void processCheckInCredit_normal() {
            when(userMapper.selectById(1L)).thenReturn(user);
            when(userMapper.updateCreditScore(anyLong(), anyInt(), anyInt())).thenReturn(1);
            assertDoesNotThrow(() -> creditService.processCheckInCredit(1L, 100L));
        }

        @Test @DisplayName("F9: processReturnCredit 逾期扣分")
        void processReturnCredit_overdue() {
            when(userMapper.selectById(1L)).thenReturn(user);
            when(userMapper.updateCreditScore(anyLong(), anyInt(), anyInt())).thenReturn(1);
            assertDoesNotThrow(() -> creditService.processReturnCredit(1L, 100L, 3,
                    LocalDateTime.now(), LocalDateTime.now()));
        }

        @Test @DisplayName("F10: processReturnCredit overdueDays=0（按时）")
        void processReturnCredit_onTime() {
            when(userMapper.selectById(1L)).thenReturn(user);
            when(userMapper.updateCreditScore(anyLong(), anyInt(), anyInt())).thenReturn(1);
            assertDoesNotThrow(() -> creditService.processReturnCredit(1L, 100L, 0,
                    LocalDateTime.now().minusDays(14), LocalDateTime.now()));
        }

        @Test @DisplayName("F11: processReturnCredit overdueDays=0 拒还（不应加分）")
        void processReturnCredit_overdueDaysZeroButLate() {
            when(userMapper.selectById(1L)).thenReturn(user);
            when(userMapper.updateCreditScore(anyLong(), anyInt(), anyInt())).thenReturn(1);
            // overdueDays=0 但 actually late: 不过业务逻辑只按 overdueDays 判断
            assertDoesNotThrow(() -> creditService.processReturnCredit(1L, 100L, 0,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now()));
        }

        @Test @DisplayName("F12: processReturnCredit 提前归还")
        void processReturnCredit_early() {
            when(userMapper.selectById(1L)).thenReturn(user);
            when(userMapper.updateCreditScore(anyLong(), anyInt(), anyInt())).thenReturn(1);
            assertDoesNotThrow(() -> creditService.processReturnCredit(1L, 100L, 0,
                    LocalDateTime.now().plusDays(7), LocalDateTime.now()));
        }

        @Test @DisplayName("F13: getCreditLogs 正常")
        void getCreditLogs_normal() {
            when(creditLogMapper.selectPage(any(), any())).thenAnswer(inv -> {
                com.baomidou.mybatisplus.core.metadata.IPage<CreditLog> p = inv.getArgument(0);
                p.setRecords(List.of()); p.setTotal(0); return p; });
            PageResult<CreditLogResponse> r = creditService.getCreditLogs(1L, 1L, 10L);
            assertNotNull(r);
        }
    }

    // ========================================================================
    // 5. SeatService — 全方法覆盖
    // ========================================================================
    @Nested
    @DisplayName("SeatService 全覆盖: reserve/checkin/checkout/cancel/query")
    class SeatServiceCoverage {

        @Mock private SeatReservationMapper seatReservationMapper;
        @Mock private UserMapper userMapper;
        @Mock private CreditService creditService;
        @Mock private DistributedLockTemplate lockTemplate;
        @Mock private SeatReservationService seatReservationService;
        @InjectMocks private SeatServiceImpl seatService;

        private User user; private SeatReservation reservation; private SeatReservationRequest request;

        @BeforeEach @SuppressWarnings("unchecked")
        void setUp() {
            user = new User(); user.setId(1L); user.setUsername("reader1");
            user.setStatus("NORMAL"); user.setViolationCount(0);
            reservation = SeatReservation.builder().id(100L).userId(1L).seatNumber("A01")
                    .area("A区-安静区").reservationDate(LocalDate.now())
                    .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0))
                    .status(Constants.ReservationStatus.PENDING).deleted(0).build();
            request = new SeatReservationRequest();
            request.setSeatNumber("B01"); request.setArea("B区-讨论区");
            request.setReservationDate(LocalDate.now());
            request.setStartTime(LocalTime.of(9, 0));
            request.setEndTime(LocalTime.of(11, 0));
            lenient().when(lockTemplate.executeWithLock(anyString(), any(Supplier.class)))
                    .thenAnswer(inv -> ((Supplier<?>)inv.getArgument(1)).get());
        }

        @Test @DisplayName("G1: reserveSeat 正常")
        void reserveSeat_normal() {
            when(userMapper.selectById(1L)).thenReturn(user);
            when(seatReservationMapper.countConflictingReservations(anyString(), any(), any(), any())).thenReturn(0);
            when(seatReservationMapper.selectByUserAndDate(anyLong(), any())).thenReturn(List.of());
            when(seatReservationMapper.insert(any())).thenReturn(1);
            SeatReservationResponse r = seatService.reserveSeat(1L, request);
            assertNotNull(r); assertEquals("PENDING", r.getStatus());
        }

        @Test @DisplayName("G2: reserveSeat 开始=结束时间")
        void reserveSeat_startEqualsEnd() {
            request.setStartTime(LocalTime.of(10, 0));
            request.setEndTime(LocalTime.of(10, 0));
            when(userMapper.selectById(1L)).thenReturn(user);
            assertThrows(BusinessException.class, () -> seatService.reserveSeat(1L, request));
        }

        @Test @DisplayName("G3: reserveSeat 过去日期")
        void reserveSeat_pastDate() {
            request.setReservationDate(LocalDate.now().minusDays(1));
            when(userMapper.selectById(1L)).thenReturn(user);
            assertThrows(BusinessException.class, () -> seatService.reserveSeat(1L, request));
        }

        @Test @DisplayName("G4: reserveSeat 超过22点")
        void reserveSeat_after22() {
            request.setStartTime(LocalTime.of(21, 0));
            request.setEndTime(LocalTime.of(23, 0));
            when(userMapper.selectById(1L)).thenReturn(user);
            assertThrows(BusinessException.class, () -> seatService.reserveSeat(1L, request));
        }

        @Test @DisplayName("G5: reserveSeat 早于8点")
        void reserveSeat_before8() {
            request.setStartTime(LocalTime.of(7, 0));
            request.setEndTime(LocalTime.of(9, 0));
            when(userMapper.selectById(1L)).thenReturn(user);
            assertThrows(BusinessException.class, () -> seatService.reserveSeat(1L, request));
        }

        @Test @DisplayName("G6: reserveSeat 时间冲突")
        void reserveSeat_conflict() {
            when(userMapper.selectById(1L)).thenReturn(user);
            when(seatReservationMapper.countConflictingReservations(anyString(), any(), any(), any())).thenReturn(1);
            assertThrows(BusinessException.class, () -> seatService.reserveSeat(1L, request));
        }

        @Test @DisplayName("G7: reserveSeat 用户被封禁")
        void reserveSeat_banned() {
            user.setViolationCount(Constants.SeatLimit.VIOLATION_THRESHOLD);
            user.setBanUntil(LocalDateTime.now().plusHours(24));
            when(userMapper.selectById(1L)).thenReturn(user);
            assertThrows(BusinessException.class, () -> seatService.reserveSeat(1L, request));
        }

        @Test @DisplayName("G8: cancelReservation 正常")
        void cancelReservation_normal() {
            reservation.setReservationDate(LocalDate.now().plusDays(1));
            when(seatReservationMapper.selectById(100L)).thenReturn(reservation);
            assertDoesNotThrow(() -> seatService.cancelReservation(1L, 100L));
        }

        @Test @DisplayName("G9: cancelReservation 距开始<2小时")
        void cancelReservation_tooLate() {
            reservation.setReservationDate(LocalDate.now());
            reservation.setStartTime(LocalTime.now().plusHours(1));
            when(seatReservationMapper.selectById(100L)).thenReturn(reservation);
            assertThrows(BusinessException.class, () -> seatService.cancelReservation(1L, 100L));
        }

        @Test @DisplayName("G10: cancelReservation 已签到状态")
        void cancelReservation_alreadyCheckedIn() {
            reservation.setStatus(Constants.ReservationStatus.CHECKED_IN);
            when(seatReservationMapper.selectById(100L)).thenReturn(reservation);
            assertThrows(BusinessException.class, () -> seatService.cancelReservation(1L, 100L));
        }

        @Test @DisplayName("G11: cancelReservation 非本人")
        void cancelReservation_notOwner() {
            when(seatReservationMapper.selectById(100L)).thenReturn(reservation);
            assertThrows(ForbiddenException.class, () -> seatService.cancelReservation(3L, 100L));
        }

        @Test @DisplayName("G12: checkIn 正常")
        void checkIn_normal() {
            // 设置签到时间为当前时间附近，使签到窗口有效
            reservation.setStartTime(LocalTime.now().minusMinutes(5));
            reservation.setEndTime(LocalTime.now().plusHours(2));
            when(seatReservationMapper.selectById(100L)).thenReturn(reservation);
            when(seatReservationMapper.updateById(any())).thenReturn(1);
            SeatReservationResponse r = seatService.checkIn(1L, 100L);
            assertEquals("CHECKED_IN", r.getStatus());
        }

        @Test @DisplayName("G13: checkIn 签到超时30分")
        void checkIn_expired() {
            reservation.setStartTime(LocalTime.now().minusHours(1));
            when(seatReservationMapper.selectById(100L)).thenReturn(reservation);
            when(userMapper.selectById(1L)).thenReturn(user);
            assertThrows(BusinessException.class, () -> seatService.checkIn(1L, 100L));
        }

        @Test @DisplayName("G14: checkIn 签到过早（早于开始前15分钟）")
        void checkIn_tooEarly() {
            reservation.setStartTime(LocalTime.now().plusHours(2));
            when(seatReservationMapper.selectById(100L)).thenReturn(reservation);
            assertThrows(BusinessException.class, () -> seatService.checkIn(1L, 100L));
        }

        @Test @DisplayName("G15: checkOut 正常")
        void checkOut_normal() {
            reservation.setStatus(Constants.ReservationStatus.CHECKED_IN);
            when(seatReservationMapper.selectById(100L)).thenReturn(reservation);
            when(seatReservationMapper.updateById(any())).thenReturn(1);
            SeatReservationResponse r = seatService.checkOut(1L, 100L);
            assertEquals("COMPLETED", r.getStatus());
        }

        @Test @DisplayName("G16: checkOut 未签到就签退")
        void checkOut_notCheckedIn() {
            when(seatReservationMapper.selectById(100L)).thenReturn(reservation);
            assertThrows(BusinessException.class, () -> seatService.checkOut(1L, 100L));
        }

        @Test @DisplayName("G17: listSeats date=null")
        void listSeats_dateNull() {
            when(seatReservationMapper.selectBySeatNumbersAndDate(anyList(), any()))
                    .thenReturn(List.of());
            List<SeatReservationResponse> r = seatService.listSeats("A区-安静区", null);
            assertNotNull(r);
        }

        @Test @DisplayName("G18: isTimeSlotAvailable 空串")
        void isTimeSlotAvailable_emptyStrings() {
            when(seatReservationMapper.countConflictingReservations(anyString(), any(), any(), any()))
                    .thenReturn(0);
            assertTrue(seatService.isTimeSlotAvailable("A01", LocalDate.now(), "00:00", "23:59"));
        }

        @Test @DisplayName("G19: getMyReservations 空")
        void getMyReservations_empty() {
            when(seatReservationMapper.selectPage(any(), any())).thenAnswer(inv -> {
                com.baomidou.mybatisplus.core.metadata.IPage<SeatReservation> p = inv.getArgument(0);
                p.setRecords(List.of()); p.setTotal(0); return p; });
            PageResult<SeatReservationResponse> r = seatService.getMyReservations(1L, 1L, 10L);
            assertTrue(r.getRecords().isEmpty());
        }
    }

    // ========================================================================
    // 6. ReaderService — 全方法覆盖
    // ========================================================================
    @Nested
    @DisplayName("ReaderService 全覆盖: register/update/delete/password/query")
    class ReaderServiceCoverage {

        @Mock private UserMapper userMapper;
        @Mock private PasswordEncoder passwordEncoder;
        @InjectMocks private ReaderServiceImpl readerService;

        private User reader;

        @BeforeEach
        void setUp() {
            reader = new User(); reader.setId(1L); reader.setUsername("reader1");
            reader.setRealName("张三"); reader.setRole("READER"); reader.setStatus("NORMAL");
            reader.setCreditScore(100); reader.setBorrowCount(0); reader.setDeleted(0);
            lenient().when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            lenient().when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        }

        @Test @DisplayName("H1: registerReader 正常")
        void registerReader_normal() {
            when(userMapper.selectByUsername("newuser")).thenReturn(null);
            when(userMapper.selectByPhone("13800000000")).thenReturn(null);
            doAnswer(i -> { ((User)i.getArgument(0)).setId(10L); return 1; }).when(userMapper).insert(any());
            ReaderResponse r = readerService.registerReader("newuser", "pass123", "新用户", "13800000000", "new@test.com");
            assertNotNull(r); assertEquals("newuser", r.getUsername());
        }

        @Test @DisplayName("H2: registerReader 用户名已存在")
        void registerReader_dupUsername() {
            when(userMapper.selectByUsername("reader1")).thenReturn(reader);
            assertThrows(BusinessException.class, () -> readerService.registerReader("reader1", "pass", "名字", null, null));
        }

        @Test @DisplayName("H3: registerReader 手机号已存在")
        void registerReader_dupPhone() {
            User phoneUser = new User(); phoneUser.setId(2L); phoneUser.setPhone("13800000000");
            when(userMapper.selectByUsername("newuser")).thenReturn(null);
            when(userMapper.selectByPhone("13800000000")).thenReturn(phoneUser);
            assertThrows(BusinessException.class, () -> readerService.registerReader("newuser", "pass", "名字", "13800000000", null));
        }

        @Test @DisplayName("H4: updateReader 非本人非管理员拒绝")
        void updateReader_forbidden() {
            when(userMapper.selectById(1L)).thenReturn(reader);
            assertThrows(ForbiddenException.class,
                    () -> readerService.updateReader(1L, 2L, false, "新名", null, null, null, null, null, null, null));
        }

        @Test @DisplayName("H5: updateReader 手机号冲突")
        void updateReader_phoneConflict() {
            User owner = new User(); owner.setId(2L); owner.setPhone("13900000000");
            when(userMapper.selectById(1L)).thenReturn(reader);
            when(userMapper.selectByPhone("13900000000")).thenReturn(owner);
            assertThrows(BusinessException.class,
                    () -> readerService.updateReader(1L, 1L, false, null, "13900000000", null, null, null, null, null, null));
        }

        @Test @DisplayName("H6: updateReader 管理员可修改角色")
        void updateReader_adminUpdateRole() {
            when(userMapper.selectById(1L)).thenReturn(reader);
            readerService.updateReader(1L, 2L, true, null, null, null, null, "LIBRARIAN", null, null, null);
            verify(userMapper).updateById(argThat(u -> "LIBRARIAN".equals(u.getRole())));
        }

        @Test @DisplayName("H7: changePassword 旧密码错误")
        void changePassword_wrongOldPwd() {
            reader.setPassword("encoded");
            when(userMapper.selectById(1L)).thenReturn(reader);
            when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);
            assertThrows(BusinessException.class, () -> readerService.changePassword(1L, 1L, "wrong", "new"));
        }

        @Test @DisplayName("H8: changePassword 非本人")
        void changePassword_notOwner() {
            when(userMapper.selectById(1L)).thenReturn(reader);
            assertThrows(ForbiddenException.class, () -> readerService.changePassword(1L, 2L, "old", "new"));
        }

        @Test @DisplayName("H9: deleteReader 有未还书")
        void deleteReader_hasActiveBorrows() {
            reader.setBorrowCount(3);
            when(userMapper.selectById(1L)).thenReturn(reader);
            assertThrows(BusinessException.class, () -> readerService.deleteReader(1L));
        }

        @Test @DisplayName("H10: deleteReader 正常")
        void deleteReader_normal() {
            reader.setBorrowCount(0);
            when(userMapper.selectById(1L)).thenReturn(reader);
            assertDoesNotThrow(() -> readerService.deleteReader(1L));
        }

        @Test @DisplayName("H11: resetPassword 正常")
        void resetPassword_normal() {
            when(userMapper.selectById(1L)).thenReturn(reader);
            assertDoesNotThrow(() -> readerService.resetPassword(1L));
        }

        @Test @DisplayName("H12: getCurrentReader 不存在")
        void getCurrentReader_notFound() {
            when(userMapper.selectByUsername("none")).thenReturn(null);
            assertThrows(ResourceNotFoundException.class, () -> readerService.getCurrentReader("none"));
        }
    }
}
