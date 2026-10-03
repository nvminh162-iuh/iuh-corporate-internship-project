export type NotificationType =
  | "SUPPORT_REQUEST_RECEIVED"
  | "SUPPORT_REQUEST_STATUS_CHANGED";

export interface NotificationItem {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  referenceType?: string;
  referenceId?: string;
  read: boolean;
  readAt?: string;
  createdAt: string;
}

export interface NotificationUnreadCount {
  unreadCount: number;
}
