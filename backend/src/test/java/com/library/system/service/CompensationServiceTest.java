package com.library.system.service;

import com.library.system.base.BaseTest;
import com.library.system.dto.CompensationRequest;
import com.library.system.dto.CompensationResponse;
import com.library.system.dto.PageResult;
import com.library.system.entity.BorrowRecord;
import com.library.system.entity.Compensation;
import com.library.system.entity.User;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.BookMapper;
import com.library.system.mapper.BorrowRecordMapper;
import com.library.system.mapper.CompensationMapper;
import com.library.system.mapper.UserMapper;
import com.library.system.service.impl.CompensationServiceImpl;
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

@DisplayName("CompensationService 单元测试")
class CompensationServiceTest extends BaseTest {

    @Mock
    private CompensationMapper compensationMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CreditService creditService;

    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private CompensationServiceImpl compensationService;

    private Compensation testCompensation;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("reader1");

        testCompensation = new Compensation();
        testCompensation.setId(1L);
        testCompensation.setBorrowId(100L);
        testCompensation.setUserId(1L);
        testCompensation.setAmount(new BigDecimal("50.00"));
        testCompensation.setStatus("PENDING");
        testCompensation.setRemark("图书损坏");
        testCompensation.setDeleted(0);
        testCompensation.setCreateTime(LocalDateTime.now());

        lenient().when(userMapper.selectById(1L)).thenReturn(testUser);
    }

    @Nested
    @DisplayName("赔偿订单管理")
    class CompensationTests {
        @Test
        void createCompensation_success() {
            when(compensationMapper.insert(any(Compensation.class))).thenReturn(1);

            CompensationRequest request = new CompensationRequest();
            request.setBorrowId(100L);
            request.setAmount(new BigDecimal("50.00"));
            request.setRemark("图书损坏");

            BorrowRecord borrowRecord = new BorrowRecord();
            borrowRecord.setId(request.getBorrowId());
            borrowRecord.setDeleted(0);
            when(borrowRecordMapper.selectById(request.getBorrowId())).thenReturn(borrowRecord);

            CompensationResponse result = compensationService.createCompensation(request, 1L);

            assertNotNull(result);
        }

        @Test
        void listCompensations_shouldReturnPage() {
            when(compensationMapper.selectPage(any(), any())).thenAnswer(inv -> {
                com.baomidou.mybatisplus.core.metadata.IPage<Compensation> p = inv.getArgument(0);
                p.setRecords(java.util.Collections.singletonList(testCompensation));
                p.setTotal(1);
                return p;
            });
            PageResult<?> result = compensationService.listCompensations(1L, 10L, null);
            assertEquals(1, result.getTotal());
        }

        @Test
        void getCompensationById_exists() {
            when(compensationMapper.selectById(1L)).thenReturn(testCompensation);
            assertNotNull(compensationService.getCompensationById(1L));
        }

        @Test
        void getCompensationById_notExists() {
            when(compensationMapper.selectById(999L)).thenReturn(null);
            assertThrows(ResourceNotFoundException.class, () -> compensationService.getCompensationById(999L));
        }

        @Test
        void processCashPayment_success() {
            when(compensationMapper.selectById(1L)).thenReturn(testCompensation);
            when(compensationMapper.updateById(any(Compensation.class))).thenReturn(1);

            CompensationResponse result = compensationService.processCashPayment(1L, 2L, "已现金支付");

            assertNotNull(result);
            verify(compensationMapper).updateById(argThat(c -> "PAID".equals(c.getStatus())));
        }

        @Test
        void processCreditPayment_success() {
            when(compensationMapper.selectById(1L)).thenReturn(testCompensation);
            doNothing().when(creditService).deductCredit(anyLong(), anyInt(), any(), any(), anyLong(), any());
            when(compensationMapper.updateById(any(Compensation.class))).thenReturn(1);

            CompensationResponse result = compensationService.processCreditPayment(1L, 2L, 50, "积分抵扣");

            assertNotNull(result);
        }

        @Test
        @DisplayName("现金支付 - 已处理订单再次处理抛异常")
        void processCashPayment_alreadyPaid_shouldThrow() {
            testCompensation.setStatus("PAID");
            when(compensationMapper.selectById(1L)).thenReturn(testCompensation);

            assertThrows(BusinessException.class,
                    () -> compensationService.processCashPayment(1L, 2L, "重复支付"));
        }

        @Test
        @DisplayName("积分抵扣 - 已处理订单抛异常")
        void processCreditPayment_alreadyPaid_shouldThrow() {
            testCompensation.setStatus("PAID");
            when(compensationMapper.selectById(1L)).thenReturn(testCompensation);

            assertThrows(BusinessException.class,
                    () -> compensationService.processCreditPayment(1L, 2L, 50, "重复处理"));
        }

        @Test
        @DisplayName("取消赔偿订单 - PENDING状态可取消")
        void cancelCompensation_pending_shouldSucceed() {
            when(compensationMapper.selectById(1L)).thenReturn(testCompensation);

            compensationService.cancelCompensation(1L, 2L, "自愿取消");

            verify(compensationMapper).updateById(argThat(c -> "CANCELLED".equals(c.getStatus())));
        }

        @Test
        @DisplayName("取消赔偿订单 - 非PENDING状态抛异常")
        void cancelCompensation_notPending_shouldThrow() {
            testCompensation.setStatus("PAID");
            when(compensationMapper.selectById(1L)).thenReturn(testCompensation);

            assertThrows(BusinessException.class,
                    () -> compensationService.cancelCompensation(1L, 2L, "取消"));
        }
    }
}
