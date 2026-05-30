package com.library.system.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.library.system.entity.Book;
import com.library.system.entity.User;
import com.library.system.mapper.BookMapper;
import com.library.system.mapper.UserMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 布隆过滤器配置类
 * 用于防止缓存穿透
 *
 * FIXED: P2-009 启动时预加载已有图书和用户ID到布隆过滤器，
 * 并注入Mapper支持动态新增
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BloomFilterConfig {

    private final BookMapper bookMapper;
    private final UserMapper userMapper;

    private BloomFilter<String> bookBloomFilter;
    private BloomFilter<String> userBloomFilter;

    private static final int EXPECTED_INSERTIONS = 100000;
    private static final double FPP = 0.01;

    @PostConstruct
    public void init() {
        // 初始化图书布隆过滤器
        bookBloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                EXPECTED_INSERTIONS,
                FPP
        );

        // 初始化用户布隆过滤器
        userBloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                EXPECTED_INSERTIONS,
                FPP
        );

        preloadBookIds();
        preloadUserIds();

        log.info("布隆过滤器初始化完成, 预加载图书和用户ID");
    }

    /**
     * 预加载已有图书ID到布隆过滤器
     */
    private void preloadBookIds() {
        try {
            LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Book::getDeleted, 0).select(Book::getId);
            List<Book> books = bookMapper.selectList(wrapper);
            for (Book book : books) {
                bookBloomFilter.put(String.valueOf(book.getId()));
            }
            log.info("布隆过滤器预加载图书ID: {} 条", books.size());
        } catch (Exception e) {
            log.warn("布隆过滤器预加载图书ID失败（非致命）: {}", e.getMessage());
        }
    }

    /**
     * 预加载已有用户ID到布隆过滤器
     */
    private void preloadUserIds() {
        try {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getDeleted, 0).select(User::getId);
            List<User> users = userMapper.selectList(wrapper);
            for (User user : users) {
                userBloomFilter.put(String.valueOf(user.getId()));
            }
            log.info("布隆过滤器预加载用户ID: {} 条", users.size());
        } catch (Exception e) {
            log.warn("布隆过滤器预加载用户ID失败（非致命）: {}", e.getMessage());
        }
    }

    public BloomFilter<String> getBookBloomFilter() {
        return bookBloomFilter;
    }

    public BloomFilter<String> getUserBloomFilter() {
        return userBloomFilter;
    }

    public void addBook(String bookId) {
        bookBloomFilter.put(bookId);
    }

    public boolean mightContainBook(String bookId) {
        return bookBloomFilter.mightContain(bookId);
    }

    public void addUser(String userId) {
        userBloomFilter.put(userId);
    }

    public boolean mightContainUser(String userId) {
        return userBloomFilter.mightContain(userId);
    }
}
