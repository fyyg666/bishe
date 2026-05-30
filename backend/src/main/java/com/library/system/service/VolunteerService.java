package com.library.system.service;

import com.library.system.dto.PageResult;
import com.library.system.dto.VolunteerRequest;
import com.library.system.dto.VolunteerResponse;
import com.library.system.dto.VolunteerStatsDto;

/**
 * 志愿服务接口 
 * <p>
 * 提供志愿服务的CRUD操作和审核功能，包括服务申请、记录更新、审核通过/拒绝等。
 * 将业务逻辑从Controller层剥离。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
public interface VolunteerService {

    /**
     * 分页查询志愿服务记录
     */
    PageResult<VolunteerResponse> listVolunteers(Long current, Long size, String status);

    /**
     * 获取当前用户的志愿服务记录
     */
    PageResult<VolunteerResponse> getMyVolunteers(Long current, Long size, Long userId);

    /**
     * 获取志愿服务详情
     */
    VolunteerResponse getVolunteerById(Long id);

    /**
     * 申请志愿服务
     */
    VolunteerResponse createVolunteer(Long userId, VolunteerRequest request);

    /**
     * 更新志愿服务记录
     */
    VolunteerResponse updateVolunteer(Long id, Long userId, VolunteerRequest request);

    /**
     * 取消志愿服务申请
     */
    void cancelVolunteer(Long id, Long userId);

    /**
     * 审核志愿服务
     */
    VolunteerResponse reviewVolunteer(Long id, Long reviewerId, Boolean approved, String remark);

    /**
     * 获取待审核志愿服务列表
     */
    PageResult<VolunteerResponse> getPendingVolunteers(Long current, Long size);

    /**
     * 删除志愿服务记录
     */
    void deleteVolunteer(Long id);

    /**
     * 获取用户志愿服务时长统计
     */
    VolunteerStatsDto getVolunteerStats(Long userId);
}
