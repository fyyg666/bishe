package com.library.system.service;

import com.library.system.base.BaseTest;
import com.library.system.dto.BranchRequest;
import com.library.system.dto.BranchResponse;
import com.library.system.entity.Branch;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.BookMapper;
import com.library.system.mapper.BranchMapper;
import com.library.system.mapper.ReadingRoomMapper;
import com.library.system.mapper.SeatMapper;
import com.library.system.service.impl.BranchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("BranchService 单元测试")
class BranchServiceTest extends BaseTest {

    @Mock
    private BranchMapper branchMapper;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private SeatMapper seatMapper;

    @Mock
    private ReadingRoomMapper readingRoomMapper;

    @InjectMocks
    private BranchServiceImpl branchService;

    private Branch testBranch;
    private Branch childBranch;
    private BranchRequest branchRequest;

    @BeforeEach
    void setUp() {
        testBranch = new Branch();
        testBranch.setId(1L);
        testBranch.setName("总馆");
        testBranch.setCode("MAIN");
        testBranch.setAddress("测试地址");
        testBranch.setPhone("12345678");
        testBranch.setEmail("main@test.com");
        testBranch.setOpeningHours("08:00-22:00");
        testBranch.setStatus(1);
        testBranch.setParentId(null);
        testBranch.setDeleted(0);

        childBranch = new Branch();
        childBranch.setId(2L);
        childBranch.setName("分馆A");
        childBranch.setCode("BRANCH_A");
        childBranch.setStatus(1);
        childBranch.setParentId(1L);
        childBranch.setDeleted(0);

        branchRequest = new BranchRequest();
        branchRequest.setName("新分馆");
        branchRequest.setCode("NEW_BRANCH");
        branchRequest.setAddress("新地址");
        branchRequest.setPhone("87654321");
        branchRequest.setEmail("new@test.com");
        branchRequest.setOpeningHours("09:00-21:00");
        branchRequest.setStatus(1);
    }

    @Nested
    @DisplayName("创建分馆用例")
    class CreateTests {

        @Test
        @DisplayName("创建分馆 - 成功")
        void createBranch_success() {
            when(branchMapper.selectCount(any())).thenReturn(0L);
            when(branchMapper.insert(any(Branch.class))).thenAnswer(invocation -> {
                Branch branch = invocation.getArgument(0);
                branch.setId(1L);
                return 1;
            });
            when(branchMapper.selectById(1L)).thenReturn(testBranch);

            BranchResponse response = branchService.createBranch(branchRequest);

            assertNotNull(response);
            verify(branchMapper).insert(any(Branch.class));
        }

        @Test
        @DisplayName("创建分馆 - 编码重复抛异常")
        void createBranch_duplicateCode_shouldThrow() {
            when(branchMapper.selectCount(any())).thenReturn(1L);

            assertThrows(BusinessException.class,
                    () -> branchService.createBranch(branchRequest));
            verify(branchMapper, never()).insert(any());
        }
    }

    @Nested
    @DisplayName("删除分馆用例")
    class DeleteTests {

        @Test
        @DisplayName("删除分馆 - 存在图书数据抛异常")
        void deleteBranch_hasBooks_shouldThrow() {
            when(branchMapper.selectById(1L)).thenReturn(testBranch);
            when(branchMapper.selectCount(argThat(w -> w != null))).thenReturn(0L);
            when(bookMapper.selectCount(any())).thenReturn(5L);

            assertThrows(BusinessException.class,
                    () -> branchService.deleteBranch(1L));
        }

        @Test
        @DisplayName("删除分馆 - 存在子分馆抛异常")
        void deleteBranch_hasChildren_shouldThrow() {
            when(branchMapper.selectById(1L)).thenReturn(testBranch);
            when(branchMapper.selectCount(argThat(w -> w != null))).thenReturn(2L);

            assertThrows(BusinessException.class,
                    () -> branchService.deleteBranch(1L));
        }

