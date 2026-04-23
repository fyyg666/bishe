package com.library.system.service;

import com.library.system.common.Constants;
import com.library.system.dto.BorrowRequest;
import com.library.system.dto.BorrowResponse;
import com.library.system.dto.PageResult;
import com.library.system.entity.Book;
import com.library.system.entity.BorrowRecord;
import com.library.system.entity.User;
import com.library.system.mapper.BookMapper;
import com.library.system.mapper.BorrowRecordMapper;
import com.library.system.mapper.UserMapper;
import com.library.system.service.impl.BorrowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 借阅服务单元测试
 *
 * @author Library Team
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class BorrowServiceTest {

    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CreditService creditService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @InjectMocks
    private BorrowServiceImpl borrowService;

    private User testUser;
    private Book testBook;
    private BorrowRecord testBorrowRecord;
    private BorrowRequest testBorrowRequest;

    @BeforeEach
    void setUp() throws InterruptedException {
        // 初始化测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encoded_password");
        testUser.setStatus("NORMAL");
        testUser.setCreditScore(100);
        testUser.setBorrowCount(0);
        testUser.setVersion(1);

        // 初始化测试图书
        testBook = new Book();
        testBook.setId(1L);
        testBook.setIsbn("9787111213826");
        testBook.setTitle("Java编程思想");
        testBook.setAuthor("Bruce Eckel");
        testBook.setTotalCount(5);
        testBook.setAvailableCount(3);
        testBook.setStatus(1);
        testBook.setDeleted(0);
        testBook.setVersion(1);

        // 初始化测试借阅记录
        testBorrowRecord = new BorrowRecord();
        testBorrowRecord.setId(1L);
        testBorrowRecord.setUserId(1L);
        testBorrowRecord.setUsername("testuser");
        testBorrowRecord.setBookId(1L);
        testBorrowRecord.setBookTitle("Java编程思想");
        testBorrowRecord.setBookIsbn("9787111213826");
        testBorrowRecord.setBorrowDate(LocalDateTime.now());
        testBorrowRecord.setDueDate(LocalDateTime.now().plusDays(30));
        testBorrowRecord.setStatus("BORROWED");
        testBorrowRecord.setRenewCount(0);
        testBorrowRecord.setOverdueDays(0);
        testBorrowRecord.setFineAmount(BigDecimal.ZERO);
        testBorrowRecord.setDeleted(0);

        // 初始化借阅请求
        testBorrowRequest = new BorrowRequest();
        testBorrowRequest.setBookId(1L);
        testBorrowRequest.setBorrowDays(30);

        // Mock分布式锁
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
    }

    @Test
    void testBorrowBook_Success() throws InterruptedException {
        // 准备测试数据
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(borrowRecordMapper.selectCount(any())).thenReturn(0L);
        when(bookMapper.selectById(1L)).thenReturn(testBook);
        when(bookMapper.updateAvailableCount(anyLong(), anyInt(), anyInt(), anyInt())).thenReturn(1);
        when(userMapper.updateBorrowCount(anyLong(), anyInt(), anyInt())).thenReturn(1);
        when(borrowRecordMapper.insert(any(BorrowRecord.class))).thenReturn(1);

        // 执行测试
        BorrowResponse result = borrowService.borrowBook(1L, testBorrowRequest);

        // 验证结果
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("Java编程思想", result.getBookTitle());
        assertEquals("BORROWED", result.getStatus());

        verify(creditService).processBorrowCredit(eq(1L), anyLong());
        verify(rLock).unlock();
    }

    @Test
    void testBorrowBook_UserNotFound() throws InterruptedException {
        when(userMapper.selectById(1L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> borrowService.borrowBook(1L, testBorrowRequest));
    }

    @Test
    void testBorrowBook_UserDisabled() throws InterruptedException {
        testUser.setStatus("DISABLED");
        when(userMapper.selectById(1L)).thenReturn(testUser);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> borrowService.borrowBook(1L, testBorrowRequest));
        assertEquals("用户不存在或已被禁用", exception.getMessage());
    }

    @Test
    void testBorrowBook_BookNotAvailable() throws InterruptedException {
        testBook.setAvailableCount(0);
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(borrowRecordMapper.selectCount(any())).thenReturn(0L);
        when(bookMapper.selectById(1L)).thenReturn(testBook);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> borrowService.borrowBook(1L, testBorrowRequest));
        assertEquals("图书库存不足", exception.getMessage());
    }

    @Test
    void testBorrowBook_MaxBorrowLimit() throws InterruptedException {
        testUser.setBorrowCount(5);
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(borrowRecordMapper.selectCount(any())).thenReturn(0L);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> borrowService.borrowBook(1L, testBorrowRequest));
        assertEquals("已达到最大借阅数量限制", exception.getMessage());
    }

    @Test
    void testBorrowBook_HasOverdueBooks() throws InterruptedException {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(borrowRecordMapper.selectCount(any())).thenReturn(1L);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> borrowService.borrowBook(1L, testBorrowRequest));
        assertEquals("您有逾期未还的图书，请先归还", exception.getMessage());
    }

    @Test
    void testBorrowBook_BorrowDaysExceedLimit() throws InterruptedException {
        testBorrowRequest.setBorrowDays(100);
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(borrowRecordMapper.selectCount(any())).thenReturn(0L);
        when(bookMapper.selectById(1L)).thenReturn(testBook);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> borrowService.borrowBook(1L, testBorrowRequest));
        assertEquals("借阅天数必须在1-60天之间", exception.getMessage());
    }

    @Test
    void testReturnBook_Success() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(borrowRecordMapper.selectById(1L)).thenReturn(testBorrowRecord);
        when(bookMapper.selectById(1L)).thenReturn(testBook);
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(bookMapper.updateAvailableCount(anyLong(), anyInt(), anyInt(), anyInt())).thenReturn(1);
        when(userMapper.updateBorrowCount(anyLong(), anyInt(), anyInt())).thenReturn(1);

        BorrowResponse result = borrowService.returnBook(1L, 1L);

        assertNotNull(result);
        assertEquals("RETURNED", result.getStatus());

        verify(creditService).processReturnCredit(eq(1L), eq(1L), anyInt());
    }

    @Test
    void testReturnBook_OverdueFine() throws InterruptedException {
        testBorrowRecord.setDueDate(LocalDateTime.now().minusDays(5));
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(borrowRecordMapper.selectById(1L)).thenReturn(testBorrowRecord);
        when(bookMapper.selectById(1L)).thenReturn(testBook);
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(bookMapper.updateAvailableCount(anyLong(), anyInt(), anyInt(), anyInt())).thenReturn(1);
        when(userMapper.updateBorrowCount(anyLong(), anyInt(), anyInt())).thenReturn(1);

        BorrowResponse result = borrowService.returnBook(1L, 1L);

        assertNotNull(result);
        assertEquals("RETURNED", result.getStatus());
        assertTrue(result.getOverdueDays() > 0);
        assertTrue(result.getFineAmount().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testReturnBook_RecordNotFound() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(borrowRecordMapper.selectById(999L)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> borrowService.returnBook(1L, 999L));
        assertEquals("借阅记录不存在", exception.getMessage());
    }

    @Test
    void testReturnBook_WrongUser() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(borrowRecordMapper.selectById(1L)).thenReturn(testBorrowRecord);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> borrowService.returnBook(999L, 1L));
        assertEquals("无权操作此借阅记录", exception.getMessage());
    }

    @Test
    void testReturnBook_AlreadyReturned() throws InterruptedException {
        testBorrowRecord.setStatus("RETURNED");
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(borrowRecordMapper.selectById(1L)).thenReturn(testBorrowRecord);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> borrowService.returnBook(1L, 1L));
        assertEquals("该图书已归还或状态异常", exception.getMessage());
    }

    @Test
    void testRenewBook_Success() {
        when(borrowRecordMapper.selectById(1L)).thenReturn(testBorrowRecord);

        BorrowResponse result = borrowService.renewBook(1L, 1L, 15);

        assertNotNull(result);
        assertEquals(1, result.getRenewCount());
        verify(borrowRecordMapper).updateById(any(BorrowRecord.class));
    }

    @Test
    void testRenewBook_MaxRenewCount() {
        testBorrowRecord.setRenewCount(2);
        when(borrowRecordMapper.selectById(1L)).thenReturn(testBorrowRecord);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> borrowService.renewBook(1L, 1L, 15));
        assertEquals("已达到最大续借次数限制", exception.getMessage());
    }

    @Test
    void testRenewBook_Overdue() {
        testBorrowRecord.setDueDate(LocalDateTime.now().minusDays(1));
        when(borrowRecordMapper.selectById(1L)).thenReturn(testBorrowRecord);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> borrowService.renewBook(1L, 1L, 15));
        assertEquals("图书已逾期，无法续借", exception.getMessage());
    }

    @Test
    void testGetMyBorrows_Success() {
        List<BorrowRecord> records = Arrays.asList(testBorrowRecord);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<BorrowRecord> page = 
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setRecords(records);
        page.setTotal(1);

        when(borrowRecordMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<BorrowResponse> result = borrowService.getMyBorrows(1L, 1L, 10L, null);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
    }

    @Test
    void testGetMyBorrows_WithStatusFilter() {
        List<BorrowRecord> records = Arrays.asList(testBorrowRecord);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<BorrowRecord> page = 
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setRecords(records);
        page.setTotal(1);

        when(borrowRecordMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<BorrowResponse> result = borrowService.getMyBorrows(1L, 1L, 10L, "BORROWED");

        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }

    @Test
    void testGetBorrowById_Success() {
        when(borrowRecordMapper.selectById(1L)).thenReturn(testBorrowRecord);

        BorrowResponse result = borrowService.getBorrowById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void testGetBorrowById_NotFound() {
        when(borrowRecordMapper.selectById(999L)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> borrowService.getBorrowById(999L));
        assertEquals("借阅记录不存在", exception.getMessage());
    }

    @Test
    void testHasOverdueBooks_True() {
        when(borrowRecordMapper.selectCount(any())).thenReturn(1L);

        boolean result = borrowService.hasOverdueBooks(1L);

        assertTrue(result);
    }

    @Test
    void testHasOverdueBooks_False() {
        when(borrowRecordMapper.selectCount(any())).thenReturn(0L);

        boolean result = borrowService.hasOverdueBooks(1L);

        assertFalse(result);
    }
}
