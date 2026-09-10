import publicAxiosClient from "@/lib/public-axios-client";
import axiosClient from "@/lib/axios-client";
import type { ApiResponse } from "@/types/plan.type";
import type {
  CreateSupportRequestInput,
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
};
