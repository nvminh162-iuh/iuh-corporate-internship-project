package com.hs.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.hs.user.advice.base.AppException;
import com.hs.user.constant.base.ErrorCode;
import com.hs.user.dto.base.PageResponse;
import com.hs.user.dto.response.NotificationResponse;
import com.hs.user.dto.response.NotificationUnreadCountResponse;
import com.hs.user.model.Notification;
import com.hs.user.model.constant.NotificationType;
import com.hs.user.repository.NotificationRepository;
import com.hs.user.service.impl.NotificationServiceImpl;
import com.hs.user.utils.CurrentUserUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private CurrentUserUtils currentUserUtils;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    @DisplayName("createNotification saves notification when sourceHistoryId is new")
    void createNotification_NewEvent_SavesNotification() {
        when(notificationRepository.existsBySourceHistoryId("hist-1")).thenReturn(false);

        notificationService.createNotification(
                "cust-1",
                NotificationType.SUPPORT_REQUEST_RECEIVED,
                "Tiếp nhận yêu cầu",
                "Yêu cầu đã được tiếp nhận",
                "SUPPORT_REQUEST",
                "sr-1",
                "hist-1"
        );

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("createNotification does not save when sourceHistoryId exists")
    void createNotification_DuplicateEvent_DoesNotSave() {
        when(notificationRepository.existsBySourceHistoryId("hist-1")).thenReturn(true);

        notificationService.createNotification(
                "cust-1",
                NotificationType.SUPPORT_REQUEST_RECEIVED,
                "Tiếp nhận yêu cầu",
                "Yêu cầu đã được tiếp nhận",
                "SUPPORT_REQUEST",
                "sr-1",
                "hist-1"
        );

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("markAsRead updates read and readAt when notification is found for current user")
    void markAsRead_Found_UpdatesRead() {
        Notification notification = Notification.builder()
                .recipientId("cust-1")
                .read(false)
                .build();
        notification.setId("notif-1");

        when(currentUserUtils.getCurrentUserId()).thenReturn("cust-1");
        when(notificationRepository.findByIdAndRecipientId("notif-1", "cust-1"))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationResponse res = notificationService.markAsRead("notif-1");

        assertThat(res.read()).isTrue();
        assertThat(notification.getRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("markAsRead throws NOTIFICATION_NOT_EXISTED when recipient does not match")
    void markAsRead_WrongRecipient_ThrowsException() {
        when(currentUserUtils.getCurrentUserId()).thenReturn("cust-1");
        when(notificationRepository.findByIdAndRecipientId("notif-999", "cust-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead("notif-999"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_NOT_EXISTED);
    }

    @Test
    @DisplayName("getUnreadCount returns correct count from repository")
    void getUnreadCount_ReturnsCount() {
        when(currentUserUtils.getCurrentUserId()).thenReturn("cust-1");
        when(notificationRepository.countByRecipientIdAndReadFalse("cust-1")).thenReturn(5L);

        NotificationUnreadCountResponse res = notificationService.getUnreadCount();

        assertThat(res.unreadCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getMyNotifications returns mapped PageResponse")
    void getMyNotifications_ReturnsPage() {
        Notification notification = Notification.builder()
                .recipientId("cust-1")
                .type(NotificationType.SUPPORT_REQUEST_STATUS_CHANGED)
                .title("Thông báo cập nhật")
                .message("Trạng thái mới: COMPLETED")
                .referenceType("SUPPORT_REQUEST")
                .referenceId("sr-1")
                .read(false)
                .build();
        notification.setId("notif-1");
        notification.setCreatedAt(Instant.now());

        Page<Notification> entityPage = new PageImpl<>(List.of(notification), PageRequest.of(0, 10), 1);
        when(currentUserUtils.getCurrentUserId()).thenReturn("cust-1");
        when(notificationRepository.findByRecipientId(any(), any()))
                .thenReturn(entityPage);

        PageResponse<NotificationResponse> result = notificationService.getMyNotifications(false, PageRequest.of(0, 10));

        assertThat(result.getResult()).hasSize(1);
        assertThat(result.getResult().get(0).id()).isEqualTo("notif-1");
        assertThat(result.getResult().get(0).title()).isEqualTo("Thông báo cập nhật");
    }
}
