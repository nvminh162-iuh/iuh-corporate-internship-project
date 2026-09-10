import axiosClient from "@/lib/axios-client";
import type { ApiResponse, PageResponse } from "@/types/api.type";
import type {
  AddSupportRequestHistoryPayload,
  AssignSupportRequestPayload,
  SupportRequestAdminDetail,
  SupportRequestAdminQuery,
  SupportRequestAdminSummary,
  SupportRequestHistory,
  UpdateSupportRequestStatusPayload,
} from "@/types/support-admin.type";

export async function getAdminSupportRequests(
  query: SupportRequestAdminQuery = {},
): Promise<PageResponse<SupportRequestAdminSummary>> {
  const page = Math.max((query.page || 1) - 1, 0);
  const size = query.size || 10;

  const params: Record<string, string | number> = {
    page,
    size,
  };

  if (query.keyword?.trim()) {
    params.keyword = query.keyword.trim();
  }
  if (query.status) {
    params.status = query.status;
  }
  if (query.categoryCode) {
    params.categoryCode = query.categoryCode;
  }
  if (query.assignedTo) {
    params.assignedTo = query.assignedTo;
  }
  if (query.fromDate) {
    params.fromDate = query.fromDate;
  }
  if (query.toDate) {
    params.toDate = query.toDate;
  }
  if (query.sort) {
    params.sort = query.sort;
  }

  const { data } = await axiosClient.get<ApiResponse<PageResponse<SupportRequestAdminSummary>>>(
    "/api/v1/admin/support-requests",
    { params },
  );

  return data.result;
}

export async function getAdminSupportRequestById(
  id: string,
): Promise<SupportRequestAdminDetail> {
  const { data } = await axiosClient.get<ApiResponse<SupportRequestAdminDetail>>(
    `/api/v1/admin/support-requests/${id}`,
  );
  return data.result;
}

export async function receiveSupportRequest(
  id: string,
): Promise<SupportRequestAdminDetail> {
  const { data } = await axiosClient.patch<ApiResponse<SupportRequestAdminDetail>>(
    `/api/v1/admin/support-requests/${id}/receive`,
  );
  return data.result;
}

export async function assignSupportRequest(
  id: string,
  payload: AssignSupportRequestPayload,
): Promise<SupportRequestAdminDetail> {
  const { data } = await axiosClient.patch<ApiResponse<SupportRequestAdminDetail>>(
    `/api/v1/admin/support-requests/${id}/assign`,
    payload,
  );
  return data.result;
}

export async function updateSupportRequestStatus(
  id: string,
  payload: UpdateSupportRequestStatusPayload,
): Promise<SupportRequestAdminDetail> {
  const { data } = await axiosClient.patch<ApiResponse<SupportRequestAdminDetail>>(
    `/api/v1/admin/support-requests/${id}/status`,
    payload,
  );
  return data.result;
}

export async function addSupportRequestHistory(
  id: string,
  payload: AddSupportRequestHistoryPayload,
): Promise<SupportRequestHistory> {
  const { data } = await axiosClient.post<ApiResponse<SupportRequestHistory>>(
    `/api/v1/admin/support-requests/${id}/histories`,
    payload,
  );
  return data.result;
}

export async function getSupportRequestHistories(
  id: string,
): Promise<SupportRequestHistory[]> {
  const { data } = await axiosClient.get<ApiResponse<SupportRequestHistory[]>>(
    `/api/v1/admin/support-requests/${id}/histories`,
  );
  return data.result;
}

export async function getSupportCategories(): Promise<{ code: string; name: string }[]> {
  const { data } = await axiosClient.get<ApiResponse<{ code: string; name: string }[]>>(
    "/api/v1/support-categories",
  );
  return data.result;
}
