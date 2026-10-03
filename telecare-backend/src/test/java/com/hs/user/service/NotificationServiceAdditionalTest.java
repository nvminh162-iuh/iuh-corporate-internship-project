package com.hs.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hs.user.advice.base.AppException;
import com.hs.user.constant.base.ErrorCode;
import com.hs.user.dto.response.NotificationResponse;
import com.hs.user.model.Notification;
import com.hs.user.model.constant.NotificationType;
import com.hs.user.repository.NotificationRepository;
import com.hs.user.service.impl.NotificationServiceImpl;
import com.hs.user.utils.CurrentUserUtils;

/**
 * NOTIF-005, NOTIF-008, NOTIF-009 - Test cases missing from previous NotificationServiceTest
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService - Additional Tests (NOTIF-005, 008, 009)")
class NotificationServiceAdditionalTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private CurrentUserUtils currentUserUtils;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    // ==========================================
    // NOTIF-005: markAsRead idempotent
    // ==========================================
    @Test
    @DisplayName("NOTIF-005: markAsRead on already-read notification is idempotent - does not call save again")
    void markAsRead_AlreadyRead_IdempotentDoesNotSave() {
        Notification notification = Notification.builder()
                .recipientId("cust-1")
                .read(true) // already read
                .build();
        notification.setId("notif-1");
        notification.setReadAt(Instant.now().minusSeconds(60));

        when(currentUserUtils.getCurrentUserId()).thenReturn("cust-1");
        when(notificationRepository.findByIdAndRecipientId("notif-1", "cust-1"))
                .thenReturn(java.util.Optional.of(notification));

        NotificationResponse res = notificationService.markAsRead("notif-1");

        // Should return without calling save again because already read
        verify(notificationRepository, never()).save(any());
        assertThat(res.read()).isTrue();
    }

    // ==========================================
    // NOTIF-008: markAllAsRead acts on current user only
    // ==========================================
    @Test
    @DisplayName("NOTIF-008: markAllAsRead calls repository with current user ID and timestamp")
    void markAllAsRead_OnlyCurrentUser() {
        when(currentUserUtils.getCurrentUserId()).thenReturn("cust-1");

        notificationService.markAllAsRead();

        // Verify markAllAsRead was called with the correct userId
        verify(notificationRepository).markAllAsRead(eq("cust-1"), any(Instant.class));
    }

    // ==========================================
    // NOTIF-009: recipientId null -> no save
    // ==========================================
    @Test
    @DisplayName("NOTIF-009: createNotification with null recipientId skips save and logs warning")
    void createNotification_NullRecipient_SkipsSave() {
        notificationService.createNotification(
                null, // null recipientId
                NotificationType.SUPPORT_REQUEST_RECEIVED,
                "Test title",
                "Test message",
                "SUPPORT_REQUEST",
                "sr-1",
                "hist-1"
        );

        verify(notificationRepository, never()).existsBySourceHistoryId(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("NOTIF-009b: createNotification with blank recipientId skips save")
    void createNotification_BlankRecipient_SkipsSave() {
        notificationService.createNotification(
                "  ", // blank recipientId
                NotificationType.SUPPORT_REQUEST_RECEIVED,
                "Test title",
                "Test message",
                "SUPPORT_REQUEST",
                "sr-1",
                "hist-1"
        );

        verify(notificationRepository, never()).save(any());
    }

    // ==========================================
    // NOTIF additional: getMyNotifications with unreadOnly=true
    // ==========================================
    @Test
    @DisplayName("getMyNotifications with unreadOnly=true uses findByRecipientIdAndRead repository method")
    void getMyNotifications_UnreadOnly_UsesUnreadRepository() {
        when(currentUserUtils.getCurrentUserId()).thenReturn("cust-1");
        org.springframework.data.domain.Page<Notification> emptyPage =
                new org.springframework.data.domain.PageImpl<>(java.util.List.of(),
                        org.springframework.data.domain.PageRequest.of(0, 10), 0);
        when(notificationRepository.findByRecipientIdAndRead(eq("cust-1"), eq(false), any()))
                .thenReturn(emptyPage);

        var result = notificationService.getMyNotifications(true,
                org.springframework.data.domain.PageRequest.of(0, 10));

        verify(notificationRepository).findByRecipientIdAndRead(eq("cust-1"), eq(false), any());
        verify(notificationRepository, never()).findByRecipientId(any(), any());
        assertThat(result.getResult()).isEmpty();
    }
}
