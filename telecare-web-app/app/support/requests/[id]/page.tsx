"use client";

import React, { useState, useEffect, useCallback, Suspense } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import {
  ArrowLeft,
  Clock,
  Layers,
  Phone,
  Mail,
  Package,
  AlertCircle,
  Copy,
  Check,
  RefreshCw,
  ShieldCheck,
  Calendar,
} from "lucide-react";
import { useAuth } from "@/features/auth/useAuth";
import type {
  CustomerSupportRequestDetail,
  CustomerSupportRequestHistory,
} from "@/types/support.type";
import { supportService } from "@/services/support.service";
import Header from "@/components/layout/Header";
import Footer from "@/components/layout/Footer";

export default function SupportRequestDetailPageWrapper() {
  return (
    <Suspense fallback={<div className="min-h-screen bg-background py-10" />}>
      <SupportRequestDetailPage />
    </Suspense>
  );
}

function getStatusBadge(status?: string) {
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
        label: status || "Không rõ",
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

function SupportRequestDetailPage() {
  const params = useParams();
  const id = params?.id as string;
  const { authenticated } = useAuth();

  const [ticket, setTicket] = useState<CustomerSupportRequestDetail | null>(null);
  const [histories, setHistories] = useState<CustomerSupportRequestHistory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const loadData = useCallback(async () => {
    if (!id || !authenticated) return;
    setLoading(true);
    setError(null);

    try {
      const [ticketData, historyData] = await Promise.all([
        supportService.getMySupportRequestById(id),
        supportService.getMySupportRequestHistories(id),
      ]);
      setTicket(ticketData);
      setHistories(historyData || []);
    } catch {
      setError("Không tìm thấy thông tin yêu cầu hỗ trợ hoặc bạn không có quyền truy cập.");
    } finally {
      setLoading(false);
    }
  }, [id, authenticated]);

  useEffect(() => {
    const timer = setTimeout(() => {
      loadData();
    }, 0);
    return () => clearTimeout(timer);
  }, [loadData]);

  const copyTicketCode = () => {
    if (!ticket?.ticketCode) return;
    navigator.clipboard.writeText(ticket.ticketCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="min-h-screen flex flex-col bg-background text-foreground">
      <Header />

      <main className="flex-1 pt-28 pb-16 px-4 sm:px-6 lg:px-8 max-w-5xl mx-auto w-full">
        {/* Back Link */}
        <div className="mb-6">
          <Link
            href="/support/requests"
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-muted-foreground hover:text-primary transition-colors group cursor-pointer"
          >
            <ArrowLeft className="w-4 h-4 group-hover:-translate-x-0.5 transition-transform" />
            <span>Quay lại danh sách yêu cầu</span>
          </Link>
        </div>

        {loading ? (
          <div className="space-y-6 animate-pulse">
            <div className="p-6 rounded-2xl border border-border bg-card space-y-4">
              <div className="h-6 w-48 bg-muted rounded" />
              <div className="h-8 w-3/4 bg-muted rounded" />
              <div className="h-20 w-full bg-muted rounded" />
            </div>
            <div className="p-6 rounded-2xl border border-border bg-card space-y-3">
              <div className="h-5 w-36 bg-muted rounded" />
              <div className="h-16 w-full bg-muted rounded" />
            </div>
          </div>
        ) : error || !ticket ? (
          <div className="p-12 text-center rounded-2xl border border-destructive/20 bg-destructive/5 space-y-4 max-w-md mx-auto my-12">
            <AlertCircle className="w-10 h-10 text-destructive mx-auto" />
            <div className="space-y-1">
              <h2 className="text-base font-bold text-foreground">Yêu cầu không tồn tại</h2>
              <p className="text-xs text-muted-foreground">{error}</p>
            </div>
            <div className="flex justify-center gap-3 pt-2">
              <Link
                href="/support/requests"
                className="px-4 py-2 rounded-xl bg-card border border-border text-xs font-semibold hover:bg-muted"
              >
                Về danh sách
              </Link>
              <button
                type="button"
                onClick={loadData}
                className="px-4 py-2 rounded-xl bg-primary text-primary-foreground text-xs font-semibold hover:bg-primary/90 flex items-center gap-1.5 cursor-pointer"
              >
                <RefreshCw className="w-3.5 h-3.5" />
                <span>Thử lại</span>
              </button>
            </div>
          </div>
        ) : (
          <div className="space-y-6">
            {/* Header Card */}
            <div className="p-6 sm:p-8 rounded-3xl border border-border bg-card shadow-sm space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-2">
                  <span className="font-mono text-sm font-bold text-foreground bg-muted px-3 py-1 rounded-lg">
                    {ticket.ticketCode}
                  </span>
                  <button
                    type="button"
                    onClick={copyTicketCode}
                    className="p-1.5 rounded-lg border border-border hover:bg-muted text-muted-foreground hover:text-foreground transition-colors cursor-pointer"
                    title="Sao chép mã yêu cầu"
                  >
                    {copied ? (
                      <Check className="w-3.5 h-3.5 text-emerald-500" />
                    ) : (
                      <Copy className="w-3.5 h-3.5" />
                    )}
                  </button>
                </div>

                <div className="flex items-center gap-2">
                  {ticket.category && (
                    <span className="text-xs font-semibold text-muted-foreground flex items-center gap-1 bg-muted/60 px-2.5 py-1 rounded-full border border-border">
                      <Layers className="w-3 h-3 text-primary" />
                      <span>{ticket.category.name}</span>
                    </span>
                  )}
                  <span
                    className={`text-xs px-3 py-1 rounded-full border font-bold ${
                      getStatusBadge(ticket.status).className
                    }`}
                  >
                    {getStatusBadge(ticket.status).label}
                  </span>
                </div>
              </div>

              <h1 className="text-xl sm:text-2xl font-bold text-foreground leading-snug">
                {ticket.subject}
              </h1>

              {/* Service plan if applicable */}
              {ticket.servicePlan && (
                <div className="inline-flex items-center gap-2 text-xs font-medium text-muted-foreground bg-primary/5 border border-primary/20 px-3 py-1.5 rounded-xl">
                  <Package className="w-3.5 h-3.5 text-primary" />
                  <span>
                    Gói dịch vụ liên quan: <strong className="text-foreground">{ticket.servicePlan.name}</strong> ({ticket.servicePlan.code})
                  </span>
                </div>
              )}

              {/* Content Description */}
              <div className="pt-2 border-t border-border">
                <h3 className="text-xs font-bold text-muted-foreground uppercase tracking-wider mb-2">
                  Nội dung chi tiết
                </h3>
                <p className="text-sm text-foreground/90 whitespace-pre-wrap leading-relaxed bg-muted/30 p-4 rounded-2xl border border-border/50">
                  {ticket.content}
                </p>
              </div>

              {/* Meta details */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-3 border-t border-border text-xs text-muted-foreground">
                <div className="flex items-center gap-1.5">
                  <Clock className="w-3.5 h-3.5 text-primary" />
                  <span>Tạo lúc: {formatDate(ticket.createdAt)}</span>
                </div>
                {ticket.contactPhone && (
                  <div className="flex items-center gap-1.5">
                    <Phone className="w-3.5 h-3.5 text-primary" />
                    <span>SĐT liên hệ: {ticket.contactPhone}</span>
                  </div>
                )}
                {ticket.contactEmail && (
                  <div className="flex items-center gap-1.5 truncate">
                    <Mail className="w-3.5 h-3.5 text-primary" />
                    <span className="truncate">Email: {ticket.contactEmail}</span>
                  </div>
                )}
              </div>
            </div>

            {/* Resolution Box (when completed or resolution present) */}
            {ticket.resolution && (
              <div className="p-6 rounded-3xl border border-emerald-500/30 bg-emerald-500/5 space-y-2 shadow-sm">
                <div className="flex items-center gap-2 text-emerald-600 dark:text-emerald-400">
                  <ShieldCheck className="w-5 h-5" />
                  <h3 className="text-sm font-bold">Kết quả xử lý & Giải pháp</h3>
                </div>
                <p className="text-sm text-foreground/90 whitespace-pre-wrap leading-relaxed pl-7">
                  {ticket.resolution}
                </p>
                {ticket.completedAt && (
                  <p className="text-[11px] text-muted-foreground pl-7 pt-1">
                    Hoàn thành lúc: {formatDate(ticket.completedAt)}
                  </p>
                )}
              </div>
            )}

            {/* Customer Timeline Card */}
            <div className="p-6 sm:p-8 rounded-3xl border border-border bg-card shadow-sm space-y-6">
              <div className="flex items-center justify-between pb-3 border-b border-border">
                <div className="flex items-center gap-2">
                  <Calendar className="w-4 h-4 text-primary" />
                  <h2 className="text-base font-bold text-foreground">Tiến độ giải quyết</h2>
                </div>
                <span className="text-xs text-muted-foreground">
                  {histories.length} mốc cập nhật
                </span>
              </div>

              {histories.length === 0 ? (
                <div className="py-6 text-center text-xs text-muted-foreground">
                  Yêu cầu đang chờ nhân viên phụ trách tiếp nhận xử lý.
                </div>
              ) : (
                <div className="relative pl-6 space-y-6 before:absolute before:left-2 before:top-2 before:bottom-2 before:w-0.5 before:bg-border">
                  {histories.map((h, idx) => {
                    const isLatest = idx === histories.length - 1;
                    const badge = getStatusBadge(h.toStatus);
                    return (
                      <div key={h.id || idx} className="relative group">
                        {/* Timeline dot */}
                        <div
                          className={`absolute -left-6 top-1 w-4 h-4 rounded-full border-2 transition-all ${
                            isLatest
                              ? "bg-primary border-background ring-4 ring-primary/20"
                              : "bg-card border-muted-foreground/40"
                          }`}
                        />

                        <div className="space-y-1">
                          <div className="flex flex-wrap items-center gap-2">
                            <span
                              className={`text-[11px] px-2 py-0.5 rounded-full border font-bold ${badge.className}`}
                            >
                              {badge.label}
                            </span>
                            <span className="text-xs text-muted-foreground">
                              {formatDate(h.createdAt)}
                            </span>
                          </div>

                          {h.message && (
                            <p className="text-xs text-foreground/80 leading-relaxed bg-muted/40 p-2.5 rounded-xl mt-1.5 border border-border/40">
                              {h.message}
                            </p>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        )}
      </main>

      <Footer />
    </div>
  );
}
