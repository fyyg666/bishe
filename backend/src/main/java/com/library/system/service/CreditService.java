package com.library.system.service;

import com.library.system.dto.*;

/**
 * 信用积分服务接口
 * <p>
 * 管理用户信用积分的查询、增加、扣减和变动日志记录。
 * 积分变动会触发相关业务事件（如借阅奖励、逾期扣减、签到奖励等），
 * 所有变动均记录到积分日志表中以供审计。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
public interface CreditService {

    /**
     * 获取用户当前积分
     *
     * @param userId 用户ID
     * @return 当前积分
     */
    Integer getUserCredit(Long userId);

    /**
     * 获取用户积分日志
     *
     * @param userId 用户ID
     * @param current 当前页
     * @param size 每页大小
     * @return 分页结果
     */
    PageResult<CreditLogResponse> getCreditLogs(Long userId, Long current, Long size);

    /**
     * 增加积分
     *
     * @param userId 用户ID
     * @param value 积分值
     * @param type 变动类型
     * @param description 描述
     * @param relatedId 关联业务ID
     * @param relatedType 关联业务类型
     */
    void addCredit(Long userId, Integer value, String type, String description, 
                   Long relatedId, String relatedType);

    /**
     * 扣减积分
     *
     * @param userId 用户ID
     * @param value 积分值
     * @param type 变动类型
     * @param description 描述
     * @param relatedId 关联业务ID
     * @param relatedType 关联业务类型
     */
    void deductCredit(Long userId, Integer value, String type, String description,
                      Long relatedId, String relatedType);

    /**
     * 处理借阅积分奖励
     *
     * @param userId 用户ID
     * @param borrowId 借阅记录ID
     */
    void processBorrowCredit(Long userId, Long borrowId);

    /**
     * 处理归还积分奖励/扣减
     *
     * @param userId 用户ID
     * @param borrowId 借阅记录ID
     * @param overdueDays 逾期天数
     */
    void processReturnCredit(Long userId, Long borrowId, Integer overdueDays);

    /**
     * 处理签到积分奖励
     *
     * @param userId 用户ID
     * @param reservationId 预约记录ID
     */
    void processCheckInCredit(Long userId, Long reservationId);
}
