package com.library.system.service;

import com.library.system.base.BaseTest;
import com.library.system.service.impl.InMemoryTokenBlacklistServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TokenBlacklistService 单元测试")
class TokenBlacklistServiceTest extends BaseTest {

    private InMemoryTokenBlacklistServiceImpl tokenBlacklistService;

    @BeforeEach
    void setUp() {
        tokenBlacklistService = new InMemoryTokenBlacklistServiceImpl();
    }

    @Nested
    @DisplayName("黑名单管理")
    class BlacklistTests {

        @Test
        @DisplayName("Token加入黑名单后应被标记为黑名单")
        void addToBlacklist_shouldMakeTokenBlacklisted() {
            tokenBlacklistService.addToBlacklist("test-token-123", 300L);

            assertTrue(tokenBlacklistService.isBlacklisted("test-token-123"));
        }

        @Test
        @DisplayName("未加入黑名单的Token应返回false")
        void isBlacklisted_notAdded_shouldReturnFalse() {
            assertFalse(tokenBlacklistService.isBlacklisted("unknown-token"));
        }

        @Test
        @DisplayName("不同的Token互不影响")
        void differentTokens_shouldNotAffectEachOther() {
            tokenBlacklistService.addToBlacklist("token-a", 300L);
            tokenBlacklistService.addToBlacklist("token-b", 300L);

            assertTrue(tokenBlacklistService.isBlacklisted("token-a"));
            assertTrue(tokenBlacklistService.isBlacklisted("token-b"));
        }

        @Test
        @DisplayName("过期Token应自动返回false")
        void expiredToken_shouldReturnFalse() throws Exception {
            tokenBlacklistService.addToBlacklist("expired-token", 0L);
            Thread.sleep(10);

            assertFalse(tokenBlacklistService.isBlacklisted("expired-token"));
        }
    }
}
