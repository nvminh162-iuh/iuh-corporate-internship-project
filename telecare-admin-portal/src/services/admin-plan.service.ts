import axiosClient from "@/lib/axios-client";
import type { ApiResponse, PageResponse } from "@/types/api.type";
import type {
  CreateCategoryRequest,
  CreateServicePlanRequest,
  PlanStatus,
  ServiceCategory,
  ServicePlanDetail,
  ServicePlanQuery,
  ServicePlanSummary,
  UpdateCategoryRequest,
  UpdateServicePlanRequest,
} from "@/types/plan.type";

export async function getAdminPlans(
  query: ServicePlanQuery = {},
): Promise<PageResponse<ServicePlanSummary>> {
  const page = Math.max((query.page || 1) - 1, 0);
  const size = query.size || 10;

  const params: Record<string, string | number | boolean> = {
    page,
    size,
  };

  if (query.keyword && query.keyword.trim()) {
    params.keyword = query.keyword.trim();
  }
  if (query.categoryId && query.categoryId !== "ALL") {
    params.categoryId = query.categoryId;
  }
  if (query.status && query.status !== "ALL") {
    params.status = query.status;
  }
  if (query.active !== undefined && query.active !== "ALL") {
    params.active = query.active;
  }

  const { data } = await axiosClient.get<ApiResponse<PageResponse<ServicePlanSummary>>>(
    "/api/v1/admin/plans",
    { params },
  );

  return data.result;
}

export async function getAdminPlanById(id: string): Promise<ServicePlanDetail> {
  const { data } = await axiosClient.get<ApiResponse<ServicePlanDetail>>(
    `/api/v1/admin/plans/${id}`,
  );
  return data.result;
}

export async function createAdminPlan(
  request: CreateServicePlanRequest,
): Promise<ServicePlanDetail> {
  const { data } = await axiosClient.post<ApiResponse<ServicePlanDetail>>(
    "/api/v1/admin/plans",
    request,
  );
  return data.result;
}

export async function updateAdminPlan(
  id: string,
  request: UpdateServicePlanRequest,
): Promise<ServicePlanDetail> {
  const { data } = await axiosClient.put<ApiResponse<ServicePlanDetail>>(
    `/api/v1/admin/plans/${id}`,
    request,
  );
  return data.result;
}

export async function updateAdminPlanStatus(
  id: string,
  status: PlanStatus,
): Promise<ServicePlanDetail> {
  const { data } = await axiosClient.patch<ApiResponse<ServicePlanDetail>>(
    `/api/v1/admin/plans/${id}/status`,
    { status },
  );
  return data.result;
}

export async function deleteAdminPlan(id: string): Promise<void> {
  await axiosClient.delete(`/api/v1/admin/plans/${id}`);
}

export async function getAdminCategories(
  page = 1,
  size = 10,
): Promise<PageResponse<ServiceCategory>> {
  const { data } = await axiosClient.get<ApiResponse<PageResponse<ServiceCategory>>>(
    "/api/v1/admin/service-categories",
    { params: { page: Math.max(page - 1, 0), size } },
  );
  return data.result;
}

export async function getAllAdminCategories(): Promise<ServiceCategory[]> {
  const { data } = await axiosClient.get<ApiResponse<ServiceCategory[]>>(
    "/api/v1/admin/service-categories/all",
  );
  return data.result;
}

export async function createAdminCategory(
  request: CreateCategoryRequest,
): Promise<ServiceCategory> {
  const { data } = await axiosClient.post<ApiResponse<ServiceCategory>>(
    "/api/v1/admin/service-categories",
    request,
  );
  return data.result;
}

export async function updateAdminCategory(
  id: string,
  request: UpdateCategoryRequest,
): Promise<ServiceCategory> {
  const { data } = await axiosClient.put<ApiResponse<ServiceCategory>>(
    `/api/v1/admin/service-categories/${id}`,
    request,
  );
  return data.result;
}

export async function setAdminCategoryActive(
  id: string,
  active: boolean,
): Promise<void> {
  await axiosClient.patch(
    `/api/v1/admin/service-categories/${id}/${active ? "enable" : "disable"}`,
  );
}
