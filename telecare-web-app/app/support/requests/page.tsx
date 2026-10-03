"use client";

import React, { useState, useEffect, useCallback, Suspense } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  Ticket,
  Search,
  Filter,
  PlusCircle,
  Clock,
  AlertCircle,
  ChevronRight,
  ChevronLeft,
  RefreshCw,
  LogIn,
  Layers,
} from "lucide-react";
import { useAuth } from "@/features/auth/useAuth";
import type { CustomerSupportRequestSummary } from "@/types/support.type";
import { supportService } from "@/services/support.service";
import Header from "@/components/layout/Header";
import Footer from "@/components/layout/Footer";

export default function MySupportRequestsPageWrapper() {
  return (
    <Suspense fallback={<div className="min-h-screen bg-background py-10" />}>
      <MySupportRequestsPage />
    </Suspense>
  );
}

const STATUS_FILTERS: { label: string; value: string }[] = [
  { label: "Tất cả", value: "ALL" },
  { label: "Mới tiếp nhận", value: "NEW" },
  { label: "Đã tiếp nhận", value: "RECEIVED" },
  { label: "Đang xử lý", value: "IN_PROGRESS" },
  { label: "Chờ phản hồi", value: "WAITING_CUSTOMER" },
  { label: "Đã giải quyết", value: "COMPLETED" },
  { label: "Đã đóng", value: "CLOSED" },
];

function getStatusBadge(status: string) {
  switch (status) {
    case "NEW":
      return {
        label: "Mới tạo",
        className: "bg-blue-500/10 text-blue-600 dark:text-blue-400 border-blue-500/20",
      };
    case "RECEIVED":
      return {
        label: "Đã tiếp nhận",
        className: "bg-cyan-500/10 text-cyan-600 dark:text-cyan-400 border-cyan-500/20",
      };
    case "IN_PROGRESS":
      return {
        label: "Đang xử lý",
        className: "bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/20",
      };
    case "WAITING_CUSTOMER":
      return {
        label: "Chờ bạn phản hồi",
        className: "bg-purple-500/10 text-purple-600 dark:text-purple-400 border-purple-500/20",
      };
    case "COMPLETED":
      return {
        label: "Đã giải quyết",
        className: "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/20",
      };
    case "CLOSED":
      return {
        label: "Đã đóng",
        className: "bg-slate-500/10 text-slate-600 dark:text-slate-400 border-slate-500/20",
      };
    default:
      return {
        label: status,
        className: "bg-muted text-muted-foreground border-border",
      };
  }
}

function formatDate(isoString?: string) {
  if (!isoString) return "-";
  try {
    const d = new Date(isoString);
    return d.toLocaleDateString("vi-VN", {
      hour: "2-digit",
      minute: "2-digit",
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    });
  } catch {
    return isoString;
  }
}

