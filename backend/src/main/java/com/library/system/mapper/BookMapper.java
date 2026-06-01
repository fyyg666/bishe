package com.library.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface BookMapper extends BaseMapper<Book> {

    List<Book> selectBookList(@Param("keyword") String keyword, @Param("categoryId") Long categoryId);

    Page<Book> selectBookPage(Page<Book> page, @Param("keyword") String keyword,
                              @Param("categoryId") Long categoryId, @Param("status") Integer status);

    Page<Book> selectAdvancedBookPage(Page<Book> page,
                                       @Param("title") String title,
                                       @Param("author") String author,
                                       @Param("isbn") String isbn,
                                       @Param("publisher") String publisher,
                                       @Param("categoryId") Long categoryId,
                                       @Param("publishDateStart") String publishDateStart,
                                       @Param("publishDateEnd") String publishDateEnd,
                                       @Param("orderBy") String orderBy);

    List<java.util.Map<String, Object>> selectCategoryFacet();

    List<java.util.Map<String, Object>> selectAuthorFacet();

    /**
     * 根据ISBN查询图书
     */
    @Select("SELECT * FROM book WHERE isbn = #{isbn} AND deleted = 0")
    Book selectByIsbn(@Param("isbn") String isbn);

    /**
     * 更新图书可借数量（乐观锁）
     */
    @Update("UPDATE book SET stock = stock + #{delta}, borrow_count = borrow_count + #{borrowDelta}, " +
            "version = version + 1 WHERE id = #{bookId} AND version = #{version} " +
            "AND stock + #{delta} >= 0")
    int updateAvailableCount(@Param("bookId") Long bookId, @Param("delta") int delta, 
                             @Param("version") Integer version, @Param("borrowDelta") int borrowDelta);

    /**
     * 查询热门图书
     * status=0 (Constants.BookStatus.NORMAL) 表示上架可借
     */
    @Select("SELECT * FROM book WHERE deleted = 0 AND status = 0 " +
            "ORDER BY borrow_count DESC LIMIT #{limit}")
    List<Book> selectHotBooks(@Param("limit") int limit);

    /**
     * 查询新书推荐
     * status=0 (Constants.BookStatus.NORMAL) 表示上架可借
     */
    List<Book> selectNewBooks(@Param("limit") int limit);

    /**
     * 悲观锁查询图书（用于并发控制）
     */
    @Select("SELECT * FROM book WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    Book selectByIdForUpdate(@Param("id") Long id);
}
