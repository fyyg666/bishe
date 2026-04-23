package com.library.system.service;

import com.library.system.dto.*;

import java.util.List;

/**
 * 图书服务接口
 * <p>
 * 提供图书的CRUD操作和查询功能，包括分页查询、热门图书获取、
 * ISBN去重检查等。图书数据基于MyBatis-Plus进行持久化，
 * 支持乐观锁并发控制和逻辑删除。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
public interface BookService {

    /**
     * 分页查询图书
     *
     * @param current 当前页
     * @param size 每页大小
     * @param keyword 关键词（标题/作者/ISBN）
     * @param categoryId 分类ID
     * @return 分页结果
     */
    PageResult<BookResponse> listBooks(Long current, Long size, String keyword, Long categoryId);

    /**
     * 获取图书详情
     *
     * @param id 图书ID
     * @return 图书详情
     */
    BookResponse getBookById(Long id);

    /**
     * 新增图书
     *
     * @param request 图书请求
     * @return 新增的图书
     */
    BookResponse createBook(BookRequest request);

    /**
     * 更新图书
     *
     * @param id 图书ID
     * @param request 图书请求
     * @return 更新后的图书
     */
    BookResponse updateBook(Long id, BookRequest request);

    /**
     * 删除图书
     *
     * @param id 图书ID
     */
    void deleteBook(Long id);

    /**
     * 获取热门图书
     *
     * @param limit 数量限制
     * @return 热门图书列表
     */
    List<BookResponse> getHotBooks(Integer limit);

    /**
     * 检查ISBN是否已存在
     *
     * @param isbn ISBN号
     * @return 是否存在
     */
    boolean isIsbnExists(String isbn);
}
