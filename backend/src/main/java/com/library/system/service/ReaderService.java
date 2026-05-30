package com.library.system.service;

import com.library.system.dto.PageResult;
import com.library.system.dto.ReaderResponse;
import com.library.system.entity.User;

/**
 * 读者服务接口 
 * <p>
 * 提供读者的CRUD操作和管理功能，包括读者注册、信息修改、密码管理、
 * 账户状态管理等。将业务逻辑从Controller层剥离。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
public interface ReaderService {

    /**
     * 分页查询读者列表
     *
     * @param current 当前页
     * @param size    每页大小
     * @param keyword 关键词
     * @param role    角色筛选
     * @return 分页结果
     */
    PageResult<ReaderResponse> listReaders(Long current, Long size, String keyword, String role);

    /**
     * 获取读者详情
     *
     * @param id 读者ID
     * @return 读者详情
     */
    ReaderResponse getReaderById(Long id);

    /**
     * 获取当前登录读者信息
     *
     * @param username 用户名
     * @return 读者信息
     */
    ReaderResponse getCurrentReader(String username);

    /**
     * 注册新读者
     *
     * @param username 用户名
     * @param password 密码
     * @param realName 真实姓名
     * @param phone    手机号
     * @param email    邮箱
     * @return 注册结果
     */
    ReaderResponse registerReader(String username, String password, String realName, String phone, String email);

    /**
     * 更新读者信息
     *
     * @param id             读者ID
     * @param currentUserId  当前用户ID
     * @param isAdmin        是否管理员
     * @param realName       真实姓名
     * @param phone          手机号
     * @param email          邮箱
     * @param avatar         头像
     * @param role           角色（管理员可修改）
     * @param status         状态（管理员可修改）
     * @param creditScore    信用积分（管理员可修改）
     * @param maxBorrowCount 最大借阅数（管理员可修改）
     * @return 更新结果
     */
    ReaderResponse updateReader(Long id, Long currentUserId, boolean isAdmin,
                                String realName, String phone, String email, String avatar,
                                String role, String status, Integer creditScore, Integer maxBorrowCount);

    /**
     * 修改密码
     *
     * @param id            读者ID
     * @param currentUserId 当前用户ID
     * @param oldPassword   旧密码
     * @param newPassword   新密码
     */
    void changePassword(Long id, Long currentUserId, String oldPassword, String newPassword);

    /**
     * 删除读者
     *
     * @param id 读者ID
     */
    void deleteReader(Long id);

    /**
     * 重置读者密码
     *
     * @param id 读者ID
     */
    void resetPassword(Long id);

    /**
     * 更新读者状态
     *
     * @param id       读者ID
     * @param disabled 是否禁用
     */
    void updateReaderStatus(Long id, Boolean disabled);

    /**
     * FIXED: ARCH-003 根据用户名查询用户信息
     *
     * @param username 用户名
     * @return 用户信息，不存在返回null
     */
    User findByUsername(String username);

    /**
     * 根据用户名获取用户ID
     *
     * @param username 用户名
     * @return 用户ID，不存在返回null
     */
    Long getUserIdByUsername(String username);

    /**
     * 判断当前用户是否为管理员或图书管理员
     *
     * @param username 用户名
     * @return true=是管理员，false=否
     */
    boolean isCurrentUserAdmin(String username);
}
