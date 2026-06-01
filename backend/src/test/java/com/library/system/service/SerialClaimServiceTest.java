package com.library.system.service;

import com.library.system.base.BaseTest;
import com.library.system.dto.SerialClaimRequest;
import com.library.system.dto.SerialClaimResponse;
import com.library.system.entity.SerialClaim;
import com.library.system.entity.SerialIssue;
import com.library.system.entity.SerialSubscription;
import com.library.system.entity.Vendor;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.SerialClaimMapper;
import com.library.system.mapper.SerialIssueMapper;
import com.library.system.mapper.SerialSubscriptionMapper;
import com.library.system.mapper.VendorMapper;
import com.library.system.service.impl.SerialClaimServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("SerialClaimService 单元测试")
class SerialClaimServiceTest extends BaseTest {

    @Mock
    private SerialClaimMapper claimMapper;

    @Mock
    private SerialSubscriptionMapper subscriptionMapper;

    @Mock
    private SerialIssueMapper issueMapper;

    @Mock
    private VendorMapper vendorMapper;

    @InjectMocks
    private SerialClaimServiceImpl serialClaimService;

    private SerialClaim testClaim;
    private SerialClaimRequest claimRequest;
    private SerialSubscription testSubscription;
    private SerialIssue testIssue;
    private Vendor testVendor;

    @BeforeEach
    void setUp() {
        testSubscription = new SerialSubscription();
        testSubscription.setId(1L);
        testSubscription.setTitle("测试期刊");
        testSubscription.setVendorId(10L);
        testSubscription.setDeleted(0);

        testIssue = new SerialIssue();
        testIssue.setId(1L);
        testIssue.setSubscriptionId(1L);
        testIssue.setStatus("MISSING");
        testIssue.setExpectedDate(LocalDate.now().minusDays(40));

        testVendor = new Vendor();
        testVendor.setId(10L);
        testVendor.setName("测试供应商");

        testClaim = new SerialClaim();
        testClaim.setId(1L);
        testClaim.setSubscriptionId(1L);
        testClaim.setIssueId(1L);
        testClaim.setClaimNumber("CL-20260601-001");
        testClaim.setVendorId(10L);
        testClaim.setClaimType("MISSING");
        testClaim.setClaimStatus("PENDING");
        testClaim.setClaimDate(LocalDate.now());
        testClaim.setDescription("缺刊催缺");
        testClaim.setOperatorId(1L);
        testClaim.setDeleted(0);

        claimRequest = new SerialClaimRequest();
        claimRequest.setSubscriptionId(1L);
        claimRequest.setIssueId(1L);
        claimRequest.setVendorId(10L);
        claimRequest.setClaimType("MISSING");
        claimRequest.setClaimDate(LocalDate.now());
        claimRequest.setDescription("缺刊催缺");
    }

    @Nested
    @DisplayName("创建催缺用例")
    class CreateTests {

        @Test
        @DisplayName("创建催缺 - 成功")
        void createClaim_success() {
            when(subscriptionMapper.selectById(1L)).thenReturn(testSubscription);
            when(claimMapper.selectOne(any())).thenReturn(null);
            when(claimMapper.insert(any(SerialClaim.class))).thenReturn(1);

            SerialClaimResponse response = serialClaimService.createClaim(claimRequest, 1L);

            assertNotNull(response);
            assertEquals("PENDING", response.getClaimStatus());
            verify(claimMapper).insert(any(SerialClaim.class));
        }

        @Test
        @DisplayName("创建催缺 - 订阅不存在抛异常")
        void createClaim_subscriptionNotFound_shouldThrow() {
            when(subscriptionMapper.selectById(999L)).thenReturn(null);
            claimRequest.setSubscriptionId(999L);

            assertThrows(ResourceNotFoundException.class,
                    () -> serialClaimService.createClaim(claimRequest, 1L));
        }
    }

    @Nested
    @DisplayName("处理催缺用例")
    class ResolveTests {

