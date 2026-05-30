package com.library.system.service;

import com.library.system.base.BaseTest;
import com.library.system.common.Constants;
import com.library.system.entity.User;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.CreditLogMapper;
import com.library.system.mapper.UserMapper;
import com.library.system.service.impl.CreditServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("CreditService 单元测试")
class CreditServiceTest extends BaseTest {

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
        testUser.setUsername("reader1");
        testUser.setCreditScore(100);
        testUser.setVersion(0);
    }

    @Nested
    @DisplayName("积分增减用例")
    class CreditChangeTests {

        @Test
        @DisplayName("增加积分 - 成功")
        void addCredit_success() {
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userMapper.updateCreditScore(anyLong(), anyInt(), anyInt())).thenReturn(1);

            creditService.addCredit(1L, 20, "BORROW", "借书奖励", null, null);

            verify(creditLogMapper).insert(any());
        }

        @Test
        @DisplayName("增加积分 - 超过上限应截断")
        void addCredit_exceedMax_shouldCap() {
            testUser.setCreditScore(290);
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userMapper.updateCreditScore(anyLong(), anyInt(), anyInt())).thenReturn(1);

            creditService.addCredit(1L, 20, "BORROW", "借书奖励", null, null);

            // 290+20=310 > MAX_SCORE=300, 上限截断到300, 实际增加10, 版本号0
            verify(userMapper).updateCreditScore(eq(1L), eq(10), eq(0));
        }

        @Test
        @DisplayName("增加积分 - 用户不存在抛异常")
        void addCredit_userNotFound_shouldThrow() {
            when(userMapper.selectById(999L)).thenReturn(null);

            assertThrows(BusinessException.class,
                    () -> creditService.addCredit(999L, 20, "BORROW", "借书奖励", null, null));
        }

        @Test
        @DisplayName("增加积分 - 乐观锁失败抛异常")
        void addCredit_optimisticLockFailure_shouldThrow() {
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userMapper.updateCreditScore(anyLong(), anyInt(), anyInt())).thenReturn(0);

            assertThrows(BusinessException.class,
                    () -> creditService.addCredit(1L, 20, "BORROW", "借书奖励", null, null));
        }

        @Test
        @DisplayName("扣除积分 - 充足时成功")
        void deductCredit_sufficient() {
            testUser.setCreditScore(80);
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userMapper.updateCreditScore(anyLong(), anyInt(), anyInt())).thenReturn(1);

            creditService.deductCredit(1L, 30, "OVERDUE", "逾期罚款", null, null);

            verify(creditLogMapper).insert(any());
        }

        @Test
        @DisplayName("扣除积分 - 不足时截断到最小值")
        void deductCredit_insufficient_truncateToMin() {
            testUser.setCreditScore(10);
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userMapper.updateCreditScore(anyLong(), anyInt(), anyInt())).thenReturn(1);

            creditService.deductCredit(1L, 50, "OVERDUE", "罚款", null, null);

            // 积分扣到 MIN_SCORE (0)，实际变动为 -10
            verify(userMapper).updateCreditScore(eq(1L), eq(-10), eq(0));
            verify(creditLogMapper).insert(any());
        }

        @Test
        @DisplayName("扣除积分 - 用户不存在抛异常")
        void deductCredit_userNotFound_shouldThrow() {
            when(userMapper.selectById(999L)).thenReturn(null);

            assertThrows(BusinessException.class,
                    () -> creditService.deductCredit(999L, 10, "OVERDUE", "罚款", null, null));
        }

        @Test
        @DisplayName("扣除积分 - 乐观锁失败抛异常")
        void deductCredit_optimisticLockFailure_shouldThrow() {
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userMapper.updateCreditScore(anyLong(), anyInt(), anyInt())).thenReturn(0);

            assertThrows(BusinessException.class,
                    () -> creditService.deductCredit(1L, 10, "OVERDUE", "罚款", null, null));
        }
    }

    @Nested
    @DisplayName("积分奖励流程用例")
    class CreditRewardTests {

        @Test
        @DisplayName("processBorrowCredit - 应正确增加借阅积分")
        void processBorrowCredit_shouldAddBorrowReward() {
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userMapper.updateCreditScore(anyLong(), anyInt(), anyInt())).thenReturn(1);

            creditService.processBorrowCredit(1L, 100L);

            verify(userMapper).updateCreditScore(eq(1L), anyInt(), anyInt());
            verify(creditLogMapper).insert(any());
        }

        @Test
        @DisplayName("processReturnCredit - 按时归还应奖励1分")
        void processReturnCredit_onTime_shouldReward1() {
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userMapper.updateCreditScore(anyLong(), anyInt(), anyInt())).thenReturn(1);

            creditService.processReturnCredit(1L, 100L, 0,
                    LocalDateTime.now().minusDays(14), LocalDateTime.now());

            verify(creditLogMapper).insert(any());
        }

        @Test
        @DisplayName("processCheckInCredit - 签到应奖励1分")
        void processCheckInCredit_shouldReward1() {
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userMapper.updateCreditScore(anyLong(), anyInt(), anyInt())).thenReturn(1);

            creditService.processCheckInCredit(1L, 100L);

            verify(creditLogMapper).insert(any());
        }
    }

    @Nested
    @DisplayName("积分查询用例")
    class CreditQueryTests {

        @Test
        @DisplayName("获取用户积分")
        void getUserCredit() {
            when(userMapper.selectById(1L)).thenReturn(testUser);

            Integer credit = creditService.getUserCredit(1L);

            assertNotNull(credit);
            assertEquals(100, credit.intValue());
        }

        @Test
        @DisplayName("获取用户积分 - 用户不存在抛异常")
        void getUserCredit_userNotFound_shouldThrow() {
            when(userMapper.selectById(999L)).thenReturn(null);

            assertThrows(ResourceNotFoundException.class,
                    () -> creditService.getUserCredit(999L));
        }
    }
}
