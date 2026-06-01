package com.library.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.system.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 用户数据访问层
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0")
    User selectByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询用户
     */
    @Select("SELECT * FROM sys_user WHERE email = #{email} AND deleted = 0")
    User selectByEmail(@Param("email") String email);

    /**
     * 根据手机号查询用户
     */
    @Select("SELECT * FROM sys_user WHERE phone = #{phone} AND deleted = 0")
    User selectByPhone(@Param("phone") String phone);

    /**
     * 更新用户借阅数量
     */
    @Update("UPDATE sys_user SET borrow_count = borrow_count + #{delta}, " +
            "version = version + 1 WHERE id = #{userId} AND version = #{version}")
    int updateBorrowCount(@Param("userId") Long userId, @Param("delta") int delta, @Param("version") Integer version);

    /**
     * 更新用户信用积分
     */
    @Update("UPDATE sys_user SET credit_score = credit_score + #{delta}, " +
            "version = version + 1 WHERE id = #{userId} AND version = #{version}")
    int updateCreditScore(@Param("userId") Long userId, @Param("delta") int delta, @Param("version") Integer version);

    @Update("UPDATE sys_user SET violation_count = violation_count + 1, " +
            "ban_until = CASE WHEN violation_count + 1 >= #{threshold} THEN #{banUntil} ELSE ban_until END " +
            "WHERE id = #{userId}")
    int incrementViolationCount(@Param("userId") Long userId, @Param("threshold") int threshold, @Param("banUntil") LocalDateTime banUntil);
}
