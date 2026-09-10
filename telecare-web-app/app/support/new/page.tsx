"use client";

import React, { useState, useEffect, Suspense } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { toast } from "sonner";
import {
  HelpCircle,
  Sparkles,
  Send,
  AlertCircle,
  RefreshCw,
  CheckCircle2,
  Copy,
  Check,
  ArrowLeft,
  ShieldCheck,
  Phone,
  Mail,
  Package,
  Layers,
  Clock,
  LogIn,
} from "lucide-react";
import { useAuth } from "@/features/auth/useAuth";
import type { SupportCategory, SupportRequest } from "@/types/support.type";
import { supportService } from "@/services/support.service";
import Header from "@/components/layout/Header";
import Footer from "@/components/layout/Footer";

export default function SupportNewPageWrapper() {
  return (
    <Suspense fallback={<div className="min-h-screen bg-background py-10" />}>
      <SupportNewPage />
    </Suspense>
  );
}

function SupportNewPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { authenticated, login } = useAuth();

  // Query params for pre-filling package info
  const planIdQuery = searchParams.get("planId") || searchParams.get("servicePlanId") || "";
  const planNameQuery = searchParams.get("planName") || searchParams.get("planCode") || "";

  // Data state
  const [categories, setCategories] = useState<SupportCategory[]>([]);
  const [categoriesLoading, setCategoriesLoading] = useState(true);
  const [categoriesError, setCategoriesError] = useState<string | null>(null);

  // Form state
  const [selectedCategoryCode, setSelectedCategoryCode] = useState<string>("");
  const [servicePlanId, setServicePlanId] = useState<string>(planIdQuery);
  const [subject, setSubject] = useState<string>("");
  const [content, setContent] = useState<string>("");
  const [contactPhone, setContactPhone] = useState<string>("");
  const [contactEmail, setContactEmail] = useState<string>("");

  // Validation errors
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  // Success state
  const [createdTicket, setCreatedTicket] = useState<SupportRequest | null>(null);
  const [copiedCode, setCopiedCode] = useState<boolean>(false);

  // Fetch support categories
  const fetchCategories = async () => {
    setCategoriesLoading(true);
    setCategoriesError(null);
    try {
      const data = await supportService.getSupportCategories();
      setCategories(data);
      if (data.length > 0) {
        // Default select PACKAGE if plan query is present, else first category
        const defaultCat = planIdQuery
          ? data.find((c) => c.code === "PACKAGE") || data[0]
          : data[0];
        setSelectedCategoryCode(defaultCat.code);
      }
    } catch {
      setCategoriesError("Không thể tải danh sách nhóm vấn đề. Vui lòng thử lại sau.");
    } finally {
      setCategoriesLoading(false);
    }
  };

  useEffect(() => {
    fetchCategories();
  }, []);

  // Update category if query planId changes
  useEffect(() => {
    if (planIdQuery) {
      setServicePlanId(planIdQuery);
    }
  }, [planIdQuery]);

  // Client-side validation
  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!selectedCategoryCode) {
      newErrors.categoryCode = "Vui lòng chọn nhóm vấn đề cần hỗ trợ";
    }

    const trimmedSubject = subject.trim();
    if (!trimmedSubject) {
      newErrors.subject = "Vui lòng nhập tiêu đề yêu cầu";
    } else if (trimmedSubject.length < 10) {
      newErrors.subject = "Tiêu đề phải có tối thiểu 10 ký tự";
    } else if (trimmedSubject.length > 200) {
      newErrors.subject = "Tiêu đề không được vượt quá 200 ký tự";
    }

    const trimmedContent = content.trim();
    if (!trimmedContent) {
      newErrors.content = "Vui lòng nhập nội dung mô tả yêu cầu";
    } else if (trimmedContent.length < 20) {
      newErrors.content = "Nội dung phải có tối thiểu 20 ký tự";
    } else if (trimmedContent.length > 2000) {
      newErrors.content = "Nội dung không được vượt quá 2.000 ký tự";
    }

    if (contactPhone.trim()) {
      const phoneRegex = /^[0-9+() -]{8,20}$/;
      if (!phoneRegex.test(contactPhone.trim())) {
        newErrors.contactPhone = "Số điện thoại không đúng định dạng";
      }
    }

    if (contactEmail.trim()) {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(contactEmail.trim())) {
        newErrors.contactEmail = "Email liên hệ không đúng định dạng";
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Submit Handler
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!authenticated) {
      toast.error("Vui lòng đăng nhập để gửi yêu cầu hỗ trợ");
      login();
      return;
    }

    if (!validateForm() || isSubmitting) {
      return;
    }

    setIsSubmitting(true);

    try {
      const ticket = await supportService.createSupportRequest({
        categoryCode: selectedCategoryCode,
        servicePlanId: servicePlanId.trim() || undefined,
        subject: subject.trim(),
        content: content.trim(),
        contactPhone: contactPhone.trim() || undefined,
        contactEmail: contactEmail.trim() || undefined,
      });

      setCreatedTicket(ticket);
      toast.success(`Tạo yêu cầu hỗ trợ thành công! Mã ticket: ${ticket.ticketCode}`);
    } catch (err: unknown) {
      let msg = "Không thể gửi yêu cầu hỗ trợ. Vui lòng thử lại sau.";
      if (typeof err === "object" && err !== null && "response" in err) {
        const axiosError = err as { response?: { data?: { message?: string } } };
        if (axiosError.response?.data?.message) {
          msg = axiosError.response.data.message;
        }
      }
      toast.error(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCopyTicketCode = () => {
    if (!createdTicket) return;
    navigator.clipboard.writeText(createdTicket.ticketCode);
    setCopiedCode(true);
    toast.success("Đã sao chép mã tra cứu!");
    setTimeout(() => setCopiedCode(false), 3000);
  };

  return (
    <div className="min-h-screen flex flex-col bg-background text-foreground">
      <Header />

      <main className="flex-grow pt-24 pb-16">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 space-y-8">
          {/* Back Navigation */}
          <Link
            href="/"
            className="inline-flex items-center gap-2 text-xs font-bold text-muted-foreground hover:text-primary transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Quay lại trang chủ</span>
          </Link>

          {/* Hero Header */}
          <div className="text-center space-y-3 max-w-2xl mx-auto">
            <div className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-primary/10 text-primary font-extrabold text-xs tracking-wider uppercase border border-primary/20">
              <HelpCircle className="w-4 h-4 text-primary" />
              <span>Trung Tâm Hỗ Trợ Khách Hàng</span>
            </div>
            <h1 className="text-3xl sm:text-4xl font-black tracking-tight text-foreground">
              Gửi Yêu Cầu Hỗ Trợ Trực Tuyến
            </h1>
            <p className="text-sm text-muted-foreground leading-relaxed">
              Điền thông tin sự cố hoặc câu hỏi của bạn. Đội ngũ kỹ thuật & CSKH TeleCare sẽ tiếp nhận và xử lý trong thời gian sớm nhất.
            </p>
          </div>

          {/* If Created Ticket Success Screen */}
          {createdTicket ? (
            <div className="bg-card/90 dark:bg-card/40 border border-emerald-500/30 rounded-3xl p-8 sm:p-10 shadow-2xl space-y-8 animate-in zoom-in-95 duration-300">
              <div className="text-center space-y-4">
                <div className="w-16 h-16 rounded-full bg-emerald-500/10 text-emerald-500 flex items-center justify-center mx-auto">
                  <CheckCircle2 className="w-10 h-10 animate-in zoom-in-75 duration-300" />
                </div>
                <div className="space-y-1">
                  <h2 className="text-2xl font-extrabold text-foreground">Gửi Yêu Cầu Thành Công!</h2>
                  <p className="text-xs text-muted-foreground">
                    Yêu cầu hỗ trợ của bạn đã được tiếp nhận và lưu trữ trên hệ thống.
                  </p>
                </div>
              </div>

              {/* Ticket Details Box */}
              <div className="bg-muted/50 dark:bg-muted/20 border border-border/80 rounded-2xl p-6 space-y-4">
                <div className="flex flex-wrap items-center justify-between gap-4 pb-4 border-b border-border/60">
                  <div>
                    <span className="text-xs text-muted-foreground font-medium block">Mã Yêu Cầu (Ticket Code)</span>
                    <div className="flex items-center gap-2 mt-1">
                      <span className="text-xl sm:text-2xl font-mono font-black text-primary tracking-wider">
                        {createdTicket.ticketCode}
                      </span>
                      <button
                        type="button"
                        onClick={handleCopyTicketCode}
                        className="p-2 rounded-xl bg-primary/10 hover:bg-primary/20 text-primary transition-colors cursor-pointer"
                        title="Sao chép mã"
                      >
                        {copiedCode ? <Check className="w-4 h-4 text-emerald-500" /> : <Copy className="w-4 h-4" />}
                      </button>
                    </div>
                  </div>

                  <div className="text-right">
                    <span className="text-xs text-muted-foreground font-medium block">Trạng thái</span>
                    <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-extrabold bg-blue-500/10 text-blue-600 dark:text-blue-400 border border-blue-500/20 mt-1">
                      <Clock className="w-3.5 h-3.5" />
                      Mới tiếp nhận (NEW)
                    </span>
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                  <div>
                    <span className="text-muted-foreground font-medium">Nhóm vấn đề:</span>
                    <span className="font-bold text-foreground ml-1.5">{createdTicket.category?.name}</span>
                  </div>
                  <div>
                    <span className="text-muted-foreground font-medium">Thời gian gửi:</span>
                    <span className="font-medium text-foreground ml-1.5">
                      {new Date(createdTicket.createdAt).toLocaleString("vi-VN")}
                    </span>
                  </div>
                  <div className="sm:col-span-2">
                    <span className="text-muted-foreground font-medium">Tiêu đề:</span>
                    <span className="font-bold text-foreground ml-1.5">{createdTicket.subject}</span>
                  </div>
                </div>
              </div>

              {/* Instructions Banner */}
              <div className="bg-amber-500/10 border border-amber-500/20 rounded-2xl p-4 flex items-start gap-3 text-xs text-amber-700 dark:text-amber-300">
                <ShieldCheck className="w-5 h-5 shrink-0 mt-0.5" />
                <div className="space-y-1">
                  <p className="font-bold">Lưu ý quan trọng:</p>
                  <p className="leading-relaxed">
                    Vui lòng chụp lại màn hình hoặc lưu lại <strong>Mã Ticket {createdTicket.ticketCode}</strong> để thuận tiện tra cứu tiến độ xử lý từ nhân viên chăm sóc khách hàng TeleCare.
                  </p>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="flex flex-col sm:flex-row gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => {
                    setCreatedTicket(null);
                    setSubject("");
                    setContent("");
                  }}
                  className="flex-1 h-11 rounded-2xl border border-border text-foreground font-bold text-xs hover:bg-muted transition-colors cursor-pointer"
                >
                  Gửi yêu cầu khác
                </button>
                <Link
                  href="/"
                  className="flex-1 h-11 rounded-2xl bg-primary hover:bg-primary/90 text-primary-foreground font-bold text-xs shadow-md transition-all flex items-center justify-center gap-2"
                >
                  <span>Về trang chủ</span>
                </Link>
              </div>
            </div>
          ) : (
            /* Main Support Form */
            <div className="bg-card/90 dark:bg-card/40 border border-border/80 rounded-3xl p-6 sm:p-8 shadow-xl space-y-6">
              {/* Login Status Alert for Guests */}
              {!authenticated && (
                <div className="bg-amber-500/10 border border-amber-500/30 rounded-2xl p-4 flex flex-wrap items-center justify-between gap-3">
                  <div className="flex items-center gap-2 text-xs text-amber-700 dark:text-amber-300 font-medium">
                    <AlertCircle className="w-4 h-4 shrink-0" />
                    <span>Bạn cần đăng nhập tài khoản TeleCare để gửi yêu cầu hỗ trợ.</span>
                  </div>
                  <button
                    type="button"
                    onClick={() => login()}
                    className="h-9 px-4 rounded-xl bg-amber-500 hover:bg-amber-600 text-white font-bold text-xs shadow-sm transition-colors flex items-center gap-1.5 cursor-pointer"
                  >
                    <LogIn className="w-3.5 h-3.5" />
                    <span>Đăng nhập ngay</span>
                  </button>
                </div>
              )}

              {/* Package Context Banner (if navigating from package detail) */}
              {planNameQuery && (
                <div className="bg-primary/10 border border-primary/20 rounded-2xl p-4 flex items-center gap-3 text-xs">
                  <Package className="w-5 h-5 text-primary shrink-0" />
                  <div>
                    <span className="text-muted-foreground">Yêu cầu liên quan đến gói cước: </span>
                    <strong className="text-primary font-bold">{planNameQuery}</strong>
                  </div>
                </div>
              )}

              <form onSubmit={handleSubmit} className="space-y-6">
                {/* 1. Category Selection */}
                <div className="space-y-2">
                  <label className="block text-xs font-bold text-foreground uppercase tracking-wider">
                    Nhóm Vấn Đề <span className="text-destructive">*</span>
                  </label>

                  {categoriesLoading ? (
                    <div className="h-12 bg-muted/50 rounded-2xl animate-pulse" />
                  ) : categoriesError ? (
                    <div className="p-4 rounded-2xl bg-destructive/10 text-destructive text-xs flex items-center justify-between">
                      <span>{categoriesError}</span>
                      <button
                        type="button"
                        onClick={fetchCategories}
                        className="text-xs underline font-bold"
                      >
                        Thử lại
                      </button>
                    </div>
                  ) : (
                    <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-2.5">
                      {categories.map((cat) => {
                        const isSelected = selectedCategoryCode === cat.code;
                        return (
                          <button
                            key={cat.code}
                            type="button"
                            onClick={() => setSelectedCategoryCode(cat.code)}
                            className={`p-3 rounded-2xl border text-left transition-all cursor-pointer flex flex-col justify-between ${
                              isSelected
                                ? "bg-primary text-primary-foreground border-primary shadow-md scale-[1.02]"
                                : "bg-card hover:bg-muted border-border text-foreground"
                            }`}
                          >
                            <span className="text-xs font-extrabold">{cat.name}</span>
                            <span className={`text-[10px] line-clamp-1 mt-1 ${isSelected ? "text-primary-foreground/80" : "text-muted-foreground"}`}>
                              {cat.code}
                            </span>
                          </button>
                        );
                      })}
                    </div>
                  )}

                  {errors.categoryCode && (
                    <p className="text-xs text-destructive font-medium">{errors.categoryCode}</p>
                  )}
                </div>

                {/* 2. Subject Input with Live Character Counter */}
                <div className="space-y-2">
                  <div className="flex justify-between items-center">
                    <label htmlFor="subject" className="text-xs font-bold text-foreground uppercase tracking-wider">
                      Tiêu Đề Yêu Cầu <span className="text-destructive">*</span>
                    </label>
                    <span className={`text-xs font-mono font-medium ${subject.length > 200 ? "text-destructive" : "text-muted-foreground"}`}>
                      {subject.length} / 200
                    </span>
                  </div>

                  <input
                    id="subject"
                    type="text"
                    value={subject}
                    onChange={(e) => setSubject(e.target.value)}
                    placeholder="Ví dụ: Sự cố kết nối mạng 5G không ổn định tại khu vực..."
                    maxLength={200}
                    className={`w-full h-12 px-4 rounded-2xl bg-card border text-sm font-medium focus:outline-none focus:ring-2 focus:ring-primary/40 transition-all ${
                      errors.subject ? "border-destructive" : "border-border"
                    }`}
                  />
                  {errors.subject && (
                    <p className="text-xs text-destructive font-medium">{errors.subject}</p>
                  )}
                </div>

                {/* 3. Content Textarea with Live Character Counter */}
                <div className="space-y-2">
                  <div className="flex justify-between items-center">
                    <label htmlFor="content" className="text-xs font-bold text-foreground uppercase tracking-wider">
                      Nội Dung Mô Tả Chi Tiết <span className="text-destructive">*</span>
                    </label>
                    <span className={`text-xs font-mono font-medium ${content.length > 2000 ? "text-destructive" : "text-muted-foreground"}`}>
                      {content.length} / 2000
                    </span>
                  </div>

                  <textarea
                    id="content"
                    rows={6}
                    value={content}
                    onChange={(e) => setContent(e.target.value)}
                    placeholder="Mô tả chi tiết tình trạng gặp phải, mốc thời gian diễn ra sự cố, thông báo lỗi nếu có..."
                    maxLength={2000}
                    className={`w-full p-4 rounded-2xl bg-card border text-sm font-medium focus:outline-none focus:ring-2 focus:ring-primary/40 transition-all leading-relaxed ${
                      errors.content ? "border-destructive" : "border-border"
                    }`}
                  />
                  {errors.content && (
                    <p className="text-xs text-destructive font-medium">{errors.content}</p>
                  )}
                </div>

                {/* 4. Contact Phone & Contact Email (Optional Grid) */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <label htmlFor="contactPhone" className="text-xs font-bold text-foreground uppercase tracking-wider flex items-center gap-1.5">
                      <Phone className="w-3.5 h-3.5 text-primary" />
                      <span>Số Điện Thoại Liên Hệ (Tùy chọn)</span>
                    </label>
                    <input
                      id="contactPhone"
                      type="tel"
                      value={contactPhone}
                      onChange={(e) => setContactPhone(e.target.value)}
                      placeholder="0987654321"
                      className={`w-full h-12 px-4 rounded-2xl bg-card border text-sm font-medium focus:outline-none focus:ring-2 focus:ring-primary/40 transition-all ${
                        errors.contactPhone ? "border-destructive" : "border-border"
                      }`}
                    />
                    {errors.contactPhone && (
                      <p className="text-xs text-destructive font-medium">{errors.contactPhone}</p>
                    )}
                  </div>

                  <div className="space-y-2">
                    <label htmlFor="contactEmail" className="text-xs font-bold text-foreground uppercase tracking-wider flex items-center gap-1.5">
                      <Mail className="w-3.5 h-3.5 text-primary" />
                      <span>Email Phản Hồi (Tùy chọn)</span>
                    </label>
                    <input
                      id="contactEmail"
                      type="email"
                      value={contactEmail}
                      onChange={(e) => setContactEmail(e.target.value)}
                      placeholder="example@gmail.com"
                      className={`w-full h-12 px-4 rounded-2xl bg-card border text-sm font-medium focus:outline-none focus:ring-2 focus:ring-primary/40 transition-all ${
                        errors.contactEmail ? "border-destructive" : "border-border"
                      }`}
                    />
                    {errors.contactEmail && (
                      <p className="text-xs text-destructive font-medium">{errors.contactEmail}</p>
                    )}
                  </div>
                </div>

                {/* Submit Action Button */}
                <div className="pt-4">
                  <button
                    type="submit"
                    disabled={isSubmitting}
                    className="w-full h-12 rounded-2xl bg-primary hover:bg-primary/90 text-primary-foreground font-black text-sm shadow-lg shadow-primary/20 transition-all disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer flex items-center justify-center gap-2"
                  >
                    {isSubmitting ? (
                      <>
                        <RefreshCw className="w-4 h-4 animate-spin" />
                        <span>Đang gửi yêu cầu...</span>
                      </>
                    ) : (
                      <>
                        <Send className="w-4 h-4" />
                        <span>Gửi Yêu Cầu Hỗ Trợ</span>
                      </>
                    )}
                  </button>
                </div>
              </form>
            </div>
          )}
        </div>
      </main>

      <Footer />
    </div>
  );
}
