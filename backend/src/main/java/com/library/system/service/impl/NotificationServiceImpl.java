package com.library.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.system.dto.NotificationResponse;
import com.library.system.dto.PageResult;
import com.library.system.entity.Notification;
import com.library.system.enums.ErrorCode;
import com.library.system.exception.ForbiddenException;
import com.library.system.exception.ResourceNotFoundException;
import com.library.system.mapper.NotificationMapper;
import com.library.system.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public PageResult<NotificationResponse> listNotifications(Long userId, Long current, Long size, String type, String status) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Notification::getType, type);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Notification::getStatus, status);
        }
        wrapper.orderByDesc(Notification::getCreateTime);

        Page<Notification> page = new Page<>(current, size);
        Page<Notification> result = notificationMapper.selectPage(page, wrapper);

        List<NotificationResponse> records = result.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    public Long getUnreadCount(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getStatus, "UNREAD");
        return notificationMapper.selectCount(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || notification.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "通知不存在");
        }
        if (!notification.getUserId().equals(userId)) {
            throw new ForbiddenException(ErrorCode.INSUFFICIENT_PERMISSION, "无权操作此通知");
        }
        if ("READ".equals(notification.getStatus())) {
            return;
        }
        notification.setStatus("READ");
        notification.setReadAt(LocalDateTime.now());
        notificationMapper.updateById(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getStatus, "UNREAD")
                .set(Notification::getStatus, "READ")
                .set(Notification::getReadAt, LocalDateTime.now());
        notificationMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || notification.getDeleted() == 1) {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "通知不存在");
        }
        if (!notification.getUserId().equals(userId)) {
            throw new ForbiddenException(ErrorCode.INSUFFICIENT_PERMISSION, "无权操作此通知");
        }
        notificationMapper.deleteById(notificationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationResponse createNotification(Long userId, String title, String content, String type, Long bizId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type != null ? type : "SYSTEM");
        notification.setStatus("UNREAD");
        notification.setBizId(bizId);
        notificationMapper.insert(notification);

        log.debug("创建通知: userId={}, type={}, title={}", userId, type, title);
        return convertToResponse(notification);
    }

    private NotificationResponse convertToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getType())
                .status(notification.getStatus())
                .readAt(notification.getReadAt())
                .bizId(notification.getBizId())
                .createTime(notification.getCreateTime())
                .build();
    }
}
