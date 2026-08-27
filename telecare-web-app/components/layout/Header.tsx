"use client";

import { useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { useAuth } from "@/features/auth/useAuth";
import { Button } from "@/components/ui/button";
import UserDropdown from "./UserDropdown";
import { Menu, X, Smartphone } from "lucide-react";

export default function Header() {
  const { authenticated, login, register, logout, username, avatarUrl } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  // Silky smooth animated glide to top (550ms easeOutCubic)
  const handleLogoClick = (e: React.MouseEvent<HTMLAnchorElement>) => {
    if (typeof window === "undefined") return;

    if (window.location.pathname === "/") {
      e.preventDefault();

      const startPosition = window.pageYOffset || document.documentElement.scrollTop;
      if (startPosition <= 0) return;

      const duration = 550; // ms
      const startTime = performance.now();

      const easeOutCubic = (t: number) => 1 - Math.pow(1 - t, 3);

      const animateScroll = (currentTime: number) => {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);
        const ease = easeOutCubic(progress);

        window.scrollTo(0, startPosition * (1 - ease));

        if (progress < 1) {
          window.requestAnimationFrame(animateScroll);
        }
      };

      window.requestAnimationFrame(animateScroll);
    }
  };

  return (
    <header className="fixed top-0 left-0 right-0 w-full z-50 bg-background/90 dark:bg-[#162032]/95 backdrop-blur-md border-b border-border dark:border-[#223147] transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-20 gap-4">
          {/* Left: Logo */}
          <div className="flex items-center gap-3 sm:gap-4 shrink-0">
            <Link
              href="/"
              onClick={handleLogoClick}
              className="flex items-center group focus:outline-none cursor-pointer"
              title="Về đầu trang TeleCare"
            >
              <Image
                src="/logo/telecare-remove-bg.png"
                alt="TeleCare Logo"
                width={260}
                height={80}
                priority
                className="h-14 sm:h-16 md:h-16 w-auto object-contain transition-transform duration-200 group-hover:scale-105"
              />
            </Link>
          </div>

          {/* Right Action Buttons */}
          <div className="hidden md:flex items-center gap-3 shrink-0">
            {authenticated ? (
              /* State 1: Authenticated User */
              <div className="flex items-center gap-3">
                {/* User Dropdown */}
                <UserDropdown />
              </div>
            ) : (
              /* State 2: Guest / Unauthenticated */
              <div className="flex items-center gap-3">
                {/* Tải ứng dụng */}
                <Link
                  href="#download-app"
                  className="h-10 flex items-center gap-1.5 text-sm font-medium text-foreground hover:text-primary transition-colors px-3 rounded-full hover:bg-muted"
                >
                  <Smartphone className="w-4 h-4 text-accent-ai" />
                  <span>Tải ứng dụng</span>
                </Link>

                {/* Divider */}
                <div className="h-5 w-px bg-border mx-1" />

                {/* Login / Register */}
                <div className="flex items-center gap-1.5 text-sm font-semibold">
                  <button
                    type="button"
                    onClick={() => login()}
                    className="text-foreground hover:text-primary transition-colors px-2.5 py-1.5 rounded-lg hover:bg-muted cursor-pointer"
                  >
                    Đăng nhập
                  </button>
                  <span className="text-muted-foreground font-light">|</span>
                  <button
                    type="button"
                    onClick={() => register()}
                    className="text-primary hover:underline transition-colors px-2.5 py-1.5 rounded-lg hover:bg-primary/10 cursor-pointer"
                  >
                    Đăng ký
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* Mobile Menu Button */}
          <div className="md:hidden flex items-center gap-2">
            {authenticated && <UserDropdown />}
            <button
              type="button"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="w-10 h-10 rounded-full border border-border bg-card flex items-center justify-center text-foreground hover:text-primary hover:bg-muted transition-colors cursor-pointer"
              aria-label="Toggle menu"
            >
              {mobileMenuOpen ? (
                <X className="w-5 h-5" />
              ) : (
                <Menu className="w-5 h-5" />
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Drawer Menu (Guest actions only when not logged in) */}
      {mobileMenuOpen && (
        <div className="md:hidden border-b border-border bg-card/95 backdrop-blur-xl px-4 pt-3 pb-6 space-y-4 animate-in slide-in-from-top-2 duration-200 shadow-xl">
          {authenticated ? (
            <div className="flex items-center justify-between px-1 pt-1">
              <Link
                href="/settings"
                onClick={() => setMobileMenuOpen(false)}
                className="flex items-center gap-2.5 text-sm font-semibold text-foreground hover:text-primary transition-colors"
              >
                {avatarUrl ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img
                    src={avatarUrl}
                    alt={username || "Đang tải"}
                    className="h-8 w-8 rounded-full object-cover"
                  />
                ) : (
                  <div className="w-8 h-8 rounded-full bg-primary text-primary-foreground font-bold flex items-center justify-center text-xs">
                    {(username || "U").charAt(0).toUpperCase()}
                  </div>
                )}
                <span>{username || "Cài đặt tài khoản"}</span>
              </Link>
              <button
                type="button"
                onClick={() => {
                  setMobileMenuOpen(false);
                  logout();
                }}
                className="text-xs font-semibold text-red-500 hover:text-red-600 flex items-center gap-1.5 px-3 py-1.5 rounded-lg hover:bg-red-50 dark:hover:bg-red-950/40 transition-colors cursor-pointer"
              >
                <span>Đăng xuất</span>
              </button>
            </div>
          ) : (
            <div className="flex flex-col gap-3 pt-1">
              <Link
                href="#download-app"
                onClick={() => setMobileMenuOpen(false)}
                className="px-3.5 py-2.5 rounded-xl text-sm font-semibold text-foreground hover:bg-muted flex items-center gap-3 transition-colors"
              >
                <Smartphone className="w-4 h-4 text-accent-ai" />
                <span>Tải ứng dụng HomeSpace</span>
              </Link>
              <div className="flex gap-2 w-full pt-1">
                <Button
                  variant="outline"
                  onClick={() => {
                    setMobileMenuOpen(false);
                    login();
                  }}
                  className="flex-1 justify-center gap-1.5 text-foreground border-border hover:bg-muted rounded-full h-10 cursor-pointer text-xs font-semibold"
                >
                  Đăng nhập
                </Button>
                <Button
                  onClick={() => {
                    setMobileMenuOpen(false);
                    register();
                  }}
                  className="flex-1 justify-center gap-1.5 bg-primary hover:bg-primary/90 text-primary-foreground rounded-full h-10 cursor-pointer text-xs font-semibold"
                >
                  Đăng ký
                </Button>
              </div>
            </div>
          )}
        </div>
      )}
    </header>
  );
}
