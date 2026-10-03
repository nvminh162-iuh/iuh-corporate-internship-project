import axiosClient from "@/lib/axios-client";
import type { ApiResponse, PageResponse } from "@/types/plan.type";
import type {
  NotificationItem,
  NotificationUnreadCount,
} from "@/types/notification.type";

export const notificationService = {
  async getMyNotifications(
    unreadOnly = false,
    page = 0,
    size = 10
  ): Promise<PageResponse<NotificationItem>> {
    const response = await axiosClient.get<ApiResponse<PageResponse<NotificationItem>>>(
      `/api/v1/notifications?unreadOnly=${unreadOnly}&page=${page}&size=${size}`
    );
    return response.data.result;
  },

  async getUnreadCount(): Promise<NotificationUnreadCount> {
    const response = await axiosClient.get<ApiResponse<NotificationUnreadCount>>(
      "/api/v1/notifications/unread-count"
    );
    return response.data.result;
  },

  async markAsRead(id: string): Promise<NotificationItem> {
    const response = await axiosClient.patch<ApiResponse<NotificationItem>>(
      `/api/v1/notifications/${id}/read`
    );
    return response.data.result;
  },

  async markAllAsRead(): Promise<void> {
    await axiosClient.patch<ApiResponse<void>>("/api/v1/notifications/read-all");
  },
};
