"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Bell, ExternalLink, Ticket, CheckCheck } from "lucide-react";
import { notificationService } from "@/services/notification.service";
import type { NotificationItem } from "@/types/notification.type";
import { useAuth } from "@/features/auth/useAuth";

export default function NotificationDropdown() {
  const { authenticated } = useAuth();
  const router = useRouter();

  const [isOpen, setIsOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Fetch unread count
  const fetchUnreadCount = useCallback(async () => {
    if (!authenticated) return;
    try {
      const data = await notificationService.getUnreadCount();
      setUnreadCount(data.unreadCount || 0);
    } catch {
      // Ignore background polling errors silently
    }
  }, [authenticated]);

  // Fetch latest notifications list
  const fetchNotifications = useCallback(async () => {
    if (!authenticated) return;
    setLoading(true);
    try {
      const res = await notificationService.getMyNotifications(false, 0, 8);
      setNotifications(res?.result || []);
    } catch {
      // Fallback empty on error
      setNotifications([]);
    } finally {
      setLoading(false);
    }
  }, [authenticated]);

  // Polling every 35 seconds
  useEffect(() => {
    if (!authenticated) return;

    const timer = setTimeout(() => {
      fetchUnreadCount();
    }, 0);
    const interval = setInterval(fetchUnreadCount, 35000);
    return () => {
      clearTimeout(timer);
      clearInterval(interval);
    };
  }, [authenticated, fetchUnreadCount]);

  // Close when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    }
    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [isOpen]);

  const handleNotificationClick = async (item: NotificationItem) => {
    if (!item.read) {
      try {
        await notificationService.markAsRead(item.id);
        setUnreadCount((prev) => Math.max(0, prev - 1));
        setNotifications((prev) =>
          prev.map((n) => (n.id === item.id ? { ...n, read: true } : n))
        );
      } catch {
        // Continue navigation even if markAsRead fails
      }
    }

    setIsOpen(false);
    if (item.referenceType === "SUPPORT_REQUEST" && item.referenceId) {
      router.push(`/support/requests/${item.referenceId}`);
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await notificationService.markAllAsRead();
      setUnreadCount(0);
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
    } catch {
      // Silent error fallback
    }
  };

  const formatDate = (isoString?: string) => {
    if (!isoString) return "";
    try {
      const d = new Date(isoString);
      return d.toLocaleDateString("vi-VN", {
        hour: "2-digit",
        minute: "2-digit",
        day: "2-digit",
        month: "2-digit",
      });
    } catch {
      return isoString;
    }
  };

  if (!authenticated) return null;

  return (
    <div className="relative" ref={dropdownRef}>
      {/* Bell Trigger Button */}
      <button
        type="button"
        onClick={() => {
          const nextState = !isOpen;
          setIsOpen(nextState);
          if (nextState) {
            fetchNotifications();
          }
        }}
        className="relative w-10 h-10 rounded-full border border-border bg-card hover:bg-muted text-muted-foreground hover:text-foreground flex items-center justify-center transition-colors cursor-pointer shadow-2xs"
        title="Thông báo"
      >
        <Bell className="w-4 h-4" />
        {unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 min-w-4 h-4 px-1 rounded-full bg-destructive text-destructive-foreground text-[10px] font-bold flex items-center justify-center leading-none shadow-xs animate-pulse">
            {unreadCount > 99 ? "99+" : unreadCount}
          </span>
        )}
      </button>

      {/* Dropdown Panel */}
      {isOpen && (
        <div className="absolute right-0 mt-2.5 w-80 sm:w-96 bg-popover text-popover-foreground rounded-2xl shadow-2xl border border-border p-3 z-50 animate-in fade-in-50 zoom-in-95 duration-150">
          {/* Header */}
          <div className="flex items-center justify-between pb-2 mb-2 border-b border-border px-1">
            <div className="flex items-center gap-2">
              <span className="text-sm font-bold text-foreground">Thông báo</span>
              {unreadCount > 0 && (
                <span className="text-xs px-2 py-0.5 rounded-full bg-primary/10 text-primary font-medium">
                  {unreadCount} mới
                </span>
              )}
            </div>

            {unreadCount > 0 && (
              <button
                type="button"
                onClick={handleMarkAllRead}
                className="text-xs text-muted-foreground hover:text-primary transition-colors flex items-center gap-1 cursor-pointer font-medium"
              >
                <CheckCheck className="w-3.5 h-3.5" />
                <span>Đọc tất cả</span>
              </button>
            )}
          </div>

          {/* List Content */}
          <div className="max-h-[360px] overflow-y-auto space-y-1.5 pr-0.5">
            {loading ? (
              <div className="py-8 text-center text-xs text-muted-foreground space-y-2">
                <div className="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin mx-auto" />
                <p>Đang tải thông báo...</p>
              </div>
            ) : notifications.length === 0 ? (
              <div className="py-8 text-center text-xs text-muted-foreground">
                <Bell className="w-8 h-8 text-muted-foreground/30 mx-auto mb-2" />
                <p>Bạn chưa có thông báo nào</p>
              </div>
            ) : (
              notifications.map((item) => (
                <div
                  key={item.id}
                  onClick={() => handleNotificationClick(item)}
                  className={`p-2.5 rounded-xl cursor-pointer transition-all flex items-start gap-2.5 ${
                    item.read
                      ? "hover:bg-muted/60 opacity-80"
                      : "bg-primary/5 hover:bg-primary/10 border-l-2 border-primary"
                  }`}
                >
                  <div className="w-7 h-7 rounded-lg bg-primary/10 text-primary flex items-center justify-center shrink-0 mt-0.5">
                    <Ticket className="w-3.5 h-3.5" />
                  </div>

                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between gap-1">
                      <p className={`text-xs font-semibold truncate ${item.read ? "text-foreground" : "text-primary"}`}>
                        {item.title}
                      </p>
                      {!item.read && (
                        <span className="w-2 h-2 rounded-full bg-primary shrink-0" />
                      )}
                    </div>
                    <p className="text-[11px] text-muted-foreground line-clamp-2 mt-0.5 leading-snug">
                      {item.message}
                    </p>
                    <span className="text-[10px] text-muted-foreground/70 mt-1 block">
                      {formatDate(item.createdAt)}
                    </span>
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Footer Link */}
          <div className="pt-2 mt-2 border-t border-border">
            <Link
              href="/support/requests"
              onClick={() => setIsOpen(false)}
              className="w-full py-1.5 text-center text-xs font-semibold text-primary hover:underline flex items-center justify-center gap-1.5 transition-colors"
            >
              <span>Xem danh sách yêu cầu của tôi</span>
              <ExternalLink className="w-3 h-3" />
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}
