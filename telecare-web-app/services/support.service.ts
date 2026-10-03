import publicAxiosClient from "@/lib/public-axios-client";
import axiosClient from "@/lib/axios-client";
import type { ApiResponse, PageResponse } from "@/types/plan.type";
import type {
  CreateSupportRequestInput,
  CustomerSupportRequestDetail,
  CustomerSupportRequestHistory,
  CustomerSupportRequestQuery,
  CustomerSupportRequestSummary,
  SupportCategory,
  SupportRequest,
} from "@/types/support.type";

export const supportService = {
  async getSupportCategories(): Promise<SupportCategory[]> {
    const response = await publicAxiosClient.get<ApiResponse<SupportCategory[]>>(
      "/api/v1/support-categories"
    );
    return response.data.result;
  },

  async createSupportRequest(
    input: CreateSupportRequestInput
  ): Promise<SupportRequest> {
    const response = await axiosClient.post<ApiResponse<SupportRequest>>(
      "/api/v1/support-requests",
      input
    );
    return response.data.result;
  },

  async getMySupportRequests(
    query: CustomerSupportRequestQuery = {}
  ): Promise<PageResponse<CustomerSupportRequestSummary>> {
    const params = new URLSearchParams();
    if (query.page != null) {
      params.append("page", Math.max(0, query.page).toString());
    }
    if (query.size != null) {
      params.append("size", query.size.toString());
    }
    if (query.status && query.status !== "ALL") {
      params.append("status", query.status);
    }
    if (query.keyword && query.keyword.trim()) {
      params.append("keyword", query.keyword.trim());
    }
    if (query.fromDate) {
      params.append("fromDate", query.fromDate);
    }
    if (query.toDate) {
      params.append("toDate", query.toDate);
    }
    if (query.sort) {
      params.append("sort", query.sort);
    }

    const response = await axiosClient.get<ApiResponse<PageResponse<CustomerSupportRequestSummary>>>(
      `/api/v1/support-requests/me?${params.toString()}`
    );
    return response.data.result;
  },

  async getMySupportRequestById(id: string): Promise<CustomerSupportRequestDetail> {
    const response = await axiosClient.get<ApiResponse<CustomerSupportRequestDetail>>(
      `/api/v1/support-requests/me/${encodeURIComponent(id)}`
    );
    return response.data.result;
  },

  async getMySupportRequestHistories(id: string): Promise<CustomerSupportRequestHistory[]> {
    const response = await axiosClient.get<ApiResponse<CustomerSupportRequestHistory[]>>(
      `/api/v1/support-requests/me/${encodeURIComponent(id)}/histories`
    );
    return response.data.result;
  },
};
