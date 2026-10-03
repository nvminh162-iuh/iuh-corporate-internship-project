package com.hs.user.service.impl;

import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hs.user.advice.base.AppException;
import com.hs.user.constant.base.ErrorCode;
import com.hs.user.dto.base.PageResponse;
import com.hs.user.dto.response.NotificationResponse;
import com.hs.user.dto.response.NotificationUnreadCountResponse;
import com.hs.user.model.Notification;
import com.hs.user.model.constant.NotificationType;
import com.hs.user.repository.NotificationRepository;
import com.hs.user.service.NotificationService;
import com.hs.user.utils.CurrentUserUtils;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationServiceImpl implements NotificationService {

    NotificationRepository notificationRepository;
    CurrentUserUtils currentUserUtils;

    private static final int MAX_PAGE_SIZE = 100;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(Boolean unreadOnly, Pageable pageable) {
        String currentUserId = currentUserUtils.getCurrentUserId();

        int pageSize = Math.min(Math.max(1, pageable.getPageSize()), MAX_PAGE_SIZE);
        int pageNumber = Math.max(0, pageable.getPageNumber());
        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        Pageable safePageable = PageRequest.of(pageNumber, pageSize, sort);

        Page<Notification> page;
        if (Boolean.TRUE.equals(unreadOnly)) {
            page = notificationRepository.findByRecipientIdAndRead(currentUserId, false, safePageable);
        } else {
            page = notificationRepository.findByRecipientId(currentUserId, safePageable);
        }

        Page<NotificationResponse> responsePage = page.map(this::toResponse);
        return new PageResponse<>(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationUnreadCountResponse getUnreadCount() {
        String currentUserId = currentUserUtils.getCurrentUserId();
        long unreadCount = notificationRepository.countByRecipientIdAndReadFalse(currentUserId);
        return NotificationUnreadCountResponse.builder()
                .unreadCount(unreadCount)
                .build();
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(String id) {
        String currentUserId = currentUserUtils.getCurrentUserId();
        Notification notification = notificationRepository.findByIdAndRecipientId(id, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_EXISTED));

        if (!Boolean.TRUE.equals(notification.getRead())) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notification = notificationRepository.save(notification);
        }

        return toResponse(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        String currentUserId = currentUserUtils.getCurrentUserId();
        notificationRepository.markAllAsRead(currentUserId, Instant.now());
    }

    @Override
    @Transactional
    public void createNotification(
            String recipientId,
            NotificationType type,
            String title,
            String message,
            String referenceType,
            String referenceId,
            String sourceHistoryId
    ) {
        if (recipientId == null || recipientId.isBlank()) {
            log.warn("Cannot create notification without recipientId");
            return;
        }

        if (sourceHistoryId != null && !sourceHistoryId.isBlank()
                && notificationRepository.existsBySourceHistoryId(sourceHistoryId)) {
            log.info("Notification for sourceHistoryId {} already exists, skipping duplicate", sourceHistoryId);
            return;
        }

        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .type(type)
                .title(title)
                .message(message)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .sourceHistoryId(sourceHistoryId)
                .read(false)
                .build();

        try {
            notificationRepository.save(notification);
            log.info("Created notification [{}] for recipient {}", type, recipientId);
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent duplicate notification for sourceHistoryId {}: {}", sourceHistoryId, e.getMessage());
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .read(notification.getRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
