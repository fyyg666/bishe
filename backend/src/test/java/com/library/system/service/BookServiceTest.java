package com.library.system.service;

import com.library.system.base.BaseTest;
import com.library.system.config.BloomFilterConfig;
import com.library.system.dto.BookRequest;
import com.library.system.dto.BookResponse;
import com.library.system.dto.PageResult;
import com.library.system.entity.Book;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.BookMapper;
import com.library.system.service.impl.BookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("BookService 单元测试")
class BookServiceTest extends BaseTest {

    @Mock
    private BookMapper bookMapper;

    @Mock
    private BloomFilterConfig bloomFilterConfig;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book testBook;
    private BookRequest bookRequest;
    
    @BeforeEach
    void mockBloomFilterGlobal() {
        lenient().when(bloomFilterConfig.mightContainBook(anyString())).thenReturn(true);
    }

    @BeforeEach
    void setUp() {
        testBook = new Book();
        testBook.setId(1L);
        testBook.setIsbn("978-7-111-11111-1");
        testBook.setTitle("测试图书");
        testBook.setAuthor("测试作者");
        testBook.setPublisher("测试出版社");
        testBook.setCategoryId(1L);
        testBook.setTotalCount(10);
        testBook.setAvailableCount(8);
        testBook.setDescription("这是一本测试图书");
        testBook.setPrice(new BigDecimal("59.00"));
        testBook.setDeleted(0);

        bookRequest = new BookRequest();
        bookRequest.setIsbn("978-7-111-22222-2");
        bookRequest.setTitle("新书测试");
        bookRequest.setAuthor("作者");
        bookRequest.setPublisher("出版社");
        bookRequest.setCategoryId(1L);
        bookRequest.setTotalCount(5);
        bookRequest.setPrice(new BigDecimal("39.00"));
    }

    @Nested
    @DisplayName("图书查询用例")
    class QueryTests {

        @Test
        @DisplayName("分页查询 - 应返回列表")
        void listBooks_shouldReturnPage() {
            when(bookMapper.selectPage(any(), any())).thenAnswer(invocation -> {
                com.baomidou.mybatisplus.core.metadata.IPage<Book> page = invocation.getArgument(0);
                page.setRecords(Arrays.asList(testBook));
                page.setTotal(1);
                return page;
            });

            PageResult<BookResponse> result = bookService.listBooks(1L, 10L, null, null, null);

            assertNotNull(result);
            assertEquals(1, result.getTotal());
            assertEquals(1, result.getRecords().size());
            assertEquals("测试图书", result.getRecords().get(0).getTitle());
        }

        @Test
        @DisplayName("获取图书详情 - 存在")
        void getBookById_whenExists_shouldReturnBook() {
            when(bookMapper.selectById(1L)).thenReturn(testBook);

            BookResponse response = bookService.getBookById(1L);

            assertNotNull(response);
            assertEquals("测试图书", response.getTitle());
            assertEquals("测试作者", response.getAuthor());
        }

        @Test
        @DisplayName("获取图书详情 - 不存在")
        void getBookById_whenNotExists_shouldThrowException() {
            when(bookMapper.selectById(999L)).thenReturn(null);

            assertThrows(ResourceNotFoundException.class, () -> bookService.getBookById(999L));
        }

        @Test
        @DisplayName("获取热门图书 - 应返回列表")
        void getHotBooks_shouldReturnList() {
            when(bookMapper.selectHotBooks(5)).thenReturn(Arrays.asList(testBook));

            List<BookResponse> books = bookService.getHotBooks(5);

            assertNotNull(books);
            assertFalse(books.isEmpty());
            assertEquals(1, books.size());
        }
    }

    @Nested
    @DisplayName("图书创建用例")
    class CreateTests {

        @Test
        @DisplayName("创建图书 - ISBN唯一时成功")
        void createBook_withUniqueIsbn_shouldSucceed() {
            when(bookMapper.selectByIsbn("978-7-111-22222-2")).thenReturn(null);
            when(bookMapper.insert(any(Book.class))).thenReturn(1);

            BookResponse response = bookService.createBook(bookRequest);

            assertNotNull(response);

            ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
            verify(bookMapper).insert(captor.capture());
            Book saved = captor.getValue();
            assertEquals("新书测试", saved.getTitle());
            assertEquals(5, saved.getTotalCount());
            assertEquals(5, saved.getAvailableCount()); // 初始库存=可用库存
        }

        @Test
        @DisplayName("创建图书 - ISBN重复时抛异常")
        void createBook_withDuplicateIsbn_shouldThrowException() {
            when(bookMapper.selectByIsbn("978-7-111-22222-2")).thenReturn(testBook);

            assertThrows(BusinessException.class, () -> bookService.createBook(bookRequest));
            verify(bookMapper, never()).insert(any());
        }
    }

