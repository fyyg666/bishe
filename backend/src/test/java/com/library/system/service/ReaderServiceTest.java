package com.library.system.service;

import com.library.system.base.BaseTest;
import com.library.system.dto.PageResult;
import com.library.system.dto.ReaderResponse;
import com.library.system.entity.User;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ForbiddenException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.UserMapper;
import com.library.system.service.impl.ReaderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ReaderService 单元测试")
class ReaderServiceTest extends BaseTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ReaderServiceImpl readerService;

    private User testReader;

    @BeforeEach
    void setUp() {
        testReader = new User();
        testReader.setId(1L);
        testReader.setUsername("reader1");
        testReader.setRealName("张三");
        testReader.setRole("READER");
        testReader.setStatus("NORMAL");
        testReader.setCreditScore(100);
        testReader.setBorrowCount(2);
        testReader.setDeleted(0);
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        lenient().when(passwordEncoder.matches(anyString(), anyString())).thenAnswer(inv ->
                inv.getArgument(0).equals("correct-password"));
    }

    @Nested
    @DisplayName("读者查询")
    class QueryTests {
        @Test
        void getReaderById_exists() {
            when(userMapper.selectById(1L)).thenReturn(testReader);
            var result = readerService.getReaderById(1L);
            assertNotNull(result);
            assertEquals("张三", result.getRealName());
        }

        @Test
        void getReaderById_notExists() {
            when(userMapper.selectById(999L)).thenReturn(null);
            assertThrows(ResourceNotFoundException.class, () -> readerService.getReaderById(999L));
        }

        @Test
        void listReaders_shouldReturnPage() {
            when(userMapper.selectPage(any(), any())).thenAnswer(inv -> {
                com.baomidou.mybatisplus.core.metadata.IPage<User> p = inv.getArgument(0);
                p.setRecords(java.util.Collections.singletonList(testReader));
                p.setTotal(1);
                return p;
            });
            PageResult<?> result = readerService.listReaders(1L, 10L, null, null);
            assertEquals(1, result.getTotal());
        }

        @Test
        @DisplayName("getCurrentReader - 存在时返回用户信息")
        void getCurrentReader_whenExists_shouldReturnReader() {
            when(userMapper.selectByUsername("reader1")).thenReturn(testReader);
            ReaderResponse result = readerService.getCurrentReader("reader1");
            assertNotNull(result);
            assertEquals("张三", result.getRealName());
        }

        @Test
        @DisplayName("getCurrentReader - 不存在时抛异常")
        void getCurrentReader_whenNotExists_shouldThrow() {
            when(userMapper.selectByUsername("nonexistent")).thenReturn(null);
            assertThrows(ResourceNotFoundException.class,
                    () -> readerService.getCurrentReader("nonexistent"));
        }
    }

    @Nested
    @DisplayName("读者状态管理")
    class StatusManagementTests {
        @Test
        void disableReader_success() {
            when(userMapper.selectById(1L)).thenReturn(testReader);
            readerService.updateReaderStatus(1L, true);
            assertEquals("DISABLED", testReader.getStatus());
            verify(userMapper).updateById(testReader);
        }

        @Test
        void enableReader_success() {
            testReader.setStatus("DISABLED");
            when(userMapper.selectById(1L)).thenReturn(testReader);
            readerService.updateReaderStatus(1L, false);
            assertEquals("NORMAL", testReader.getStatus());
            verify(userMapper).updateById(testReader);
        }
    }

    @Nested
    @DisplayName("读者注册用例")
    class RegistrationTests {

        @Test
        @DisplayName("registerReader - 成功注册读者")
        void registerReader_success() {
            when(userMapper.selectByUsername("newuser")).thenReturn(null);
            when(userMapper.selectByPhone("13800000000")).thenReturn(null);
            doAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(10L);
                return 1;
            }).when(userMapper).insert(any(User.class));

            ReaderResponse result = readerService.registerReader(
                    "newuser", "pass123", "新用户", "13800000000", "new@test.com");

            assertNotNull(result);
            assertEquals("newuser", result.getUsername());
            verify(userMapper).insert(any(User.class));
        }

        @Test
        @DisplayName("registerReader - 用户名重复抛异常")
        void registerReader_duplicateUsername_shouldThrow() {
            when(userMapper.selectByUsername("reader1")).thenReturn(testReader);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> readerService.registerReader("reader1", "pass", "张三", null, null));
            assertEquals(ErrorCode.READER_ALREADY_EXISTS, ex.getErrorCode());
        }

        @Test
        @DisplayName("registerReader - 手机号重复抛异常")
        void registerReader_duplicatePhone_shouldThrow() {
            User phoneUser = new User();
            phoneUser.setId(2L);
            phoneUser.setPhone("13800000000");
            when(userMapper.selectByUsername("newuser")).thenReturn(null);
            when(userMapper.selectByPhone("13800000000")).thenReturn(phoneUser);

            assertThrows(BusinessException.class,
                    () -> readerService.registerReader("newuser", "pass", "新用户", "13800000000", null));
        }
    }

    @Nested
    @DisplayName("读者更新用例")
    class UpdateTests {

        @Test
        @DisplayName("updateReader - 本人更新成功")
        void updateReader_bySelf_success() {
            when(userMapper.selectById(1L)).thenReturn(testReader);

            ReaderResponse result = readerService.updateReader(1L, 1L, false,
                    "新名字", null, null, null, null, null, null, null);

            assertEquals("新名字", result.getRealName());
            verify(userMapper).updateById(any(User.class));
        }

        @Test
        @DisplayName("updateReader - 非本人非管理员抛Forbidden")
        void updateReader_byOther_shouldThrow() {
            when(userMapper.selectById(1L)).thenReturn(testReader);

            assertThrows(ForbiddenException.class,
                    () -> readerService.updateReader(1L, 2L, false, "新名字", null, null, null, null, null, null, null));
        }

        @Test
        @DisplayName("updateReader - 手机号被其他用户使用抛异常")
        void updateReader_phoneConflict_shouldThrow() {
            User phoneOwner = new User();
            phoneOwner.setId(2L);
            phoneOwner.setPhone("13900000000");
            when(userMapper.selectById(1L)).thenReturn(testReader);
            when(userMapper.selectByPhone("13900000000")).thenReturn(phoneOwner);

            assertThrows(BusinessException.class,
                    () -> readerService.updateReader(1L, 1L, false, null, "13900000000", null, null, null, null, null, null));
        }

        @Test
        @DisplayName("updateReader - 管理员可修改角色和状态")
        void updateReader_adminCanChangeRoleAndStatus() {
            when(userMapper.selectById(1L)).thenReturn(testReader);

            readerService.updateReader(1L, 2L, true, null, null, null, null, "LIBRARIAN", "DISABLED", 200, 10);

            verify(userMapper).updateById(argThat(u ->
                    "LIBRARIAN".equals(u.getRole()) && "DISABLED".equals(u.getStatus())));
        }
    }

    @Nested
    @DisplayName("密码管理用例")
    class PasswordTests {

        @Test
        @DisplayName("changePassword - 成功修改")
        void changePassword_success() {
            testReader.setPassword("encoded-old");
            when(userMapper.selectById(1L)).thenReturn(testReader);
            when(passwordEncoder.matches("correct-password", "encoded-old")).thenReturn(true);

            readerService.changePassword(1L, 1L, "correct-password", "new-password");

            verify(userMapper).updateById(argThat(u ->
                    u.getPassword() != null && !u.getPassword().equals("encoded-old")));
        }

        @Test
        @DisplayName("changePassword - 旧密码错误抛异常")
        void changePassword_wrongOldPassword_shouldThrow() {
            testReader.setPassword("encoded-old");
            when(userMapper.selectById(1L)).thenReturn(testReader);
            when(passwordEncoder.matches("wrong-password", "encoded-old")).thenReturn(false);

            assertThrows(BusinessException.class,
                    () -> readerService.changePassword(1L, 1L, "wrong-password", "new-password"));
        }

        @Test
        @DisplayName("changePassword - 非本人操作抛Forbidden")
        void changePassword_notOwner_shouldThrow() {
            when(userMapper.selectById(1L)).thenReturn(testReader);

            assertThrows(ForbiddenException.class,
                    () -> readerService.changePassword(1L, 2L, "old", "new"));
        }

        @Test
        @DisplayName("resetPassword - 成功重置为默认密码")
        void resetPassword_success() {
            when(userMapper.selectById(1L)).thenReturn(testReader);

            readerService.resetPassword(1L);

            verify(userMapper).updateById(any(User.class));
        }

        @Test
        @DisplayName("resetPassword - 读者不存在抛异常")
        void resetPassword_notFound_shouldThrow() {
            when(userMapper.selectById(999L)).thenReturn(null);

            assertThrows(ResourceNotFoundException.class,
                    () -> readerService.resetPassword(999L));
        }
    }

    @Nested
    @DisplayName("读者删除用例")
    class DeleteTests {

        @Test
        @DisplayName("deleteReader - 无未还书时成功删除")
        void deleteReader_noActiveBorrows_shouldSucceed() {
            testReader.setBorrowCount(0);
            when(userMapper.selectById(1L)).thenReturn(testReader);

            readerService.deleteReader(1L);

            verify(userMapper).deleteById(1L);
        }

        @Test
        @DisplayName("deleteReader - 有未还书时抛异常")
        void deleteReader_hasActiveBorrows_shouldThrow() {
            when(userMapper.selectById(1L)).thenReturn(testReader);

            assertThrows(BusinessException.class,
                    () -> readerService.deleteReader(1L));
        }

        @Test
        @DisplayName("deleteReader - 读者不存在抛异常")
        void deleteReader_notFound_shouldThrow() {
            when(userMapper.selectById(999L)).thenReturn(null);

            assertThrows(ResourceNotFoundException.class,
                    () -> readerService.deleteReader(999L));
        }
    }

    @Nested
    @DisplayName("辅助方法用例")
    class UtilityTests {

        @Test
        @DisplayName("findByUsername - 存在时返回User")
        void findByUsername_exists() {
            when(userMapper.selectByUsername("reader1")).thenReturn(testReader);
            assertNotNull(readerService.findByUsername("reader1"));
        }

        @Test
        @DisplayName("findByUsername - 不存在时返回null")
        void findByUsername_notExists() {
            when(userMapper.selectByUsername("none")).thenReturn(null);
            assertNull(readerService.findByUsername("none"));
        }

        @Test
        @DisplayName("getUserIdByUsername - 存在时返回ID")
        void getUserIdByUsername_exists() {
            when(userMapper.selectByUsername("reader1")).thenReturn(testReader);
            assertEquals(1L, readerService.getUserIdByUsername("reader1"));
        }

        @Test
        @DisplayName("getUserIdByUsername - 不存在时返回null")
        void getUserIdByUsername_notExists() {
            when(userMapper.selectByUsername("none")).thenReturn(null);
            assertNull(readerService.getUserIdByUsername("none"));
        }

        @Test
        @DisplayName("isCurrentUserAdmin - 管理员返回true")
        void isCurrentUserAdmin_admin_returnsTrue() {
            testReader.setRole("ADMIN");
            when(userMapper.selectByUsername("admin")).thenReturn(testReader);
            assertTrue(readerService.isCurrentUserAdmin("admin"));
        }

        @Test
        @DisplayName("isCurrentUserAdmin - 读者返回false")
        void isCurrentUserAdmin_reader_returnsFalse() {
            when(userMapper.selectByUsername("reader1")).thenReturn(testReader);
            assertFalse(readerService.isCurrentUserAdmin("reader1"));
        }

        @Test
        @DisplayName("isCurrentUserAdmin - 用户不存在返回false")
        void isCurrentUserAdmin_notExists_returnsFalse() {
            when(userMapper.selectByUsername("none")).thenReturn(null);
            assertFalse(readerService.isCurrentUserAdmin("none"));
        }
    }
}
