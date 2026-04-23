package com.library.system.service;

import com.library.system.dto.BookRequest;
import com.library.system.dto.BookResponse;
import com.library.system.entity.Book;
import com.library.system.mapper.BookMapper;
import com.library.system.service.impl.BookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 图书服务单元测试
 *
 * @author Library Team
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book testBook;
    private BookRequest testRequest;

    @BeforeEach
    void setUp() {
        testBook = new Book();
        testBook.setId(1L);
        testBook.setIsbn("9787111213826");
        testBook.setTitle("Java编程思想");
        testBook.setAuthor("Bruce Eckel");
        testBook.setCategoryId(1L);
        testBook.setTotalCount(5);
        testBook.setAvailableCount(3);
        testBook.setBorrowCount(2);
        testBook.setStatus(1);
        testBook.setDeleted(0);

        testRequest = new BookRequest();
        testRequest.setIsbn("9787111213826");
        testRequest.setTitle("Java编程思想");
        testRequest.setAuthor("Bruce Eckel");
        testRequest.setCategoryId(1L);
        testRequest.setTotalCount(5);
        testRequest.setPublisher("机械工业出版社");
        testRequest.setPublishDate("2007-06");
        testRequest.setPrice(new BigDecimal("108.00"));
    }

    @Test
    void testGetBookById_Success() {
        when(bookMapper.selectById(1L)).thenReturn(testBook);

        BookResponse result = bookService.getBookById(1L);

        assertNotNull(result);
        assertEquals("Java编程思想", result.getTitle());
        assertEquals("Bruce Eckel", result.getAuthor());
        verify(bookMapper).selectById(1L);
    }

    @Test
    void testGetBookById_NotFound() {
        when(bookMapper.selectById(999L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> bookService.getBookById(999L));
    }

    @Test
    void testCreateBook_Success() {
        when(bookMapper.selectByIsbn(anyString())).thenReturn(null);
        when(bookMapper.insert(any(Book.class))).thenReturn(1);

        BookResponse result = bookService.createBook(testRequest);

        assertNotNull(result);
        assertEquals("Java编程思想", result.getTitle());
        verify(bookMapper).insert(any(Book.class));
    }

    @Test
    void testCreateBook_DuplicateIsbn() {
        when(bookMapper.selectByIsbn(testRequest.getIsbn())).thenReturn(testBook);

        assertThrows(RuntimeException.class, () -> bookService.createBook(testRequest));
        verify(bookMapper, never()).insert(any(Book.class));
    }

    @Test
    void testGetHotBooks() {
        List<Book> hotBooks = Arrays.asList(testBook);
        when(bookMapper.selectHotBooks(10)).thenReturn(hotBooks);

        List<BookResponse> result = bookService.getHotBooks(10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Java编程思想", result.get(0).getTitle());
        verify(bookMapper).selectHotBooks(10);
    }
}

