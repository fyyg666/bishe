package com.library.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.system.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志数据访问层
 * <p>
 * 提供操作日志的持久化操作，包括日志记录、分页查询和统计分析。
 * 支持按模块、用户、时间范围等条件查询操作日志。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    /**
     * 查询指定用户的操作日志
     *
     * @param userId 用户ID
     * @param limit  查询数量限制
     * @return 操作日志列表（按时间降序）
     */
    @Select("SELECT * FROM sys_operation_log WHERE user_id = #{userId} AND deleted = 0 " +
            "ORDER BY create_time DESC LIMIT #{limit}")
    List<OperationLog> selectByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 查询指定模块的操作日志
     *
     * @param module 模块名称
     * @param limit  查询数量限制
     * @return 操作日志列表（按时间降序）
     */
    @Select("SELECT * FROM sys_operation_log WHERE module = #{module} AND deleted = 0 " +
            "ORDER BY create_time DESC LIMIT #{limit}")
    List<OperationLog> selectByModule(@Param("module") String module, @Param("limit") int limit);

    /**
     * 统计指定时间范围内的操作次数
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 操作次数
     */
    @Select("SELECT COUNT(*) FROM sys_operation_log WHERE deleted = 0 " +
            "AND create_time >= #{startTime} AND create_time <= #{endTime}")
    long countByTimeRange(@Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime);

    /**
     * 清理过期的操作日志（逻辑删除）
     * <p>
     * 将超过指定天数的日志标记为已删除，用于日志定期清理。
     * </p>
     *
     * @param days 保留天数
     * @return 受影响的行数
     */
    @Select("UPDATE sys_operation_log SET deleted = 1 " +
            "WHERE deleted = 0 AND create_time < DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int cleanExpiredLogs(@Param("days") int days);
}