        @Test
        @DisplayName("删除分馆 - 不存在抛异常")
        void deleteBranch_notExists_shouldThrow() {
            when(branchMapper.selectById(999L)).thenReturn(null);

            assertThrows(ResourceNotFoundException.class,
                    () -> branchService.deleteBranch(999L));
        }

        @Test
        @DisplayName("删除分馆 - 无关联数据时成功")
        void deleteBranch_noAssociations_shouldSucceed() {
            when(branchMapper.selectById(1L)).thenReturn(testBranch);
            when(branchMapper.selectCount(argThat(w -> w != null))).thenReturn(0L);
            when(bookMapper.selectCount(any())).thenReturn(0L);
            when(seatMapper.selectCount(any())).thenReturn(0L);
            when(readingRoomMapper.selectCount(any())).thenReturn(0L);

            branchService.deleteBranch(1L);

            verify(branchMapper).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("更新分馆用例")
    class UpdateTests {

        @Test
        @DisplayName("更新分馆 - 存在时成功")
        void updateBranch_whenExists_shouldSucceed() {
            when(branchMapper.selectById(1L)).thenReturn(testBranch);
            when(branchMapper.updateById(any(Branch.class))).thenReturn(1);

            branchService.updateBranch(1L, branchRequest);

            verify(branchMapper).updateById(any(Branch.class));
        }

        @Test
        @DisplayName("更新分馆 - 不存在时抛异常")
        void updateBranch_whenNotExists_shouldThrow() {
            when(branchMapper.selectById(999L)).thenReturn(null);

            assertThrows(ResourceNotFoundException.class,
                    () -> branchService.updateBranch(999L, branchRequest));
        }

        @Test
        @DisplayName("更新分馆 - 编码冲突抛异常")
        void updateBranch_codeConflict_shouldThrow() {
            when(branchMapper.selectById(1L)).thenReturn(testBranch);
            when(branchMapper.selectCount(any())).thenReturn(1L);

            branchRequest.setCode("CONFLICT_CODE");

            assertThrows(BusinessException.class,
                    () -> branchService.updateBranch(1L, branchRequest));
        }
    }

    @Nested
    @DisplayName("查询分馆用例")
    class QueryTests {

        @Test
        @DisplayName("查询分馆详情 - 存在")
        void getBranch_whenExists_shouldReturn() {
            when(branchMapper.selectById(1L)).thenReturn(testBranch);
            when(branchMapper.selectCount(any())).thenReturn(0L);

            BranchResponse response = branchService.getBranch(1L);

            assertNotNull(response);
            assertEquals("总馆", response.getName());
            assertEquals("MAIN", response.getCode());
        }

        @Test
        @DisplayName("查询分馆详情 - 不存在抛异常")
        void getBranch_whenNotExists_shouldThrow() {
            when(branchMapper.selectById(999L)).thenReturn(null);

            assertThrows(ResourceNotFoundException.class,
                    () -> branchService.getBranch(999L));
        }

        @Test
        @DisplayName("查询分馆树 - 构建层级结构")
        void listBranchTree_shouldBuildHierarchy() {
            when(branchMapper.selectList(any())).thenReturn(Arrays.asList(testBranch, childBranch));

            java.util.List<BranchResponse> tree = branchService.listBranchTree();

            assertNotNull(tree);
            assertFalse(tree.isEmpty());
            assertEquals(1, tree.size());
            assertEquals("总馆", tree.get(0).getName());
            assertNotNull(tree.get(0).getChildren());
            assertEquals(1, tree.get(0).getChildren().size());
            assertEquals("分馆A", tree.get(0).getChildren().get(0).getName());
        }

        @Test
        @DisplayName("查询分馆列表 - 空列表")
        void listBranches_emptyList() {
            when(branchMapper.selectList(any())).thenReturn(Collections.emptyList());

            java.util.List<BranchResponse> branches = branchService.listBranches(null);

            assertNotNull(branches);
            assertTrue(branches.isEmpty());
        }
    }
}
