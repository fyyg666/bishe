package com.library.system.service;

import com.library.system.entity.AccountLock;
import com.library.system.mapper.AccountLockMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 账户锁定服务单元测试
 *
 * @author Library Team
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class AccountLockServiceTest {

    @Mock
    private AccountLockMapper accountLockMapper;

    @InjectMocks
    private AccountLockServiceImpl accountLockService;

    private AccountLock testAccountLock;

    @BeforeEach
    void setUp() {
        testAccountLock = new AccountLock();
        testAccountLock.setId(1L);
        testAccountLock.setUsername("testuser");
        testAccountLock.setLockTime(LocalDateTime.now());
        testAccountLock.setLockDuration(900);  // 15分钟
        testAccountLock.setUnlockTime(LocalDateTime.now().plusSeconds(900));
        testAccountLock.setLockReason("密码错误次数过多");
        testAccountLock.setStatus(1);  // 1=锁定中
    }

    @Test
    void testLockAccount_Success() {
        when(accountLockMapper.insert(any(AccountLock.class))).thenReturn(1);

        AccountLock result = accountLockService.lockAccount("testuser", "密码错误次数过多", 900);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("密码错误次数过多", result.getLockReason());
        assertEquals(1, result.getStatus());
        verify(accountLockMapper).insert(any(AccountLock.class));
    }

    @Test
    void testLockAccount_AlreadyLocked() {
        when(accountLockMapper.selectByUsernameAndStatus("testuser", 1))
                .thenReturn(testAccountLock);

        // Should throw exception or return existing lock
        assertThrows(RuntimeException.class, () -> 
                accountLockService.lockAccount("testuser", "密码错误", 900));
        
        verify(accountLockMapper, never()).insert(any(AccountLock.class));
    }

    @Test
    void testUnlockAccount_Success() {
        testAccountLock.setStatus(1);
        when(accountLockMapper.selectByUsernameAndStatus("testuser", 1))
                .thenReturn(testAccountLock);
        when(accountLockMapper.updateById(any(AccountLock.class))).thenReturn(1);

        boolean result = accountLockService.unlockAccount("testuser");

        assertTrue(result);
        verify(accountLockMapper).updateById(any(AccountLock.class));
    }

    @Test
    void testUnlockAccount_NotLocked() {
        when(accountLockMapper.selectByUsernameAndStatus("testuser", 1))
                .thenReturn(null);

        boolean result = accountLockService.unlockAccount("testuser");

        assertFalse(result);
        verify(accountLockMapper, never()).updateById(any(AccountLock.class));
    }

    @Test
    void testIsAccountLocked_Locked() {
        when(accountLockMapper.selectByUsernameAndStatus("testuser", 1))
                .thenReturn(testAccountLock);

        boolean result = accountLockService.isAccountLocked("testuser");

        assertTrue(result);
        verify(accountLockMapper).selectByUsernameAndStatus("testuser", 1);
    }

    @Test
    void testIsAccountLocked_NotLocked() {
        when(accountLockMapper.selectByUsernameAndStatus("testuser", 1))
                .thenReturn(null);

        boolean result = accountLockService.isAccountLocked("testuser");

        assertFalse(result);
        verify(accountLockMapper).selectByUsernameAndStatus("testuser", 1);
    }

    @Test
    void testIsAccountLocked_ExpiredLock() {
        // Lock already expired
        testAccountLock.setUnlockTime(LocalDateTime.now().minusMinutes(1));
        testAccountLock.setStatus(1);
        when(accountLockMapper.selectByUsernameAndStatus("testuser", 1))
                .thenReturn(testAccountLock);
        when(accountLockMapper.updateById(any(AccountLock.class))).thenReturn(1);

        boolean result = accountLockService.isAccountLocked("testuser");

        // Should auto-unlock if expired
        assertFalse(result);
        verify(accountLockMapper).updateById(any(AccountLock.class));
    }

    @Test
    void testGetLockInfo_Success() {
        when(accountLockMapper.selectByUsernameAndStatus("testuser", 1))
                .thenReturn(testAccountLock);

        AccountLock result = accountLockService.getLockInfo("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("密码错误次数过多", result.getLockReason());
        verify(accountLockMapper).selectByUsernameAndStatus("testuser", 1);
    }

    @Test
    void testGetLockInfo_NotLocked() {
        when(accountLockMapper.selectByUsernameAndStatus("testuser", 1))
                .thenReturn(null);

        AccountLock result = accountLockService.getLockInfo("testuser");

        assertNull(result);
        verify(accountLockMapper).selectByUsernameAndStatus("testuser", 1);
    }

    @Test
    void testGetAllLockedAccounts_Success() {
        List<AccountLock> lockedAccounts = Arrays.asList(testAccountLock);
        when(accountLockMapper.selectByStatus(1)).thenReturn(lockedAccounts);

        List<AccountLock> result = accountLockService.getAllLockedAccounts();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).getUsername());
        verify(accountLockMapper).selectByStatus(1);
    }

    @Test
    void testUpdateLockDuration_Success() {
        testAccountLock.setStatus(1);
        when(accountLockMapper.selectByUsernameAndStatus("testuser", 1))
                .thenReturn(testAccountLock);
        when(accountLockMapper.updateById(any(AccountLock.class))).thenReturn(1);

        boolean result = accountLockService.updateLockDuration("testuser", 1800);  // 30分钟

        assertTrue(result);
        verify(accountLockMapper).updateById(any(AccountLock.class));
    }

    @Test
    void testIncrementFailedAttempts_Success() {
        when(accountLockMapper.selectByUsernameAndStatus("testuser", 1))
                .thenReturn(null);  // Not locked yet
        when(accountLockMapper.insert(any(AccountLock.class))).thenReturn(1);

        // This method should track failed attempts
        accountLockService.incrementFailedAttempts("testuser");

        verify(accountLockMapper).insert(any(AccountLock.class));
    }

    @Test
    void testResetFailedAttempts_Success() {
        when(accountLockMapper.selectByUsernameAndStatus("testuser", 1))
                .thenReturn(testAccountLock);

        accountLockService.resetFailedAttempts("testuser");

        verify(accountLockMapper).deleteById(testAccountLock.getId());
    }
}
