import type { BillingCycle } from "@/types/plan.type";

export function formatVND(amount?: number | null): string {
  if (amount == null || Number.isNaN(amount)) return "0 ₫";
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(amount);
}

export function getBillingCycleLabel(cycle?: BillingCycle | null): string {
  switch (cycle) {
    case "DAY":
      return "Ngày";
    case "MONTH":
      return "Tháng";
    case "YEAR":
      return "Năm";
    case "ONE_TIME":
      return "Lượt";
    default:
      return cycle || "Tháng";
  }
}

export function getCategoryLabel(code?: string | null, name?: string | null): string {
  if (!code) return name || "Gói dịch vụ";
  const upperCode = code.toUpperCase();
  switch (upperCode) {
    case "DATA":
      return "Data & Internet di động";
    case "CALL":
      return "Gọi thoại & SMS";
    case "COMBO":
      return "Gói tích hợp";
    case "INTERNET":
      return "Internet gia đình";
    case "HEALTH":
      return "Dịch vụ TeleCare";
    default:
      return name || code;
  }
}

export function getApiErrorMessage(error: unknown, defaultMsg = "Đã xảy ra lỗi, vui lòng thử lại sau."): string {
  if (typeof error === "object" && error !== null && "response" in error) {
    const axiosError = error as { response?: { data?: { message?: string } } };
    return axiosError.response?.data?.message || defaultMsg;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return defaultMsg;
}
