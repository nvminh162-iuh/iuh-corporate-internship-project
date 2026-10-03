package com.hs.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hs.user.advice.base.AppException;
import com.hs.user.advice.exception.GlobalException;
import com.hs.user.constant.base.ErrorCode;
import com.hs.user.dto.base.PageResponse;
import com.hs.user.dto.response.NotificationResponse;
import com.hs.user.dto.response.NotificationUnreadCountResponse;
import com.hs.user.model.constant.NotificationType;
import com.hs.user.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalException())
                .build();
    }

    @Test
    @DisplayName("GET /notifications returns 200 OK with notifications")
    void getMyNotifications_ReturnsOk() throws Exception {
        NotificationResponse item = NotificationResponse.builder()
                .id("notif-1")
                .type(NotificationType.SUPPORT_REQUEST_STATUS_CHANGED)
                .title("Cập nhật trạng thái yêu cầu")
                .message("Yêu cầu SR-001 đang được xử lý")
                .referenceType("SUPPORT_REQUEST")
                .referenceId("sr-1")
                .read(false)
                .createdAt(Instant.now())
                .build();

        PageResponse<NotificationResponse> page = new PageResponse<>(
                new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1)
        );

        when(notificationService.getMyNotifications(eq(false), any()))
                .thenReturn(page);

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.result[0].id").value("notif-1"))
                .andExpect(jsonPath("$.result.result[0].type").value("SUPPORT_REQUEST_STATUS_CHANGED"))
                .andExpect(jsonPath("$.result.result[0].read").value(false));
    }

    @Test
    @DisplayName("GET /notifications/unread-count returns 200 OK with unread count")
    void getUnreadCount_ReturnsOk() throws Exception {
        NotificationUnreadCountResponse response = NotificationUnreadCountResponse.builder()
                .unreadCount(3L)
                .build();

        when(notificationService.getUnreadCount())
                .thenReturn(response);

        mockMvc.perform(get("/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.unreadCount").value(3));
    }

    @Test
    @DisplayName("PATCH /notifications/{id}/read returns 200 OK when owned")
    void markAsRead_Owned_ReturnsOk() throws Exception {
        NotificationResponse item = NotificationResponse.builder()
                .id("notif-1")
                .read(true)
                .build();

        when(notificationService.markAsRead("notif-1")).thenReturn(item);

        mockMvc.perform(patch("/notifications/notif-1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.read").value(true));

        verify(notificationService).markAsRead("notif-1");
    }

    @Test
    @DisplayName("PATCH /notifications/{id}/read returns 404 NOT_FOUND for anti-enumeration when notification belongs to another user")
    void markAsRead_OtherUser_ReturnsNotFound() throws Exception {
        when(notificationService.markAsRead("notif-999"))
                .thenThrow(new AppException(ErrorCode.NOTIFICATION_NOT_EXISTED));

        mockMvc.perform(patch("/notifications/notif-999/read"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1701));
    }

    @Test
    @DisplayName("PATCH /notifications/read-all returns 200 OK")
    void markAllAsRead_ReturnsOk() throws Exception {
        mockMvc.perform(patch("/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All notifications marked as read"));

        verify(notificationService).markAllAsRead();
    }
}
