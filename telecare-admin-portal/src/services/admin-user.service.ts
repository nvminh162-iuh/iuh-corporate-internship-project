import axiosClient from "@/lib/axios-client";
import type { ApiResponse, PageResponse } from "@/types/api.type";
import type {
  AdminUser,
  CreateAdminUserRequest,
  UpdateAdminUserRequest,
} from "@/types/user.type";

export interface GetAdminUsersParams {
  page?: number;
  size?: number;
  keyword?: string;
  active?: boolean;
  roleId?: string;
}

const pendingListRequests = new Map<string, Promise<PageResponse<AdminUser>>>();

async function requestUsers(params: GetAdminUsersParams): Promise<PageResponse<AdminUser>> {
  const { page = 1, size = 10, keyword, active, roleId } = params;
  const queryParams: Record<string, string | number | boolean> = {
    page: Math.max(page - 1, 0),
    size,
  };
  if (keyword && keyword.trim()) {
    queryParams.keyword = keyword.trim();
  }
  if (typeof active === "boolean") {
    queryParams.active = active;
  }
  if (roleId && roleId.trim() && roleId !== "ALL") {
    queryParams.roleId = roleId.trim();
  }

  const { data } = await axiosClient.get<ApiResponse<PageResponse<AdminUser>>>(
    "/api/v1/admin/users",
    { params: queryParams },
  );

  return data.result;
}

export function getAdminUsers(params: GetAdminUsersParams | number = 1, size = 10): Promise<PageResponse<AdminUser>> {
  const normalized: GetAdminUsersParams =
    typeof params === "number" ? { page: params, size } : params;
  const key = `${normalized.page || 1}:${normalized.size || 10}:${normalized.keyword || ""}:${normalized.active}:${normalized.roleId || ""}`;
  const pending = pendingListRequests.get(key);

  if (pending) return pending;

  const request = requestUsers(normalized).finally(() => {
    pendingListRequests.delete(key);
  });

  pendingListRequests.set(key, request);
  return request;
}

export async function getAdminUserById(userId: string): Promise<AdminUser> {
  const { data } = await axiosClient.get<ApiResponse<AdminUser>>(
    `/api/v1/admin/users/${userId}`,
  );
  return data.result;
}

export async function createAdminUser(request: CreateAdminUserRequest): Promise<AdminUser> {
  const { data } = await axiosClient.post<ApiResponse<AdminUser>>(
    "/api/v1/admin/users",
    request,
  );
  return data.result;
}

export async function updateAdminUser(
  userId: string,
  request: UpdateAdminUserRequest,
): Promise<AdminUser> {
  const { data } = await axiosClient.put<ApiResponse<AdminUser>>(
    `/api/v1/admin/users/${userId}`,
    request,
  );
  return data.result;
}

export async function setAdminUserActive(userId: string, active: boolean): Promise<void> {
  await axiosClient.patch(
    `/api/v1/admin/users/${userId}/${active ? "enable" : "disable"}`,
  );
}

export async function resendAdminUserInvitation(userId: string): Promise<void> {
  await axiosClient.post(`/api/v1/admin/users/${userId}/resend-invitation`);
}
