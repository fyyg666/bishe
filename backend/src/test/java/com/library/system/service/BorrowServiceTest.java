package com.library.system.service;

import com.library.system.base.BaseTest;
import com.library.system.common.Constants;
import com.library.system.dto.BorrowRequest;
import com.library.system.dto.BorrowResponse;
import com.library.system.dto.PageResult;
import com.library.system.entity.Book;
import com.library.system.entity.BorrowRecord;
import com.library.system.entity.User;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.BookMapper;
import com.library.system.mapper.BorrowRecordMapper;
import com.library.system.mapper.UserMapper;
import com.library.system.service.impl.BorrowServiceImpl;
import com.library.system.util.HolidayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("BorrowService 单元测试")
class BorrowServiceTest extends BaseTest {

    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @Mock
    private CreditService creditService;

    @Mock
    private HolidayUtil holidayUtil;

    @InjectMocks
    private BorrowServiceImpl borrowService;

    private User testUser;
    private Book testBook;
    private BorrowRecord testRecord;
    private BorrowRequest borrowRequest;

    @BeforeEach
    void setUp() {
        lenient().when(redissonClient.getLock(anyString())).thenReturn(rLock);
        try {
            lenient().when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("reader1");
        testUser.setRole("READER");
        testUser.setStatus("NORMAL");
        testUser.setBorrowCount(0);
        testUser.setCreditScore(100);
        testUser.setVersion(0);
        testUser.setDeleted(0);

        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitle("测试图书");
        testBook.setIsbn("978-7-111-11111-1");
        testBook.setTotalCount(10);
        testBook.setAvailableCount(8);
        testBook.setVersion(0);
        testBook.setDeleted(0);
        testBook.setStatus(0);

        testRecord = new BorrowRecord();
        testRecord.setId(100L);
        testRecord.setUserId(1L);
        testRecord.setBookId(1L);
        testRecord.setStatus(Constants.BorrowStatus.BORROWING);
        testRecord.setBorrowDate(LocalDateTime.now().minusDays(3));
        testRecord.setDueDate(LocalDateTime.now().plusDays(27));
        testRecord.setDeleted(0);
        testRecord.setRenewCount(0);

        borrowRequest = new BorrowRequest();
        borrowRequest.setBookId(1L);
        borrowRequest.setBorrowDays(30);
    }

    @Nested
    @DisplayName("借阅用例")
    class BorrowTests {

        @Test
        @DisplayName("借书成功 - 正常流程")
        void borrowBook_success() {
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(bookMapper.selectById(1L)).thenReturn(testBook);
            when(borrowRecordMapper.selectCount(any())).thenReturn(0L);
            when(bookMapper.updateAvailableCount(1L, -1, 0, 1)).thenReturn(1);
            when(userMapper.updateBorrowCount(anyLong(), anyInt(), anyInt())).thenReturn(1);
            doNothing().when(creditService).processBorrowCredit(anyLong(), any());

            BorrowResponse response = borrowService.borrowBook(1L, borrowRequest);

            assertNotNull(response);
            verify(borrowRecordMapper).insert(any(BorrowRecord.class));
            verify(bookMapper).updateAvailableCount(1L, -1, 0, 1);
        }

        @Test
        @DisplayName("借书失败 - 库存不足")
        void borrowBook_whenNoStock_shouldThrowException() {
            testBook.setAvailableCount(0);
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(bookMapper.selectById(1L)).thenReturn(testBook);

            assertThrows(BusinessException.class, () -> borrowService.borrowBook(1L, borrowRequest));
        }

        @Test
        @DisplayName("借书失败 - 借阅数量超限")
        void borrowBook_whenExceedLimit_shouldThrowException() {
            testUser.setBorrowCount(5);
            testUser.setMaxBorrowCount(5);
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(bookMapper.selectById(1L)).thenReturn(testBook);

            assertThrows(BusinessException.class, () -> borrowService.borrowBook(1L, borrowRequest));
        }

        @Test
        @DisplayName("借书失败 - 有逾期图书")
        void borrowBook_whenHasOverdue_shouldThrowException() {
            testRecord.setDueDate(LocalDateTime.now().minusDays(1));
            when(borrowRecordMapper.selectCount(any())).thenReturn(1L);
            when(userMapper.selectById(1L)).thenReturn(testUser);

            assertThrows(BusinessException.class, () -> borrowService.borrowBook(1L, borrowRequest));
        }

        @Test
        @DisplayName("借书失败 - 用户信用分过低")
        void borrowBook_whenLowCredit_shouldThrowException() {
            testUser.setCreditScore(20);
            when(userMapper.selectById(1L)).thenReturn(testUser);

            assertThrows(BusinessException.class, () -> borrowService.borrowBook(1L, borrowRequest));
        }
    }

    @Nested
    @DisplayName("还书用例")
    class ReturnTests {

        @Test
        @DisplayName("还书成功 - 正常归还")
        void returnBook_success() {
            when(borrowRecordMapper.selectById(100L)).thenReturn(testRecord);
            when(bookMapper.selectById(1L)).thenReturn(testBook);
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userMapper.updateBorrowCount(anyLong(), anyInt(), anyInt())).thenReturn(1);
            when(bookMapper.updateAvailableCount(anyLong(), anyInt(), anyInt(), anyInt())).thenReturn(1);
            doNothing().when(creditService).processReturnCredit(anyLong(), anyLong(), anyInt(), any(), any());

            BorrowResponse response = borrowService.returnBook(1L, 100L);

            assertNotNull(response);
            verify(borrowRecordMapper).updateById(any(BorrowRecord.class));
            verify(bookMapper).updateAvailableCount(1L, 1, 0, 0);
        }

        @Test
        @DisplayName("还书失败 - 借阅记录不存在")
        void returnBook_whenRecordNotExists_shouldThrowException() {
            when(borrowRecordMapper.selectById(999L)).thenReturn(null);

            assertThrows(ResourceNotFoundException.class,
                    () -> borrowService.returnBook(1L, 999L));
        }

        @Test
        @DisplayName("还书失败 - 图书不属于该用户")
        void returnBook_whenNotOwned_shouldThrowException() {
            BorrowRecord otherRecord = new BorrowRecord();
            otherRecord.setId(200L);
            otherRecord.setUserId(2L); // 不同用户
            otherRecord.setBookId(1L);
            otherRecord.setStatus(Constants.BorrowStatus.BORROWING);
            otherRecord.setDeleted(0);
            when(borrowRecordMapper.selectById(200L)).thenReturn(otherRecord);

            assertThrows(BusinessException.class,
                    () -> borrowService.returnBook(1L, 200L));
        }
    }

