import type { BillingCycle, PlanStatus } from "@/types/plan.type";

export function formatCurrency(amount?: number | null, currency = "VND"): string {
  if (amount === undefined || amount === null) return "0 ₫";
  try {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: currency || "VND",
    }).format(amount);
  } catch {
    return `${amount} ${currency}`;
  }
}

export function formatBillingCycle(cycle?: BillingCycle | null): string {
  switch (cycle) {
    case "DAY":
      return "Theo ngày";
    case "MONTH":
      return "Theo tháng";
    case "YEAR":
      return "Theo năm";
    case "ONE_TIME":
      return "Thanh toán 1 lần";
    default:
      return cycle || "N/A";
  }
}

export interface PlanStatusInfo {
  label: string;
  className: string;
  dotClassName: string;
}

export function getPlanStatusInfo(status?: PlanStatus | null): PlanStatusInfo {
  switch (status) {
    case "PUBLISHED":
      return {
        label: "Đã xuất bản",
        className: "border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 font-semibold",
        dotClassName: "bg-emerald-500",
      };
    case "HIDDEN":
      return {
        label: "Tạm ẩn",
        className: "border-slate-500/30 bg-slate-500/10 text-slate-600 dark:text-slate-400 font-semibold",
        dotClassName: "bg-slate-400",
      };
    case "DRAFT":
    default:
      return {
        label: "Bản nháp",
        className: "border-amber-500/30 bg-amber-500/10 text-amber-600 dark:text-amber-400 font-semibold",
        dotClassName: "bg-amber-500",
      };
  }
}

export function sanitizeCode(code: string): string {
  return code.toUpperCase().trim();
}

export function sanitizeSlug(slug: string): string {
  return slug.toLowerCase().trim();
}
