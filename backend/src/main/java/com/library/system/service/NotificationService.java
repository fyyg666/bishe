package com.library.system.service;

import com.library.system.dto.NotificationResponse;
import com.library.system.dto.PageResult;

public interface NotificationService {

    PageResult<NotificationResponse> listNotifications(Long userId, Long current, Long size, String type, String status);

    Long getUnreadCount(Long userId);

    void markAsRead(Long userId, Long notificationId);

    void markAllAsRead(Long userId);

    void deleteNotification(Long userId, Long notificationId);

    NotificationResponse createNotification(Long userId, String title, String content, String type, Long bizId);
}
