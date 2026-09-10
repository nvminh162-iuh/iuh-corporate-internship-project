"use client";

import React, { useState, useEffect, use } from "react";
import Link from "next/link";
import {
  ArrowLeft,
  Sparkles,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  Zap,
  PhoneCall,
  ShieldCheck,
  Award,
  Layers,
  X,
  FileQuestion,
} from "lucide-react";
import type { PublicPlanDetail } from "@/types/plan.type";
import { publicPlanService } from "@/services/public-plan.service";
import { formatVND, getApiErrorMessage, getBillingCycleLabel, getCategoryLabel } from "@/utils/planUtils";
import PlanDetailSkeleton from "@/components/plans/PlanDetailSkeleton";
import { toast } from "sonner";

import Header from "@/components/layout/Header";
import Footer from "@/components/layout/Footer";

interface PageProps {
  params: Promise<{ slug: string }>;
}

export default function PlanDetailPage({ params }: PageProps) {
  const resolvedParams = use(params);
  const slug = resolvedParams.slug;

  const [plan, setPlan] = useState<PublicPlanDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);

  // Subscribe modal state
  const [showSubscribeModal, setShowSubscribeModal] = useState(false);
  const [subscribedSuccess, setSubscribedSuccess] = useState(false);

  const fetchPlanDetail = async () => {
    setLoading(true);
    setError(null);
    setNotFound(false);

    try {
      const data = await publicPlanService.getPublicPlanBySlug(slug);
      setPlan(data);
    } catch (err) {
      if (typeof err === "object" && err !== null && "response" in err) {
        const axiosError = err as { response?: { status?: number; data?: { message?: string } } };
        if (axiosError.response?.status === 404) {
          setNotFound(true);
          return;
        }
      }
      setError(getApiErrorMessage(err, "Không thể tải thông tin gói cước. Vui lòng thử lại sau."));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPlanDetail();
  }, [slug]);

  const handleConfirmSubscription = () => {
    setSubscribedSuccess(true);
    toast.success(`Đã đăng ký tư vấn thành công cho gói ${plan?.name}`);
  };

  const closeSubscribeModal = () => {
    setShowSubscribeModal(false);
    setSubscribedSuccess(false);
  };

  if (loading) {
    return (
      <div className="min-h-screen flex flex-col bg-background text-foreground">
        <Header />
        <main className="flex-grow pt-20">
          <PlanDetailSkeleton />
        </main>
        <Footer />
      </div>
    );
  }

  if (notFound) {
    return (
      <div className="min-h-screen flex flex-col bg-background text-foreground">
        <Header />
        <main className="flex-grow pt-20 flex items-center justify-center py-16 px-4">
          <div className="bg-card/80 dark:bg-card/40 rounded-3xl border border-border p-8 text-center space-y-4 max-w-md w-full shadow-lg">
            <FileQuestion className="w-16 h-16 text-muted-foreground/60 mx-auto" />
            <h2 className="text-xl font-bold text-foreground">Gói Cước Không Tồn Tại</h2>
            <p className="text-xs text-muted-foreground">
              Gói cước bạn đang tìm kiếm không tồn tại, chưa được xuất bản hoặc đã ngừng cung cấp.
            </p>
            <Link
              href="/plans"
              className="h-11 px-6 rounded-2xl bg-primary hover:bg-primary/90 text-primary-foreground font-bold text-xs shadow-md transition-all inline-flex items-center justify-center gap-2"
            >
              <ArrowLeft className="w-4 h-4" />
              <span>Quay lại danh sách gói cước</span>
            </Link>
          </div>
        </main>
        <Footer />
      </div>
    );
  }

  if (error || !plan) {
    return (
      <div className="min-h-screen flex flex-col bg-background text-foreground">
        <Header />
        <main className="flex-grow pt-20 flex items-center justify-center py-16 px-4">
          <div className="bg-destructive/10 border border-destructive/30 rounded-3xl p-8 text-center space-y-4 max-w-md w-full shadow-lg">
            <AlertCircle className="w-12 h-12 text-destructive mx-auto" />
            <h2 className="text-lg font-bold text-destructive">Lỗi Tải Thông Tin Gói Cước</h2>
            <p className="text-xs text-muted-foreground">{error || "Không có dữ liệu gói cước."}</p>
            <button
              type="button"
              onClick={fetchPlanDetail}
              className="h-10 px-6 rounded-2xl bg-destructive text-destructive-foreground font-bold text-xs shadow-md transition-all cursor-pointer inline-flex items-center gap-2"
            >
              <RefreshCw className="w-4 h-4" />
              <span>Thử lại</span>
            </button>
          </div>
        </main>
        <Footer />
      </div>
    );
  }

  const categoryLabel = getCategoryLabel(plan.category?.code, plan.category?.name);

  return (
    <div className="min-h-screen flex flex-col bg-background text-foreground">
      <Header />
      <main className="flex-grow pt-20 py-8 sm:py-12">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 space-y-8">
        {/* Back Link */}
        <Link
          href="/plans"
          className="inline-flex items-center gap-2 text-xs font-bold text-muted-foreground hover:text-primary transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Quay lại danh sách gói cước</span>
        </Link>

        {/* Hero Package Banner Box */}
        <div className="relative overflow-hidden bg-card/90 dark:bg-card/40 rounded-3xl border border-border/80 p-6 sm:p-8 md:p-10 shadow-lg space-y-6">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <span className="px-3.5 py-1 rounded-full text-xs font-bold bg-primary/10 text-primary uppercase tracking-wider border border-primary/20">
                {categoryLabel}
              </span>
              <span className="px-3.5 py-1 rounded-full text-xs font-mono font-medium bg-muted text-muted-foreground border border-border/60">
                MÃ: {plan.code}
              </span>
            </div>

            {plan.highlighted && (
              <span className="inline-flex items-center gap-1.5 px-4 py-1.5 rounded-full text-xs font-extrabold bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20">
                <Sparkles className="w-4 h-4 text-amber-500" />
                Gói Nổi Bật Ưu Tiên
              </span>
            )}
          </div>

          <div className="space-y-3">
            <h1 className="text-2xl sm:text-3xl lg:text-4xl font-black text-foreground tracking-tight">
              {plan.name}
            </h1>
            {plan.summary && (
              <p className="text-sm sm:text-base text-muted-foreground leading-relaxed max-w-3xl">
                {plan.summary}
              </p>
            )}
          </div>

          <div className="pt-4 border-t border-border/60 flex flex-wrap items-baseline justify-between gap-4">
            <div>
              <span className="text-3xl sm:text-4xl font-black text-primary tracking-tight">
                {formatVND(plan.price)}
              </span>
              <span className="text-xs font-semibold text-muted-foreground ml-2">
                / {getBillingCycleLabel(plan.billingCycle)}
              </span>
            </div>

            <button
              type="button"
              onClick={() => setShowSubscribeModal(true)}
              className="h-12 px-8 rounded-2xl bg-primary hover:bg-primary/90 text-primary-foreground text-sm font-black shadow-lg shadow-primary/20 transition-all cursor-pointer flex items-center gap-2 hover:scale-[1.02]"
            >
              <Zap className="w-4 h-4 fill-primary-foreground" />
              <span>Đăng ký ngay</span>
            </button>
          </div>
        </div>

        {/* Content Layout Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {/* Main Info: Features & Description */}
          <div className="md:col-span-2 space-y-8">
            {/* Features List */}
            {plan.features && plan.features.length > 0 && (
              <div className="bg-card/90 dark:bg-card/40 rounded-3xl border border-border/80 p-6 sm:p-8 space-y-6 shadow-sm">
                <div className="flex items-center gap-2 text-foreground font-extrabold text-base">
                  <Award className="w-5 h-5 text-primary" />
                  <h2>Đặc Điểm & Ưu Đãi Gói Cước</h2>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {plan.features.map((feat, idx) => (
                    <div
                      key={feat.code || idx}
                      className="bg-muted/40 rounded-2xl p-4 border border-border/50 flex items-start gap-3"
                    >
                      <div className="w-8 h-8 rounded-xl bg-primary/10 text-primary flex items-center justify-center shrink-0 mt-0.5">
                        <CheckCircle2 className="w-4 h-4" />
                      </div>
                      <div className="space-y-0.5">
                        <span className="text-xs text-muted-foreground font-semibold">
                          {feat.name}
                        </span>
                        <p className="text-sm font-extrabold text-foreground">
                          {feat.value} {feat.unit ? <span className="text-xs font-medium">{feat.unit}</span> : ""}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Detailed Description */}
            <div className="bg-card/90 dark:bg-card/40 rounded-3xl border border-border/80 p-6 sm:p-8 space-y-4 shadow-sm">
              <div className="flex items-center gap-2 text-foreground font-extrabold text-base">
                <Layers className="w-5 h-5 text-primary" />
                <h2>Mô Tả Chi Tiết Gói Cước</h2>
              </div>

              {plan.description ? (
                <div className="text-xs sm:text-sm text-muted-foreground leading-relaxed whitespace-pre-line space-y-2">
                  {plan.description}
                </div>
              ) : (
                <p className="text-xs text-muted-foreground italic">
                  Chưa có thông tin mô tả bổ sung cho gói cước này.
                </p>
              )}
            </div>
          </div>

          {/* Sidebar CTA & Support Info */}
          <div className="space-y-6">
            <div className="bg-card/90 dark:bg-card/40 rounded-3xl border border-border/80 p-6 space-y-6 shadow-sm sticky top-6">
              <div className="space-y-2">
                <h3 className="font-extrabold text-base text-foreground">Thông Tin Đăng Ký</h3>
                <p className="text-xs text-muted-foreground leading-relaxed">
                  Đăng ký dễ dàng trong vài giây. Nhân viên CSKH TeleCare sẽ liên hệ kích hoạt dịch vụ cho bạn.
                </p>
              </div>

              <div className="space-y-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowSubscribeModal(true)}
                  className="w-full h-12 rounded-2xl bg-primary hover:bg-primary/90 text-primary-foreground text-xs font-black shadow-md shadow-primary/20 transition-all cursor-pointer flex items-center justify-center gap-2"
                >
                  <Zap className="w-4 h-4 fill-primary-foreground" />
                  <span>Đăng ký gói {plan.code}</span>
                </button>

                <a
                  href="tel:19001000"
                  className="w-full h-11 rounded-2xl border border-border hover:bg-muted text-foreground text-xs font-bold transition-all flex items-center justify-center gap-2"
                >
                  <PhoneCall className="w-4 h-4 text-primary" />
                  <span>Tổng đài CSKH: 1900 1000</span>
                </a>
              </div>

              <div className="pt-4 border-t border-border/60 space-y-3 text-xs text-muted-foreground">
                <div className="flex items-center gap-2">
                  <ShieldCheck className="w-4 h-4 text-emerald-500 shrink-0" />
                  <span>Cam kết băng thông & dịch vụ TeleCare</span>
                </div>
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-500 shrink-0" />
                  <span>Hỗ trợ tư vấn trực tuyến 24/7</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Subscription Modal */}
      {showSubscribeModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-background/80 backdrop-blur-xs animate-in fade-in duration-200">
          <div className="bg-card border border-border rounded-3xl p-6 sm:p-8 max-w-md w-full shadow-2xl relative space-y-6">
            <button
              type="button"
              onClick={closeSubscribeModal}
              className="absolute top-4 right-4 p-2 text-muted-foreground hover:text-foreground rounded-full hover:bg-muted transition-colors cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>

            {!subscribedSuccess ? (
              <>
                <div className="space-y-2 text-center">
                  <div className="w-12 h-12 rounded-2xl bg-primary/10 text-primary flex items-center justify-center mx-auto mb-2">
                    <Sparkles className="w-6 h-6" />
                  </div>
                  <h3 className="text-xl font-bold text-foreground">
                    Đăng Ký {plan.name}
                  </h3>
                  <p className="text-xs text-muted-foreground">
                    Xác nhận gửi thông tin tư vấn & kích hoạt gói cước cho số điện thoại chính chủ của bạn.
                  </p>
                </div>

                <div className="bg-muted/50 rounded-2xl p-4 border border-border/60 space-y-2 text-xs">
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Mã gói cước:</span>
                    <span className="font-mono font-bold text-foreground">{plan.code}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Giá cước niêm yết:</span>
                    <span className="font-bold text-primary">{formatVND(plan.price)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Chu kỳ áp dụng:</span>
                    <span className="font-medium text-foreground">{getBillingCycleLabel(plan.billingCycle)}</span>
                  </div>
                </div>

                <div className="pt-2 flex gap-3">
                  <button
                    type="button"
                    onClick={closeSubscribeModal}
                    className="flex-1 h-11 rounded-2xl border border-border text-foreground font-bold text-xs hover:bg-muted cursor-pointer"
                  >
                    Hủy bỏ
                  </button>
                  <button
                    type="button"
                    onClick={handleConfirmSubscription}
                    className="flex-1 h-11 rounded-2xl bg-primary hover:bg-primary/90 text-primary-foreground font-extrabold text-xs shadow-md shadow-primary/20 cursor-pointer"
                  >
                    Xác nhận đăng ký
                  </button>
                </div>
              </>
            ) : (
              <div className="text-center space-y-4 py-4">
                <CheckCircle2 className="w-16 h-16 text-emerald-500 mx-auto animate-in zoom-in-75 duration-300" />
                <h3 className="text-xl font-bold text-foreground">Gửi Yêu Cầu Thành Công!</h3>
                <p className="text-xs text-muted-foreground leading-relaxed">
                  Cảm ơn bạn đã quan tâm đến gói cước <strong className="text-foreground">{plan.name}</strong>. Bộ phận CSKH TeleCare sẽ liên hệ hỗ trợ bạn trong thời gian sớm nhất.
                </p>
                <button
                  type="button"
                  onClick={closeSubscribeModal}
                  className="w-full h-11 rounded-2xl bg-primary text-primary-foreground font-bold text-xs shadow-md cursor-pointer"
                >
                  Đóng cửa sổ
                </button>
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