        @Test
        @DisplayName("处理催缺 - 成功")
        void resolveClaim_success() {
            when(claimMapper.selectById(1L)).thenReturn(testClaim);
            when(claimMapper.updateById(any(SerialClaim.class))).thenReturn(1);

            serialClaimService.resolveClaim(1L, "已补发");

            verify(claimMapper).updateById(any(SerialClaim.class));
        }

        @Test
        @DisplayName("处理催缺 - 不存在抛异常")
        void resolveClaim_notExists_shouldThrow() {
            when(claimMapper.selectById(999L)).thenReturn(null);

            assertThrows(ResourceNotFoundException.class,
                    () -> serialClaimService.resolveClaim(999L, "已补发"));
        }
    }

    @Nested
    @DisplayName("关闭催缺用例")
    class CloseTests {

        @Test
        @DisplayName("关闭催缺 - 成功")
        void closeClaim_success() {
            when(claimMapper.selectById(1L)).thenReturn(testClaim);
            when(claimMapper.updateById(any(SerialClaim.class))).thenReturn(1);

            serialClaimService.closeClaim(1L);

            verify(claimMapper).updateById(argThat(claim ->
                    "CLOSED".equals(claim.getClaimStatus())));
        }

        @Test
        @DisplayName("关闭催缺 - 不存在抛异常")
        void closeClaim_notExists_shouldThrow() {
            when(claimMapper.selectById(999L)).thenReturn(null);

            assertThrows(ResourceNotFoundException.class,
                    () -> serialClaimService.closeClaim(999L));
        }
    }

    @Nested
    @DisplayName("自动催缺用例")
    class AutoClaimTests {

        @Test
        @DisplayName("自动催缺 - 发现缺刊并创建催缺记录")
        void checkAndAutoClaim_findsMissingIssues() {
            when(issueMapper.selectList(any())).thenReturn(Arrays.asList(testIssue));
            when(claimMapper.selectCount(any())).thenReturn(0L);
            when(subscriptionMapper.selectById(1L)).thenReturn(testSubscription);
            when(claimMapper.selectOne(any())).thenReturn(null);
            when(claimMapper.insert(any(SerialClaim.class))).thenReturn(1);

            int count = serialClaimService.checkAndAutoClaim();

            assertEquals(1, count);
            verify(claimMapper).insert(any(SerialClaim.class));
        }

        @Test
        @DisplayName("自动催缺 - 无缺刊返回0")
        void checkAndAutoClaim_noMissingIssues() {
            when(issueMapper.selectList(any())).thenReturn(Collections.emptyList());

            int count = serialClaimService.checkAndAutoClaim();

            assertEquals(0, count);
            verify(claimMapper, never()).insert(any());
        }

        @Test
        @DisplayName("自动催缺 - 已有催缺记录不重复创建")
        void checkAndAutoClaim_existingClaim_shouldNotDuplicate() {
            when(issueMapper.selectList(any())).thenReturn(Arrays.asList(testIssue));
            when(claimMapper.selectCount(any())).thenReturn(1L);

            int count = serialClaimService.checkAndAutoClaim();

            assertEquals(0, count);
            verify(claimMapper, never()).insert(any());
        }
    }

    @Nested
    @DisplayName("查询催缺用例")
    class QueryTests {

        @Test
        @DisplayName("查询催缺详情 - 存在")
        void getClaim_whenExists_shouldReturn() {
            when(claimMapper.selectById(1L)).thenReturn(testClaim);
            when(subscriptionMapper.selectById(1L)).thenReturn(testSubscription);
            when(vendorMapper.selectById(10L)).thenReturn(testVendor);

            SerialClaimResponse response = serialClaimService.getClaim(1L);

            assertNotNull(response);
            assertEquals(1L, response.getId());
        }

        @Test
        @DisplayName("查询催缺详情 - 不存在抛异常")
        void getClaim_whenNotExists_shouldThrow() {
            when(claimMapper.selectById(999L)).thenReturn(null);

            assertThrows(ResourceNotFoundException.class,
                    () -> serialClaimService.getClaim(999L));
        }
    }
}