    @Nested
    @DisplayName("续借用例")
    class RenewTests {

        @Test
        @DisplayName("续借成功")
        void renewBook_success() {
            when(borrowRecordMapper.selectById(100L)).thenReturn(testRecord);

            BorrowResponse response = borrowService.renewBook(1L, 100L, 14);

            assertNotNull(response);
            verify(borrowRecordMapper).updateById(any(BorrowRecord.class));
        }

        @Test
        @DisplayName("续借失败 - 已逾期不可续借")
        void renewBook_whenOverdue_shouldThrowException() {
            BorrowRecord overdueRecord = new BorrowRecord();
            overdueRecord.setId(100L);
            overdueRecord.setUserId(1L);
            overdueRecord.setBookId(1L);
            overdueRecord.setStatus(Constants.BorrowStatus.OVERDUE);
            overdueRecord.setDeleted(0);
            when(borrowRecordMapper.selectById(100L)).thenReturn(overdueRecord);

            assertThrows(BusinessException.class,
                    () -> borrowService.renewBook(1L, 100L, 14));
        }
    }

    @Nested
    @DisplayName("查询用例")
    class QueryTests {

        @Test
        @DisplayName("获取我的借阅列表")
        void getMyBorrows_shouldReturnRecords() {
            when(borrowRecordMapper.selectPage(any(), any())).thenAnswer(invocation -> {
                com.baomidou.mybatisplus.core.metadata.IPage<BorrowRecord> page = invocation.getArgument(0);
                page.setRecords(Arrays.asList(testRecord));
                page.setTotal(1);
                return page;
            });

            PageResult<BorrowResponse> result = borrowService.getMyBorrows(1L, 1L, 10L, null);

            assertNotNull(result);
            assertEquals(1, result.getTotal());
        }

        @Test
        @DisplayName("检查是否有逾期图书")
        void hasOverdueBooks_shouldCheck() {
            when(borrowRecordMapper.selectCount(any())).thenReturn(1L);

            assertTrue(borrowService.hasOverdueBooks(1L));
        }
    }
}