    @Nested
    @DisplayName("图书更新用例")
    class UpdateTests {

        @Test
        @DisplayName("更新图书 - 存在时成功")
        void updateBook_whenExists_shouldSucceed() {
            when(bookMapper.selectById(1L)).thenReturn(testBook);
            when(bookMapper.updateById(any(Book.class))).thenReturn(1);

            BookResponse response = bookService.updateBook(1L, bookRequest);

            assertNotNull(response);
            verify(bookMapper).updateById(any(Book.class));
        }

        @Test
        @DisplayName("更新图书 - 不存在时抛异常")
        void updateBook_whenNotExists_shouldThrowException() {
            when(bookMapper.selectById(999L)).thenReturn(null);

            assertThrows(ResourceNotFoundException.class,
                    () -> bookService.updateBook(999L, bookRequest));
        }
    }

    @Nested
    @DisplayName("图书删除用例")
    class DeleteTests {

        @Test
        @DisplayName("删除图书 - 存在时成功")
        void deleteBook_whenExists_shouldSucceed() {
            when(bookMapper.selectById(1L)).thenReturn(testBook);

            bookService.deleteBook(1L);

            verify(bookMapper).deleteById(1L);
        }

        @Test
        @DisplayName("删除图书 - 不存在时抛异常")
        void deleteBook_whenNotExists_shouldThrowException() {
            when(bookMapper.selectById(999L)).thenReturn(null);

            assertThrows(ResourceNotFoundException.class, () -> bookService.deleteBook(999L));
        }

        @Test
        @DisplayName("删除失败 - 图书有活跃借阅")
        void deleteBook_withActiveBorrows_shouldThrowException() {
            testBook.setBorrowCount(3);
            when(bookMapper.selectById(1L)).thenReturn(testBook);
            assertThrows(BusinessException.class,
                    () -> bookService.deleteBook(1L));
        }
    }

    @Nested
    @DisplayName("ISBN校验用例")
    class IsbnTests {

        @Test
        @DisplayName("ISBN存在 - 返回true")
        void isIsbnExists_whenExists_shouldReturnTrue() {
            when(bookMapper.selectByIsbn("978-7-111-11111-1")).thenReturn(testBook);

            assertTrue(bookService.isIsbnExists("978-7-111-11111-1"));
        }

        @Test
        @DisplayName("ISBN不存在 - 返回false")
        void isIsbnExists_whenNotExists_shouldReturnFalse() {
            when(bookMapper.selectByIsbn("978-999-99999-9")).thenReturn(null);

            assertFalse(bookService.isIsbnExists("978-999-99999-9"));
        }
    }

    @Nested
    @DisplayName("ISBN冲突与布隆过滤器用例")
    class IsbnConflictAndBloomTests {

        @Test
        @DisplayName("创建图书 - ISBN已存在抛异常")
        void createBook_duplicateIsbn_shouldThrow() {
            when(bookMapper.selectByIsbn("978-7-111-22222-2")).thenReturn(testBook);

            assertThrows(BusinessException.class,
                    () -> bookService.createBook(bookRequest));
            verify(bookMapper, never()).insert(any());
        }

        @Test
        @DisplayName("更新图书 - ISBN被其他图书使用抛异常")
        void updateBook_isbnConflictWithOther_shouldThrow() {
            Book anotherBook = new Book();
            anotherBook.setId(2L);
            anotherBook.setIsbn("978-7-111-33333-3");

            when(bookMapper.selectById(1L)).thenReturn(testBook);
            when(bookMapper.selectByIsbn("978-7-111-33333-3")).thenReturn(anotherBook);
            BookRequest updateRequest = new BookRequest();
            updateRequest.setIsbn("978-7-111-33333-3");
            updateRequest.setTitle("更新标题");
            updateRequest.setAuthor("更新作者");
            updateRequest.setPublisher("更新出版社");
            updateRequest.setCategoryId(1L);
            updateRequest.setTotalCount(5);
            updateRequest.setPrice(new BigDecimal("49.00"));

            assertThrows(BusinessException.class,
                    () -> bookService.updateBook(1L, updateRequest));
        }

        @Test
        @DisplayName("获取图书详情 - 布隆过滤器返回false应抛出404")
        void getBookById_bloomFilterMiss_shouldThrowNotFound() {
            // bloomFilterConfig mightContainBook already mocked to return true in @BeforeEach,
            // so override for this specific test
            doReturn(false).when(bloomFilterConfig).mightContainBook("999");

            assertThrows(ResourceNotFoundException.class,
                    () -> bookService.getBookById(999L));
            verify(bookMapper, never()).selectById(any());
        }
    }
}
