package com.library.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.system.entity.BorrowRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 借阅记录数据访问层
 */
@Mapper
public interface BorrowRecordMapper extends BaseMapper<BorrowRecord> {

    /**
     * 查询用户的借阅记录
     */
    @Select("SELECT * FROM borrow_record WHERE user_id = #{userId} AND deleted = 0 " +
            "ORDER BY create_time DESC")
    List<BorrowRecord> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询用户当前借阅中的记录
     */
    @Select("SELECT * FROM borrow_record WHERE user_id = #{userId} AND status = 'BORROWED' AND deleted = 0")
    List<BorrowRecord> selectCurrentBorrows(@Param("userId") Long userId);

    /**
     * 查询图书的借阅记录
     */
    @Select("SELECT * FROM borrow_record WHERE book_id = #{bookId} AND deleted = 0 " +
            "ORDER BY create_time DESC")
    List<BorrowRecord> selectByBookId(@Param("bookId") Long bookId);

    /**
     * 查询逾期未还的借阅记录
     */
    @Select("SELECT * FROM borrow_record WHERE status = 'BORROWED' AND due_date < CURDATE() AND deleted = 0")
    List<BorrowRecord> selectOverdueRecords();

    /**
     * 统计用户借阅数量
     */
    @Select("SELECT COUNT(*) FROM borrow_record WHERE user_id = #{userId} AND status = 'BORROWED' AND deleted = 0")
    int countCurrentBorrows(@Param("userId") Long userId);

    /**
     * 批量查询指定日期范围的借阅和归还数量
     * FIXED: PERF-001 替代N+1循环查询，一次SQL获取所有数据
     */
    @Select("<script>" +
            "SELECT " +
            "  DATE(create_time) as borrow_date, " +
            "  COUNT(*) as borrow_count, " +
            "  SUM(CASE WHEN status = 'RETURNED' AND DATE(return_date) IS NOT NULL THEN 1 ELSE 0 END) as return_count " +
            "FROM borrow_record " +
            "WHERE deleted = 0 " +
            "  AND create_time >= #{startDate} " +
            "  AND create_time < #{endDate} " +
            "GROUP BY DATE(create_time) " +
            "ORDER BY borrow_date" +
            "</script>")
    java.util.List<java.util.Map<String, Object>> selectBorrowStatsByDateRange(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);
}
