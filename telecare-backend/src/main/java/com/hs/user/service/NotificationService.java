package com.hs.user.service;

import org.springframework.data.domain.Pageable;

import com.hs.user.dto.base.PageResponse;
import com.hs.user.dto.response.NotificationResponse;
import com.hs.user.dto.response.NotificationUnreadCountResponse;
import com.hs.user.model.constant.NotificationType;

public interface NotificationService {

    PageResponse<NotificationResponse> getMyNotifications(Boolean unreadOnly, Pageable pageable);

    NotificationUnreadCountResponse getUnreadCount();

    NotificationResponse markAsRead(String id);

    void markAllAsRead();

    void createNotification(
            String recipientId,
            NotificationType type,
            String title,
            String message,
            String referenceType,
            String referenceId,
            String sourceHistoryId
    );
}
