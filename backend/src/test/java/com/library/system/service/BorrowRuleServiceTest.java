package com.library.system.service;

import com.library.system.base.BaseTest;
import com.library.system.dto.BorrowRuleRequest;
import com.library.system.dto.BorrowRuleResponse;
import com.library.system.entity.BorrowRule;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.BorrowRuleMapper;
import com.library.system.service.impl.BorrowRuleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("BorrowRuleService 单元测试")
class BorrowRuleServiceTest extends BaseTest {

    @Mock
    private BorrowRuleMapper borrowRuleMapper;

    @InjectMocks
    private BorrowRuleServiceImpl borrowRuleService;

    private BorrowRule testRule;
    private BorrowRuleRequest ruleRequest;

    @BeforeEach
    void setUp() {
        testRule = new BorrowRule();
        testRule.setId(1L);
        testRule.setReaderType("READER");
        testRule.setBookType("NORMAL");
        testRule.setMaxBorrow(5);
        testRule.setMaxDays(30);
        testRule.setMaxRenew(1);
        testRule.setRenewDays(15);
        testRule.setFinePerDay(new BigDecimal("0.10"));
        testRule.setDeleted(0);

        ruleRequest = new BorrowRuleRequest();
        ruleRequest.setReaderType("READER");
        ruleRequest.setBookType("NORMAL");
        ruleRequest.setMaxBorrow(5);
        ruleRequest.setMaxDays(30);
        ruleRequest.setMaxRenew(1);
        ruleRequest.setRenewDays(15);
        ruleRequest.setFinePerDay(new BigDecimal("0.10"));
    }

    @Nested
    @DisplayName("查询规则用例")
    class QueryTests {

        @Test
        @DisplayName("根据类型查询规则 - 存在")
        void getRuleByType_whenExists_shouldReturnRule() {
            when(borrowRuleMapper.selectOne(any())).thenReturn(testRule);

            BorrowRuleResponse response = borrowRuleService.getRuleByType("READER", "NORMAL");

            assertNotNull(response);
            assertEquals("READER", response.getReaderType());
            assertEquals(5, response.getMaxBorrow());
        }

        @Test
        @DisplayName("根据类型查询规则 - 不存在时回退到默认")
        void getRuleByType_whenNotExists_shouldFallback() {
            when(borrowRuleMapper.selectOne(any())).thenReturn(null);

            BorrowRule rule = borrowRuleService.getRuleEntity("READER", "NORMAL");

            assertNotNull(rule);
            assertEquals(5, rule.getMaxBorrow());
            assertEquals(30, rule.getMaxDays());
        }

        @Test
        @DisplayName("查询规则列表")
        void listRules_shouldReturnList() {
            when(borrowRuleMapper.selectList(any())).thenReturn(Arrays.asList(testRule));

            java.util.List<BorrowRuleResponse> rules = borrowRuleService.listRules();

            assertNotNull(rules);
            assertEquals(1, rules.size());
            assertEquals("READER", rules.get(0).getReaderType());
        }
    }

    @Nested
    @DisplayName("创建规则用例")
    class CreateTests {

        @Test
        @DisplayName("创建规则 - 成功")
        void createRule_success() {
            when(borrowRuleMapper.insert(any(BorrowRule.class))).thenReturn(1);

            BorrowRuleResponse response = borrowRuleService.createRule(ruleRequest);

            assertNotNull(response);
            assertEquals("READER", response.getReaderType());
            assertEquals(5, response.getMaxBorrow());
            verify(borrowRuleMapper).insert(any(BorrowRule.class));
        }
    }

    @Nested
    @DisplayName("更新规则用例")
    class UpdateTests {

        @Test
        @DisplayName("更新规则 - 存在时成功")
        void updateRule_whenExists_shouldSucceed() {
            when(borrowRuleMapper.selectById(1L)).thenReturn(testRule);
            when(borrowRuleMapper.updateById(any(BorrowRule.class))).thenReturn(1);

            ruleRequest.setMaxBorrow(10);
            BorrowRuleResponse response = borrowRuleService.updateRule(1L, ruleRequest);

            assertNotNull(response);
            assertEquals(10, response.getMaxBorrow());
            verify(borrowRuleMapper).updateById(any(BorrowRule.class));
        }

        @Test
        @DisplayName("更新规则 - 不存在时抛异常")
        void updateRule_whenNotExists_shouldThrow() {
            when(borrowRuleMapper.selectById(999L)).thenReturn(null);

            assertThrows(ResourceNotFoundException.class,
                    () -> borrowRuleService.updateRule(999L, ruleRequest));
        }
    }

    @Nested
    @DisplayName("删除规则用例")
    class DeleteTests {

        @Test
        @DisplayName("删除规则 - 存在时成功")
        void deleteRule_whenExists_shouldSucceed() {
            when(borrowRuleMapper.selectById(1L)).thenReturn(testRule);

            borrowRuleService.deleteRule(1L);

            verify(borrowRuleMapper).deleteById(1L);
        }

        @Test
        @DisplayName("删除规则 - 不存在时不报错")
        void deleteRule_whenNotExists_shouldNotThrow() {
            when(borrowRuleMapper.selectById(999L)).thenReturn(null);

            assertDoesNotThrow(() -> borrowRuleService.deleteRule(999L));
            verify(borrowRuleMapper, never()).deleteById(any());
        }
    }
}
