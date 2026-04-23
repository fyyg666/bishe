package com.library.system.service;

import com.library.system.entity.User;
import com.library.system.mapper.CreditLogMapper;
import com.library.system.mapper.UserMapper;
import com.library.system.service.impl.CreditServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 信用积分服务单元测试
 *
 * @author Library Team
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class CreditServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private CreditLogMapper creditLogMapper;

    @InjectMocks
    private CreditServiceImpl creditService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setCreditScore(100);
        testUser.setVersion(1);
    }

    @Test
    void testGetUserCredit_Success() {
        when(userMapper.selectById(1L)).thenReturn(testUser);

        Integer result = creditService.getUserCredit(1L);

        assertEquals(100, result);
    }

    @Test
    void testGetUserCredit_UserNotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> creditService.getUserCredit(999L));
    }

    @Test
    void testAddCredit_Success() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.updateCreditScore(anyLong(), anyInt(), anyInt())).thenReturn(1);
        when(creditLogMapper.insert(any())).thenReturn(1);

        creditService.addCredit(1L, 10, "TEST", "测试加分", 1L, "TEST_TYPE");

        verify(userMapper).updateCreditScore(anyLong(), anyInt(), anyInt());
        verify(creditLogMapper).insert(any());
    }

    @Test
    void testDeductCredit_Success() {
        testUser.setCreditScore(100);
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.updateCreditScore(anyLong(), anyInt(), anyInt())).thenReturn(1);
        when(creditLogMapper.insert(any())).thenReturn(1);

        creditService.deductCredit(1L, 10, "OVERDUE", "测试扣分", 1L, "OVERDUE_TYPE");

        verify(userMapper).updateCreditScore(anyLong(), anyInt(), anyInt());
        verify(creditLogMapper).insert(any());
    }

    @Test
    void testProcessBorrowCredit() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.updateCreditScore(anyLong(), anyInt(), anyInt())).thenReturn(1);
        when(creditLogMapper.insert(any())).thenReturn(1);

        creditService.processBorrowCredit(1L, 1L);

        verify(userMapper).updateCreditScore(anyLong(), anyInt(), anyInt());
        verify(creditLogMapper).insert(any());
    }

    @Test
    void testProcessReturnCredit_OnTime() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.updateCreditScore(anyLong(), anyInt(), anyInt())).thenReturn(1);
        when(creditLogMapper.insert(any())).thenReturn(1);

        creditService.processReturnCredit(1L, 1L, 0);

        verify(userMapper).updateCreditScore(anyLong(), anyInt(), anyInt());
        verify(creditLogMapper).insert(any());
    }

    @Test
    void testProcessReturnCredit_Overdue() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.updateCreditScore(anyLong(), anyInt(), anyInt())).thenReturn(1);
        when(creditLogMapper.insert(any())).thenReturn(1);

        creditService.processReturnCredit(1L, 1L, 2);

        verify(userMapper).updateCreditScore(anyLong(), anyInt(), anyInt());
        verify(creditLogMapper).insert(any());
    }

    @Test
    void testProcessCheckInCredit() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.updateCreditScore(anyLong(), anyInt(), anyInt())).thenReturn(1);
        when(creditLogMapper.insert(any())).thenReturn(1);

        creditService.processCheckInCredit(1L, 1L);

        verify(userMapper).updateCreditScore(anyLong(), anyInt(), anyInt());
        verify(creditLogMapper).insert(any());
    }
}

