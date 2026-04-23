package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.dto.AnnouncementRequest;
import com.library.system.dto.AnnouncementResponse;
import com.library.system.dto.PageResult;
import com.library.system.entity.Announcement;
import com.library.system.entity.User;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.AnnouncementMapper;
import com.library.system.mapper.UserMapper;
import com.library.system.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 公告服务实现类 
 * <p>
 * 实现公告的CRUD操作和查询功能，使用缓存优化查询性能。
 * </p>
 *
 * @author Library Team
 * @version 2.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "announcements")
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementMapper announcementMapper;
    private final UserMapper userMapper;

    @Override
    public PageResult<AnnouncementResponse> listAnnouncements(Long current, Long size, String keyword, String status) {
        // 构建查询条件
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Announcement::getDeleted, 0);

        // 状态筛选（默认为已发布）
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Announcement::getStatus, status);
        } else {
            wrapper.eq(Announcement::getStatus, "PUBLISHED");
        }

        // 关键词搜索
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Announcement::getTitle, keyword)
                    .or()
                    .like(Announcement::getContent, keyword));
        }

        // 按优先级和发布时间降序排序
        wrapper.orderByDesc(Announcement::getPriority)
               .orderByDesc(Announcement::getPublishTime);

        // 分页查询
        Page<Announcement> page = new Page<>(current, size);
        Page<Announcement> resultPage = announcementMapper.selectPage(page, wrapper);

        List<Announcement> records = resultPage.getRecords();
        if (!records.isEmpty()) {
            Set<Long> publisherIds = records.stream()
                    .map(Announcement::getPublisherId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());
            
            Map<Long, User> publisherMap = publisherIds.isEmpty() ?
                Map.of() :
                userMapper.selectBatchIds(publisherIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
            
            List<AnnouncementResponse> responseList = records.stream()
                    .map(a -> convertToResponseWithPublisher(a, publisherMap))
                    .collect(Collectors.toList());
            
            return PageResult.of(resultPage.getCurrent(), resultPage.getSize(),
                    resultPage.getTotal(), responseList);
        }

        return PageResult.of(resultPage.getCurrent(), resultPage.getSize(),
                resultPage.getTotal(), List.of());
    }

    @Override
    @Cacheable(key = "#id", unless = "#result == null")
    public AnnouncementResponse getAnnouncementById(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null || announcement.getDeleted() == 1) {
            
            throw new ResourceNotFoundException(ErrorCode.ANNOUNCEMENT_NOT_FOUND, "公告不存在");
        }
        return convertToResponse(announcement);
    }

    @Override
    public List<AnnouncementResponse> getLatestAnnouncements(Integer limit) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Announcement::getDeleted, 0)
               .eq(Announcement::getStatus, "PUBLISHED")
               .orderByDesc(Announcement::getPriority)
               .orderByDesc(Announcement::getPublishTime)
               .last("LIMIT " + limit);

        List<Announcement> announcements = announcementMapper.selectList(wrapper);

        if (announcements.isEmpty()) {
            return List.of();
        }
        
        Set<Long> publisherIds = announcements.stream()
                .map(Announcement::getPublisherId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        
        Map<Long, User> publisherMap = publisherIds.isEmpty() ?
            Map.of() :
            userMapper.selectBatchIds(publisherIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        
        return announcements.stream()
                .map(a -> convertToResponseWithPublisher(a, publisherMap))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(allEntries = true)
    public AnnouncementResponse createAnnouncement(AnnouncementRequest request, Long publisherId) {
        Announcement announcement = new Announcement();
        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        announcement.setType(request.getType() != null ? request.getType() : "NOTICE");
        announcement.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        announcement.setPublisherId(publisherId);
        announcement.setStatus("DRAFT");

        announcementMapper.insert(announcement);

        log.info("公告创建成功: {}", announcement.getTitle());
        return convertToResponse(announcement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(allEntries = true)
    public AnnouncementResponse updateAnnouncement(Long id, AnnouncementRequest request) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null || announcement.getDeleted() == 1) {
            
            throw new ResourceNotFoundException(ErrorCode.ANNOUNCEMENT_NOT_FOUND, "公告不存在");
        }

        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        if (request.getType() != null) {
            announcement.setType(request.getType());
        }
        if (request.getPriority() != null) {
            announcement.setPriority(request.getPriority());
        }
        if (request.getStatus() != null) {
            announcement.setStatus(request.getStatus());
            // 发布时设置发布时间
            if ("PUBLISHED".equals(request.getStatus()) && announcement.getPublishTime() == null) {
                announcement.setPublishTime(LocalDateTime.now());
            }
        }

        announcementMapper.updateById(announcement);

        log.info("公告更新成功: id={}", id);
        return convertToResponse(announcement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(allEntries = true)
    public void publishAnnouncement(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null || announcement.getDeleted() == 1) {
            
            throw new ResourceNotFoundException(ErrorCode.ANNOUNCEMENT_NOT_FOUND, "公告不存在");
        }

        announcement.setStatus("PUBLISHED");
        announcement.setPublishTime(LocalDateTime.now());
        announcementMapper.updateById(announcement);

        log.info("公告发布成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(allEntries = true)
    public void deleteAnnouncement(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null || announcement.getDeleted() == 1) {
            
            throw new ResourceNotFoundException(ErrorCode.ANNOUNCEMENT_NOT_FOUND, "公告不存在");
        }

        announcementMapper.deleteById(id);

        log.info("公告删除成功: id={}", id);
    }

    /**
     * 将Announcement实体转换为AnnouncementResponse DTO（单条查询）
     */
    private AnnouncementResponse convertToResponse(Announcement announcement) {
        AnnouncementResponse.AnnouncementResponseBuilder builder = AnnouncementResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .content(announcement.getContent())
                .type(announcement.getType())
                .priority(announcement.getPriority())
                .publisherId(announcement.getPublisherId())
                .status(announcement.getStatus())
                .publishTime(announcement.getPublishTime())
                .createTime(announcement.getCreateTime());

        // 获取发布人姓名
        if (announcement.getPublisherId() != null) {
            User publisher = userMapper.selectById(announcement.getPublisherId());
            if (publisher != null) {
                builder.publisherName(publisher.getRealName() != null ?
                        publisher.getRealName() : publisher.getUsername());
            }
        }

        return builder.build();
    }

    /**
     * 使用预加载的发布人Map转换为AnnouncementResponse DTO（批量查询优化）
     * FIXED: PERF-008 避免在循环中执行N+1查询
     */
    private AnnouncementResponse convertToResponseWithPublisher(Announcement announcement, Map<Long, User> publisherMap) {
        AnnouncementResponse.AnnouncementResponseBuilder builder = AnnouncementResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .content(announcement.getContent())
                .type(announcement.getType())
                .priority(announcement.getPriority())
                .publisherId(announcement.getPublisherId())
                .status(announcement.getStatus())
                .publishTime(announcement.getPublishTime())
                .createTime(announcement.getCreateTime());

        // 从预加载的Map中获取发布人信息
        if (announcement.getPublisherId() != null) {
            User publisher = publisherMap.get(announcement.getPublisherId());
            if (publisher != null) {
                builder.publisherName(publisher.getRealName() != null ?
                        publisher.getRealName() : publisher.getUsername());
            }
        }

        return builder.build();
    }
}
