"use client";

import { useState, useRef, useEffect } from "react";
import Link from "next/link";
import { useAuth } from "@/features/auth/useAuth";
import {
  LogOut,
  ChevronDown,
  ChevronUp,
  ChevronRight,
  Settings,
} from "lucide-react";

export default function UserDropdown() {
  const { username, avatarUrl, logout } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Close dropdown when clicking outside
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
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isOpen]);

  const displayName = username || "Người dùng";
  const initialLetter = displayName.charAt(0).toUpperCase() || "U";

  return (
    <div className="relative" ref={dropdownRef}>
      {/* Trigger Button */}
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className={`h-10 flex items-center gap-2 pl-1 pr-3.5 rounded-full border transition-all cursor-pointer ${
          isOpen
            ? "border-primary/50 bg-muted shadow-sm"
            : "border-border bg-card text-foreground hover:border-primary/30 hover:bg-muted"
        }`}
      >
        {/* Avatar with status indicator */}
        <div className="relative">
          {avatarUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={avatarUrl}
              alt={displayName}
              className="w-8 h-8 rounded-full object-cover"
            />
          ) : (
            <div className="w-8 h-8 rounded-full bg-primary text-primary-foreground font-bold flex items-center justify-center text-sm shadow-xs">
              {initialLetter}
            </div>
          )}
          {/* Active online green dot */}
          <span className="absolute bottom-0 right-0 w-2.5 h-2.5 bg-emerald-500 border-2 border-card rounded-full" />
        </div>

        {/* Username */}
        <span className="text-sm font-semibold text-foreground max-w-[120px] truncate">
          {displayName}
        </span>

        {/* Chevron Icon */}
        {isOpen ? (
          <ChevronUp className="w-4 h-4 text-muted-foreground" />
        ) : (
          <ChevronDown className="w-4 h-4 text-muted-foreground" />
        )}
      </button>

      {/* Dropdown Menu Panel */}
      {isOpen && (
        <div className="absolute right-0 mt-2.5 w-64 bg-popover text-popover-foreground rounded-2xl shadow-2xl border border-border p-2 z-50 animate-in fade-in-50 zoom-in-95 duration-150">
          <nav className="space-y-1">
            {/* Cài đặt */}
            <Link
              href="/settings"
              onClick={() => setIsOpen(false)}
              className="flex items-center justify-between px-3 py-2.5 rounded-xl text-foreground hover:bg-muted transition-colors group"
            >
              <div className="flex items-center gap-3">
                <Settings className="w-4 h-4 text-muted-foreground group-hover:text-primary transition-colors" />
                <span className="text-xs font-semibold">Cài đặt tài khoản</span>
              </div>
              <ChevronRight className="w-3.5 h-3.5 text-muted-foreground/50 group-hover:text-primary group-hover:translate-x-0.5 transition-all" />
            </Link>

            {/* Divider */}
            <div className="my-1 border-t border-border" />

            {/* Đăng xuất */}
            <button
              type="button"
              onClick={() => {
                setIsOpen(false);
                logout();
              }}
              className="w-full flex items-center justify-between px-3 py-2.5 rounded-xl text-foreground hover:bg-rose-50 dark:hover:bg-rose-950/30 hover:text-rose-600 dark:hover:text-rose-400 transition-colors group text-left cursor-pointer"
            >
              <div className="flex items-center gap-3">
                <LogOut className="w-4 h-4 text-muted-foreground group-hover:text-rose-600 transition-colors" />
                <span className="text-xs font-semibold">Đăng xuất</span>
              </div>
              <ChevronRight className="w-3.5 h-3.5 text-muted-foreground/50 group-hover:text-rose-600 group-hover:translate-x-0.5 transition-all" />
            </button>
          </nav>
        </div>
      )}
    </div>
  );
}
