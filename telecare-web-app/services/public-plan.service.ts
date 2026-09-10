import publicAxiosClient from "@/lib/public-axios-client";
import type {
  ApiResponse,
  PageResponse,
  PublicCategory,
  PublicPlanDetail,
  PublicPlanQuery,
  PublicPlanSummary,
} from "@/types/plan.type";

export const publicPlanService = {
  async getPublicPlans(
    query: PublicPlanQuery = {},
  ): Promise<PageResponse<PublicPlanSummary>> {
    const params = new URLSearchParams();

    if (query.keyword && query.keyword.trim()) {
      params.append("keyword", query.keyword.trim());
    }
    if (query.categoryCode && query.categoryCode.trim()) {
      params.append("categoryCode", query.categoryCode.trim());
    }
    if (query.minPrice != null && !Number.isNaN(query.minPrice)) {
      params.append("minPrice", query.minPrice.toString());
    }
    if (query.maxPrice != null && !Number.isNaN(query.maxPrice)) {
      params.append("maxPrice", query.maxPrice.toString());
    }
    if (query.billingCycle) {
      params.append("billingCycle", query.billingCycle);
    }
    if (query.highlighted != null) {
      params.append("highlighted", String(query.highlighted));
    }
    if (query.page != null) {
      // Backend expects 0-indexed page
      params.append("page", Math.max(0, query.page - 1).toString());
    }
    if (query.size != null) {
      params.append("size", query.size.toString());
    }
    if (query.sort) {
      params.append("sort", query.sort);
    }

    const response = await publicAxiosClient.get<
      ApiResponse<PageResponse<PublicPlanSummary>>
    >(`/api/v1/plans?${params.toString()}`);

    return response.data.result;
  },

  async getPublicPlanBySlug(slug: string): Promise<PublicPlanDetail> {
    const response = await publicAxiosClient.get<
      ApiResponse<PublicPlanDetail>
    >(`/api/v1/plans/${encodeURIComponent(slug)}`);

    return response.data.result;
  },

  async getPublicCategories(): Promise<PublicCategory[]> {
    const response = await publicAxiosClient.get<
      ApiResponse<PublicCategory[]>
    >("/api/v1/service-categories");

    return response.data.result;
  },
};
