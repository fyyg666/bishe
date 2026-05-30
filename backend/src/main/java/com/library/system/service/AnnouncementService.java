package com.library.system.service;

import com.library.system.dto.AnnouncementRequest;
import com.library.system.dto.AnnouncementResponse;
import com.library.system.dto.PageResult;

import java.util.List;

/**
 * 公告服务接口 
 * <p>
 * 提供公告的CRUD操作和查询功能，包括分页查询、最新公告获取等。
 * 将业务逻辑从Controller层剥离，遵循分层架构原则。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
public interface AnnouncementService {

    /**
     * 分页查询公告列表
     *
     * @param current 当前页
     * @param size    每页大小
     * @param keyword 关键词（标题/内容）
     * @param status  状态
     * @return 分页结果
     */
    PageResult<AnnouncementResponse> listAnnouncements(Long current, Long size, String keyword, String status);

    /**
     * 获取公告详情
     *
     * @param id 公告ID
     * @return 公告详情
     */
    AnnouncementResponse getAnnouncementById(Long id);

    /**
     * 获取最新公告列表
     *
     * @param limit 数量限制
     * @return 最新公告列表
     */
    List<AnnouncementResponse> getLatestAnnouncements(Integer limit);

    /**
     * 新增公告
     *
     * @param request  公告请求
     * @param username 发布人用户名
     * @return 新增的公告
     */
    AnnouncementResponse createAnnouncement(AnnouncementRequest request, String username);

    /**
     * 更新公告
     *
     * @param id      公告ID
     * @param request 公告请求
     * @return 更新后的公告
     */
    AnnouncementResponse updateAnnouncement(Long id, AnnouncementRequest request);

    /**
     * 发布公告
     *
     * @param id 公告ID
     */
    void publishAnnouncement(Long id);

    /**
     * 删除公告
     *
     * @param id 公告ID
     */
    void deleteAnnouncement(Long id);
}
