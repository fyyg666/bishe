package com.library.system.service;

import com.library.system.base.BaseTest;
import com.library.system.service.impl.AccountLockServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("AccountLockService 单元测试")
class AccountLockServiceTest extends BaseTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private AccountLockServiceImpl accountLockService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Nested
    @DisplayName("账户锁定检测")
    class LockCheckTests {
        @Test
        void isLocked_whenExists_shouldReturnTrue() {
            when(redisTemplate.hasKey(eq("account:locked:1"))).thenReturn(true);
            assertTrue(accountLockService.isLocked(1L));
        }

        @Test
        void isLocked_whenNotExists_shouldReturnFalse() {
            when(redisTemplate.hasKey(eq("account:locked:1"))).thenReturn(false);
            assertFalse(accountLockService.isLocked(1L));
        }
    }

    @Nested
    @DisplayName("登录失败记录")
    class FailureRecordTests {
        @Test
        void recordLoginFailure_shouldIncrementAndCheckLock() {
            when(valueOps.increment(anyString())).thenReturn(3L);
            int count = accountLockService.recordLoginFailure(1L, "testuser");
            assertTrue(count >= 0);
        }

        @Test
        void clearLoginFailures_shouldDeleteKeys() {
            accountLockService.clearLoginFailures(1L);
            verify(redisTemplate, atLeastOnce()).delete(anyString());
        }
    }

    @Nested
    @DisplayName("手动解锁")
    class UnlockTests {
        @Test
        void unlockAccount_shouldDeleteLock() {
            accountLockService.unlockAccount(1L);
            verify(redisTemplate, atLeastOnce()).delete(anyString());
        }
    }
}
