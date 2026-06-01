package com.library.system.service;

import com.library.system.dto.*;

import java.util.List;

/**
 * 借阅服务接口
 * <p>
 * 处理图书借阅、归还、续借等核心业务逻辑。
 * 借阅时会自动检查用户信用积分和可借数量限制，
 * 归还时自动计算逾期天数并调整信用积分。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
public interface BorrowService {

    /**
     * 借阅图书
     *
     * @param userId 用户ID
     * @param request 借阅请求
     * @return 借阅记录
     */
    BorrowResponse borrowBook(Long userId, BorrowRequest request);

    /**
     * 归还图书
     *
     * @param userId 用户ID
     * @param borrowId 借阅记录ID
     * @return 归还后的借阅记录
     */
    BorrowResponse returnBook(Long userId, Long borrowId);

    /**
     * 续借图书
     *
     * @param userId 用户ID
     * @param borrowId 借阅记录ID
     * @param days 续借天数
     * @return 续借后的借阅记录
     */
    BorrowResponse renewBook(Long userId, Long borrowId, Integer days);

    /**
     * 获取我的借阅列表
     *
     * @param userId 用户ID
     * @param current 当前页
     * @param size 每页大小
     * @param status 借阅状态筛选
     * @return 分页结果
     */
    PageResult<BorrowResponse> getMyBorrows(Long userId, Long current, Long size, String status);

    /**
     * 获取所有借阅列表（管理员用）
     *
     * @param current 当前页
     * @param size 每页大小
     * @param status 借阅状态筛选
     * @return 分页结果
     */
    PageResult<BorrowResponse> getAllBorrows(Long current, Long size, String status);

    /**
     * 获取借阅详情
     *
     * @param borrowId 借阅记录ID
     * @return 借阅详情
     */
    BorrowResponse getBorrowById(Long borrowId);

    /**
     * 获取借阅详情（带用户归属检查）
     * FIXED: SEC-007 防止水平越权漏洞
     *
     * @param borrowId 借阅记录ID
     * @param currentUserId 当前用户ID
     * @param currentRole 当前用户角色
     * @return 借阅详情
     * @throws RuntimeException 如果无权限访问
     */
    BorrowResponse getBorrowByIdWithOwnershipCheck(Long borrowId, Long currentUserId, String currentRole);

    /**
     * 检查用户是否有逾期未还图书
     *
     * @param userId 用户ID
     * @return 是否有逾期
     */
    boolean hasOverdueBooks(Long userId);

    List<BorrowExportDTO> getExportData(String status);
}