function MySupportRequestsPage() {
  const { authenticated, login } = useAuth();
  const router = useRouter();

  const [tickets, setTickets] = useState<CustomerSupportRequestSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filter & Pagination state
  const [selectedStatus, setSelectedStatus] = useState<string>("ALL");
  const [keyword, setKeyword] = useState<string>("");
  const [debouncedKeyword, setDebouncedKeyword] = useState<string>("");
  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [totalElements, setTotalElements] = useState<number>(0);

  // Debounce search input
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedKeyword(keyword);
      setPage(0);
    }, 400);
    return () => clearTimeout(timer);
  }, [keyword]);

  const loadTickets = useCallback(async () => {
    if (!authenticated) return;
    setLoading(true);
    setError(null);
    try {
      const res = await supportService.getMySupportRequests({
        page,
        size: 10,
        status: selectedStatus,
        keyword: debouncedKeyword,
      });

      setTickets(res?.result || []);
      setTotalPages(res?.totalPages || 1);
      setTotalElements(res?.totalElements || 0);
    } catch {
      setError("Không thể tải danh sách yêu cầu hỗ trợ. Vui lòng thử lại sau.");
    } finally {
      setLoading(false);
    }
  }, [authenticated, page, selectedStatus, debouncedKeyword]);

  useEffect(() => {
    const timer = setTimeout(() => {
      loadTickets();
    }, 0);
    return () => clearTimeout(timer);
  }, [loadTickets]);

  return (
    <div className="min-h-screen flex flex-col bg-background text-foreground">
      <Header />

      <main className="flex-1 pt-28 pb-16 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto w-full">
        {/* Unauthenticated View */}
        {!authenticated ? (
          <div className="max-w-lg mx-auto py-20 text-center space-y-6">
            <div className="w-16 h-16 rounded-2xl bg-primary/10 text-primary flex items-center justify-center mx-auto shadow-sm">
              <Ticket className="w-8 h-8" />
            </div>
            <div className="space-y-2">
              <h1 className="text-2xl font-bold tracking-tight">Yêu cầu hỗ trợ của tôi</h1>
              <p className="text-sm text-muted-foreground">
                Vui lòng đăng nhập để xem lịch sử và theo dõi tiến độ xử lý các yêu cầu hỗ trợ của bạn.
              </p>
            </div>
            <button
              type="button"
              onClick={() => login()}
              className="inline-flex items-center gap-2 px-6 py-3 rounded-full bg-primary hover:bg-primary/90 text-primary-foreground font-semibold shadow-md transition-all cursor-pointer"
            >
              <LogIn className="w-4 h-4" />
              <span>Đăng nhập ngay</span>
            </button>
          </div>
        ) : (
          /* Authenticated Dashboard */
          <div className="space-y-8">
            {/* Header Title & Action */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-border">
              <div>
                <div className="flex items-center gap-3">
                  <h1 className="text-2xl sm:text-3xl font-bold tracking-tight">
                    Yêu cầu hỗ trợ của tôi
                  </h1>
                  <span className="px-3 py-1 rounded-full bg-primary/10 text-primary text-xs font-bold">
                    {totalElements} yêu cầu
                  </span>
                </div>
                <p className="text-sm text-muted-foreground mt-1">
                  Theo dõi trạng thái, phản hồi và kết quả giải quyết các sự cố dịch vụ của bạn.
                </p>
              </div>

              <Link
                href="/support/new"
                className="inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-full bg-primary hover:bg-primary/90 text-primary-foreground text-sm font-semibold shadow-sm transition-all shrink-0 cursor-pointer"
              >
                <PlusCircle className="w-4 h-4" />
                <span>Gửi yêu cầu mới</span>
              </Link>
            </div>

            {/* Filters Bar */}
            <div className="grid grid-cols-1 md:grid-cols-12 gap-3 items-center">
              {/* Search */}
              <div className="md:col-span-6 relative">
                <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
                <input
                  type="text"
                  value={keyword}
                  onChange={(e) => setKeyword(e.target.value)}
                  placeholder="Tìm theo mã yêu cầu (SR-...) hoặc tiêu đề..."
                  className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-border bg-card text-foreground placeholder:text-muted-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                />
              </div>

              {/* Status Select / Filter */}
              <div className="md:col-span-6 flex items-center gap-2 overflow-x-auto pb-1 md:pb-0 scrollbar-none">
                <span className="text-xs text-muted-foreground font-medium shrink-0 flex items-center gap-1">
                  <Filter className="w-3.5 h-3.5" />
                  <span>Trạng thái:</span>
                </span>
                <div className="flex items-center gap-1.5 flex-nowrap">
                  {STATUS_FILTERS.map((f) => (
                    <button
                      key={f.value}
                      type="button"
                      onClick={() => {
                        setSelectedStatus(f.value);
                        setPage(0);
                      }}
                      className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors cursor-pointer ${
                        selectedStatus === f.value
                          ? "bg-primary text-primary-foreground shadow-xs"
                          : "bg-card border border-border text-muted-foreground hover:text-foreground hover:bg-muted"
                      }`}
                    >
                      {f.label}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            {/* Content List */}
            {loading ? (
              <div className="space-y-3">
                {[1, 2, 3, 4].map((i) => (
                  <div
                    key={i}
                    className="p-5 rounded-2xl border border-border bg-card animate-pulse space-y-3"
                  >
                    <div className="flex items-center justify-between">
                      <div className="h-4 w-32 bg-muted rounded" />
                      <div className="h-5 w-24 bg-muted rounded-full" />
                    </div>
                    <div className="h-5 w-3/4 bg-muted rounded" />
                    <div className="h-3 w-40 bg-muted rounded" />
                  </div>
                ))}
              </div>
            ) : error ? (
              <div className="p-8 rounded-2xl border border-destructive/20 bg-destructive/5 text-center space-y-3">
                <AlertCircle className="w-8 h-8 text-destructive mx-auto" />
                <p className="text-sm font-medium text-destructive">{error}</p>
                <button
                  type="button"
                  onClick={loadTickets}
                  className="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg bg-card border border-border text-xs font-semibold hover:bg-muted cursor-pointer"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                  <span>Thử lại</span>
                </button>
              </div>
            ) : tickets.length === 0 ? (
              <div className="py-16 text-center rounded-2xl border border-dashed border-border bg-card/50 space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-muted flex items-center justify-center mx-auto text-muted-foreground">
                  <Ticket className="w-6 h-6" />
                </div>
                <div className="space-y-1">
                  <h3 className="text-base font-bold text-foreground">Chưa có yêu cầu hỗ trợ nào</h3>
                  <p className="text-xs text-muted-foreground max-w-sm mx-auto">
                    {debouncedKeyword || selectedStatus !== "ALL"
                      ? "Không tìm thấy yêu cầu phù hợp với bộ lọc hiện tại."
                      : "Khi bạn gửi yêu cầu hỗ trợ hoặc báo cáo sự cố, thông tin sẽ hiển thị tại đây."}
                  </p>
                </div>
                <Link
                  href="/support/new"
                  className="inline-flex items-center gap-1.5 px-4 py-2 rounded-full bg-primary/10 text-primary text-xs font-bold hover:bg-primary/20 transition-colors"
                >
                  <PlusCircle className="w-3.5 h-3.5" />
                  <span>Tạo yêu cầu ngay</span>
                </Link>
              </div>
            ) : (
              <div className="space-y-3">
                {tickets.map((t) => {
                  const badge = getStatusBadge(t.status);
                  return (
                    <div
                      key={t.id}
                      onClick={() => router.push(`/support/requests/${t.id}`)}
                      className="group p-5 rounded-2xl border border-border bg-card hover:border-primary/40 hover:shadow-md transition-all cursor-pointer flex flex-col sm:flex-row sm:items-center justify-between gap-4"
                    >
                      <div className="space-y-2 flex-1 min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="font-mono text-xs font-bold text-foreground/80 bg-muted px-2 py-0.5 rounded-md">
                            {t.ticketCode}
                          </span>
                          {t.category && (
                            <span className="text-xs font-medium text-muted-foreground flex items-center gap-1">
                              <Layers className="w-3 h-3 text-primary" />
                              <span>{t.category.name}</span>
                            </span>
                          )}
                          <span
                            className={`text-xs px-2.5 py-0.5 rounded-full border font-semibold inline-flex items-center ${badge.className}`}
                          >
                            {badge.label}
                          </span>
                        </div>

                        <h3 className="text-base font-bold text-foreground group-hover:text-primary transition-colors line-clamp-1">
                          {t.subject}
                        </h3>

                        <div className="flex items-center gap-4 text-xs text-muted-foreground">
                          <span className="flex items-center gap-1">
                            <Clock className="w-3.5 h-3.5" />
                            <span>Tạo lúc: {formatDate(t.createdAt)}</span>
                          </span>
                          {t.updatedAt && (
                            <span className="hidden sm:inline">
                              Cập nhật: {formatDate(t.updatedAt)}
                            </span>
                          )}
                        </div>
                      </div>

                      <div className="flex items-center gap-2 self-end sm:self-center shrink-0">
                        <span className="text-xs font-semibold text-primary group-hover:translate-x-0.5 transition-transform flex items-center gap-1">
                          <span>Xem chi tiết</span>
                          <ChevronRight className="w-4 h-4" />
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between pt-4 border-t border-border">
                <span className="text-xs text-muted-foreground">
                  Trang <strong className="text-foreground">{page + 1}</strong> / {totalPages}
                </span>

                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    disabled={page === 0 || loading}
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    className="p-2 rounded-xl border border-border bg-card text-foreground hover:bg-muted disabled:opacity-40 disabled:cursor-not-allowed transition-colors cursor-pointer"
                    title="Trang trước"
                  >
                    <ChevronLeft className="w-4 h-4" />
                  </button>
                  <button
                    type="button"
                    disabled={page + 1 >= totalPages || loading}
                    onClick={() => setPage((p) => p + 1)}
                    className="p-2 rounded-xl border border-border bg-card text-foreground hover:bg-muted disabled:opacity-40 disabled:cursor-not-allowed transition-colors cursor-pointer"
                    title="Trang sau"
                  >
                    <ChevronRight className="w-4 h-4" />
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
      </main>

      <Footer />
    </div>
  );
}
