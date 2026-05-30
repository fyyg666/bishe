package com.library.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.system.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 图书数据访问层
 */
@Mapper
public interface BookMapper extends BaseMapper<Book> {

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
