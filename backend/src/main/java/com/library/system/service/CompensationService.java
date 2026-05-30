package com.library.system.service;

import com.library.system.dto.CompensationRequest;
import com.library.system.dto.CompensationResponse;
import com.library.system.dto.PageResult;

import java.math.BigDecimal;

/**
 * 赔偿服务接口
 *
 * @author Library Team
 * @version 2.0.0
 */
public interface CompensationService {

    /**
     * 创建赔偿订单
     */
    CompensationResponse createCompensation(CompensationRequest request, Long operatorId);

    /**
     * 分页查询赔偿列表
     */
    PageResult<CompensationResponse> listCompensations(Long current, Long size, String status);

    /**
     * 获取赔偿详情
     */
    CompensationResponse getCompensationById(Long id);

    /**
     * 现金赔偿处理
     */
    CompensationResponse processCashPayment(Long id, Long operatorId, String remark);

    /**
     * 积分抵扣赔偿
     */
    CompensationResponse processCreditPayment(Long id, Long operatorId, Integer creditAmount, String remark);

    /**
     * 志愿服务抵扣赔偿
     */
    CompensationResponse processVolunteerPayment(Long id, Long operatorId, BigDecimal hours, String remark);

    /**
     * 取消赔偿订单
     */
    void cancelCompensation(Long id, Long operatorId, String reason);
}
