"use client";

import React, { useState, useEffect, useCallback, Suspense } from "react";
import { useRouter, useSearchParams, usePathname } from "next/navigation";
import { toast } from "sonner";
import {
  Sparkles,
  SearchX,
  AlertCircle,
  RefreshCw,
  ChevronLeft,
  ChevronRight,
  CheckCircle2,
  X,
} from "lucide-react";
import type {
  BillingCycle,
  PageResponse,
  PublicCategory,
  PublicPlanSummary,
} from "@/types/plan.type";
import { publicPlanService } from "@/services/public-plan.service";
import { formatVND, getApiErrorMessage, getBillingCycleLabel } from "@/utils/planUtils";
import PlanCard from "@/components/plans/PlanCard";
import PlanToolbar from "@/components/plans/PlanToolbar";
import PlanCardSkeleton from "@/components/plans/PlanCardSkeleton";

export default function PlansCatalogWrapper() {
  return (
    <Suspense fallback={<div className="min-h-screen bg-background py-10" />}>
      <PlansCatalogContent />
    </Suspense>
  );
}

export function PlansCatalogContent() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  // URL search params state
  const keywordParam = searchParams.get("keyword") || "";
  const categoryCodeParam = searchParams.get("categoryCode") || "";
  const minPriceParam = searchParams.get("minPrice") ? Number(searchParams.get("minPrice")) : undefined;
  const maxPriceParam = searchParams.get("maxPrice") ? Number(searchParams.get("maxPrice")) : undefined;
  const billingCycleParam = (searchParams.get("billingCycle") || "") as BillingCycle | "";
  const sortParam = searchParams.get("sort") || "displayOrder,asc";
  const pageParam = Number(searchParams.get("page") || "1");

  // Data state
  const [categories, setCategories] = useState<PublicCategory[]>([]);
  const [pageData, setPageData] = useState<PageResponse<PublicPlanSummary> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Selected plan for subscription dialog
  const [selectedPlanForSubscribe, setSelectedPlanForSubscribe] = useState<PublicPlanSummary | null>(null);
  const [subscribedSuccess, setSubscribedSuccess] = useState(false);

  // Load public categories once
  useEffect(() => {
    let isMounted = true;
    publicPlanService
      .getPublicCategories()
      .then((data) => {
        if (isMounted) setCategories(data);
      })
      .catch(() => {
        // Silently fallback if category list fails
      });
    return () => {
      isMounted = false;
    };
  }, []);

  // Update URL helper
  const updateQueryParams = useCallback(
    (newParams: Record<string, string | number | undefined | null>) => {
      const current = new URLSearchParams(Array.from(searchParams.entries()));

      Object.entries(newParams).forEach(([key, value]) => {
        if (value == null || value === "" || (key === "page" && value === 1)) {
          current.delete(key);
        } else {
          current.set(key, String(value));
        }
      });

      const search = current.toString();
      const query = search ? `?${search}` : "";
      router.push(`${pathname}${query}`, { scroll: false });
    },
    [router, pathname, searchParams]
  );

  // Fetch package catalog data whenever URL params change
  const fetchPlans = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const data = await publicPlanService.getPublicPlans({
        keyword: keywordParam,
        categoryCode: categoryCodeParam,
        minPrice: minPriceParam,
        maxPrice: maxPriceParam,
        billingCycle: billingCycleParam,
        page: pageParam,
        size: 9,
        sort: sortParam,
      });

      setPageData(data);
    } catch (err) {
      setError(getApiErrorMessage(err, "Không thể tải danh sách gói cước. Vui lòng thử lại sau."));
    } finally {
      setLoading(false);
    }
  }, [
    keywordParam,
    categoryCodeParam,
    minPriceParam,
    maxPriceParam,
    billingCycleParam,
    pageParam,
    sortParam,
  ]);

  useEffect(() => {
    fetchPlans();
  }, [fetchPlans]);

  // Handlers
  const handleKeywordChange = (val: string) => {
    updateQueryParams({ keyword: val, page: 1 });
  };

  const handleCategoryChange = (val: string) => {
    updateQueryParams({ categoryCode: val, page: 1 });
  };

  const handlePriceRangeChange = (min?: number, max?: number) => {
    updateQueryParams({ minPrice: min, maxPrice: max, page: 1 });
  };

  const handleBillingCycleChange = (val: BillingCycle | "") => {
    updateQueryParams({ billingCycle: val, page: 1 });
  };

  const handleSortChange = (val: string) => {
    updateQueryParams({ sort: val, page: 1 });
  };

  const handlePageChange = (newPage: number) => {
    updateQueryParams({ page: newPage });
  };

  const handleResetFilters = () => {
    router.push(pathname, { scroll: false });
  };

  const handleConfirmSubscription = () => {
    setSubscribedSuccess(true);
    toast.success(`Đã ghi nhận yêu cầu đăng ký gói ${selectedPlanForSubscribe?.name}`);
  };

  const closeSubscribeModal = () => {
    setSelectedPlanForSubscribe(null);
    setSubscribedSuccess(false);
  };

  const plans = pageData?.result || [];
  const totalPages = pageData?.totalPages || 1;
  const currentPage = pageData?.page || 1;

  return (
    <div className="min-h-screen bg-background text-foreground py-8 sm:py-12">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-8">
        {/* Page Hero Header */}
        <div className="text-center space-y-3 max-w-3xl mx-auto">
          <div className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-primary/10 text-primary font-extrabold text-xs tracking-wider uppercase border border-primary/20">
            <Sparkles className="w-4 h-4 text-primary" />
            <span>Gói Cước & Dịch Vụ TeleCare</span>
          </div>
          <h1 className="text-3xl sm:text-4xl lg:text-5xl font-black tracking-tight text-foreground">
            Tra Cứu Gói Cước Ưu Đãi Siêu Tốc
          </h1>
          <p className="text-sm sm:text-base text-muted-foreground leading-relaxed">
            Khám phá các gói cước Data 5G tốc độ cao, gọi thoại thả ga và dịch vụ chăm sóc sức khỏe TeleCare phù hợp nhất với nhu cầu sử dụng của bạn.
          </p>
        </div>

        {/* Toolbar & Filters */}
        <PlanToolbar
          categories={categories}
          keyword={keywordParam}
          categoryCode={categoryCodeParam}
          minPrice={minPriceParam}
          maxPrice={maxPriceParam}
          billingCycle={billingCycleParam}
          sort={sortParam}
          onKeywordChange={handleKeywordChange}
          onCategoryChange={handleCategoryChange}
          onPriceRangeChange={handlePriceRangeChange}
          onBillingCycleChange={handleBillingCycleChange}
          onSortChange={handleSortChange}
          onResetFilters={handleResetFilters}
        />

        {/* Catalog Content Grid */}
        {loading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {[1, 2, 3, 4, 5, 6].map((idx) => (
              <PlanCardSkeleton key={idx} />
            ))}
          </div>
        ) : error ? (
          <div className="bg-destructive/10 border border-destructive/30 rounded-3xl p-8 text-center space-y-4 max-w-xl mx-auto my-8">
            <AlertCircle className="w-12 h-12 text-destructive mx-auto" />
            <h3 className="text-lg font-bold text-destructive">Không thể tải dữ liệu gói cước</h3>
            <p className="text-xs text-muted-foreground">{error}</p>
            <button
              type="button"
              onClick={fetchPlans}
              className="h-10 px-6 rounded-2xl bg-destructive text-destructive-foreground font-bold text-xs shadow-md transition-all cursor-pointer inline-flex items-center gap-2"
            >
              <RefreshCw className="w-4 h-4" />
              <span>Thử lại</span>
            </button>
          </div>
        ) : plans.length === 0 ? (
          <div className="bg-card/60 dark:bg-card/30 rounded-3xl border border-border p-12 text-center space-y-4 max-w-md mx-auto my-8">
            <SearchX className="w-16 h-16 text-muted-foreground/60 mx-auto" />
            <h3 className="text-lg font-bold text-foreground">Không tìm thấy gói cước nào</h3>
            <p className="text-xs text-muted-foreground">
              Không có gói cước nào phù hợp với bộ lọc hiện tại. Vui lòng thử tìm kiếm lại hoặc xóa bộ lọc.
            </p>
            <button
              type="button"
              onClick={handleResetFilters}
              className="h-10 px-6 rounded-2xl bg-primary hover:bg-primary/90 text-primary-foreground font-bold text-xs shadow-md transition-all cursor-pointer inline-flex items-center gap-1.5"
            >
              <RotateCcwIcon className="w-4 h-4" />
              <span>Xóa bộ lọc</span>
            </button>
          </div>
        ) : (
          <div className="space-y-8">
            {/* Cards Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {plans.map((plan) => (
                <PlanCard
                  key={plan.id}
                  plan={plan}
                  onSubscribe={(item) => setSelectedPlanForSubscribe(item)}
                />
              ))}
            </div>

            {/* Pagination Controls */}
            {totalPages > 1 && (
              <div className="flex items-center justify-center gap-2 pt-4">
                <button
                  type="button"
                  disabled={currentPage <= 1}
                  onClick={() => handlePageChange(currentPage - 1)}
                  className="h-10 px-4 rounded-xl border border-border bg-card hover:bg-muted text-foreground text-xs font-bold transition-all disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer flex items-center gap-1"
                >
                  <ChevronLeft className="w-4 h-4" />
                  <span>Trang trước</span>
                </button>

                <span className="text-xs font-bold text-muted-foreground px-3">
                  Trang {currentPage} / {totalPages}
                </span>

                <button
                  type="button"
                  disabled={currentPage >= totalPages}
                  onClick={() => handlePageChange(currentPage + 1)}
                  className="h-10 px-4 rounded-xl border border-border bg-card hover:bg-muted text-foreground text-xs font-bold transition-all disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer flex items-center gap-1"
                >
                  <span>Trang sau</span>
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Subscription CTA Modal */}
      {selectedPlanForSubscribe && (
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
                    Đăng Ký {selectedPlanForSubscribe.name}
                  </h3>
                  <p className="text-xs text-muted-foreground">
                    Xác nhận yêu cầu tư vấn & đăng ký gói cước trực tiếp cho tài khoản dịch vụ TeleCare.
                  </p>
                </div>

                <div className="bg-muted/50 rounded-2xl p-4 border border-border/60 space-y-2 text-xs">
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Mã gói:</span>
                    <span className="font-mono font-bold text-foreground">{selectedPlanForSubscribe.code}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Giá cước:</span>
                    <span className="font-bold text-primary">{formatVND(selectedPlanForSubscribe.price)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Chu kỳ:</span>
                    <span className="font-medium text-foreground">{getBillingCycleLabel(selectedPlanForSubscribe.billingCycle)}</span>
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
                  Cảm ơn bạn đã quan tâm đến gói cước <strong className="text-foreground">{selectedPlanForSubscribe.name}</strong>. Bộ phận CSKH TeleCare sẽ liên hệ hỗ trợ bạn trong thời gian sớm nhất.
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
    </div>
  );
}

function RotateCcwIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg
      {...props}
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      viewBox="0 0 24 24"
    >
      <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8" />
      <path d="M3 3v5h5" />
    </svg>
  );
}
