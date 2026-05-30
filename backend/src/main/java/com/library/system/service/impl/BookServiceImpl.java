package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.common.Constants;
import com.library.system.config.BloomFilterConfig;
import com.library.system.dto.*;
import com.library.system.entity.Book;
import com.library.system.mapper.BookMapper;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.BusinessException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 图书服务实现类
 * 使用Caffeine本地缓存 + Redis分布式缓存的二级缓存架构
 *
 * FIXED: P2-009 集成布隆过滤器，在getBookById前先进行布隆判断，
 * 防止缓存穿透攻击（恶意查询不存在的图书ID）
 */
@Slf4j
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "books")
public class BookServiceImpl implements BookService {

    private final BookMapper bookMapper;
    private final BloomFilterConfig bloomFilterConfig;

    @Override
    public PageResult<BookResponse> listBooks(Long current, Long size, String keyword, Long categoryId) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getDeleted, 0);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Book::getTitle, keyword)
                    .or()
                    .like(Book::getAuthor, keyword)
                    .or()
                    .like(Book::getIsbn, keyword));
        }

        if (categoryId != null) {
            wrapper.eq(Book::getCategoryId, categoryId);
        }

        wrapper.orderByDesc(Book::getBorrowCount);

        Page<Book> page = new Page<>(current, size);
        Page<Book> bookPage = bookMapper.selectPage(page, wrapper);

        List<BookResponse> records = bookPage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return PageResult.of(bookPage.getCurrent(), bookPage.getSize(),
                bookPage.getTotal(), records);
    }

    @Override
    @Cacheable(key = "#id", unless = "#result == null")
    public BookResponse getBookById(Long id) {
        
        // 布隆过滤器判断"可能不存在"时直接拒绝，避免穿透到数据库
        String bookKey = String.valueOf(id);
        if (!bloomFilterConfig.mightContainBook(bookKey)) {
            log.warn("布隆过滤器拦截: 图书ID={} 不可能存在，返回404", id);
            throw new ResourceNotFoundException(ErrorCode.BOOK_NOT_FOUND, "图书不存在");
        }

        Book book = bookMapper.selectById(id);
        if (book == null || book.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.BOOK_NOT_FOUND, "图书不存在"); 
        }
        return convertToResponse(book);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CachePut(key = "#result.id")
    public BookResponse createBook(BookRequest request) {
        if (bookMapper.selectByIsbn(request.getIsbn()) != null) {
            throw new BusinessException(ErrorCode.ISBN_DUPLICATE, "ISBN已存在"); 
        }

        Book book = Book.builder()
                .isbn(request.getIsbn())
                .title(request.getTitle())
                .author(request.getAuthor())
                .publisher(request.getPublisher())
                .publishDate(request.getPublishDate() != null && request.getPublishDate().length() >= 7 ?
                    safeParsePublishDate(request.getPublishDate()) : null)
                .categoryId(request.getCategoryId())
                .description(request.getDescription())
                .coverImage(request.getCoverImage())
                .location(request.getLocation())
                .totalCount(request.getTotalCount())
                .availableCount(request.getTotalCount())
                .price(request.getPrice())
                .borrowCount(0)
                .status(request.getStatus() != null ? request.getStatus() : Constants.BookStatus.NORMAL)
                .build();


        bookMapper.insert(book);

        // FIXED: P2-009 新增图书后同步更新布隆过滤器
        bloomFilterConfig.addBook(String.valueOf(book.getId()));

        // FIXED: PERF-004 说明：@CachePut(key = "#result.id")已自动将新图书写入L1和L2缓存
        // 由于新建图书ID不在缓存中，首次查询会miss，这是预期行为

        log.info("图书创建成功: {}", book.getTitle());
        return convertToResponse(book);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CachePut(key = "#id")
    public BookResponse updateBook(Long id, BookRequest request) {
        Book book = bookMapper.selectById(id);
        if (book == null || book.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.BOOK_NOT_FOUND, "图书不存在"); 
        }

        if (!book.getIsbn().equals(request.getIsbn())) {
            Book existingBook = bookMapper.selectByIsbn(request.getIsbn());
            if (existingBook != null && !existingBook.getId().equals(id)) {
                throw new BusinessException(ErrorCode.ISBN_DUPLICATE, "ISBN已存在"); 
            }
        }

        book.setIsbn(request.getIsbn());
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPublisher(request.getPublisher());
        book.setPublishDate(request.getPublishDate() != null && request.getPublishDate().length() >= 7 ?
            safeParsePublishDate(request.getPublishDate()) : null);
        book.setCategoryId(request.getCategoryId());
        book.setDescription(request.getDescription());
        book.setCoverImage(request.getCoverImage());
        book.setLocation(request.getLocation());
        book.setPrice(request.getPrice());
        book.setStatus(request.getStatus());

        int oldTotal = book.getTotalCount();
        int newTotal = request.getTotalCount();
        int diff = newTotal - oldTotal;
        book.setTotalCount(newTotal);
        book.setAvailableCount(Math.max(0, book.getAvailableCount() + diff));

        bookMapper.updateById(book);

        log.info("图书更新成功: {}", book.getTitle());
        return convertToResponse(book);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(key = "#id")
    public void deleteBook(Long id) {
        Book book = bookMapper.selectById(id);
        if (book == null || book.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.BOOK_NOT_FOUND, "图书不存在"); 
        }

        bookMapper.deleteById(id);

        log.info("图书删除成功: {}", id);
    }

    @Override
    @Cacheable(key = "'hot:' + #limit", unless = "#result == null || #result.isEmpty()")
    public List<BookResponse> getHotBooks(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        List<Book> books = bookMapper.selectHotBooks(limit);
        return books.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(key = "'new:' + #limit", unless = "#result == null || #result.isEmpty()")
    public List<BookResponse> getNewBooks(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        List<Book> books = bookMapper.selectNewBooks(limit);
        return books.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isIsbnExists(String isbn) {
        return bookMapper.selectByIsbn(isbn) != null;
    }

    /**
     * 安全解析出版日期字符串为 LocalDate
     * 支持 "yyyy-MM" 或 "yyyy-MM-dd" 格式，防止 StringIndexOutOfBoundsException
     */
    private java.time.LocalDate safeParsePublishDate(String dateStr) {
        try {
            if (dateStr.length() >= 10) {
                return java.time.LocalDate.parse(dateStr.substring(0, 10));
            } else if (dateStr.length() >= 7) {
                return java.time.LocalDate.parse(dateStr.substring(0, 7) + "-01");
            }
        } catch (Exception e) {
            log.warn("出版日期解析失败: dateStr={}, error={}", dateStr, e.getMessage());
        }
        return null;
    }

    private BookResponse convertToResponse(Book book) {
        return BookResponse.builder()
                .id(book.getId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .author(book.getAuthor())
                .publisher(book.getPublisher())
                .publishDate(book.getPublishDate() != null ? book.getPublishDate().toString() : null)
                .categoryId(book.getCategoryId())
                .categoryName(book.getCategoryName())
                .description(book.getDescription())
                .coverImage(book.getCoverImage())
                .location(book.getLocation())
                .totalCount(book.getTotalCount())
                .availableCount(book.getAvailableCount())
                .price(book.getPrice())
                .borrowCount(book.getBorrowCount())
                .status(book.getStatus())
                .createTime(book.getCreateTime())
                .updateTime(book.getUpdateTime())
                .build();
    }
}
