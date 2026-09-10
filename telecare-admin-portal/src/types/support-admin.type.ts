export type SupportRequestStatus =
  | "NEW"
  | "RECEIVED"
  | "IN_PROGRESS"
  | "WAITING_CUSTOMER"
  | "COMPLETED"
  | "CLOSED";

export interface UserSummaryInfo {
  id: string;
  username?: string;
  firstName?: string;
  lastName?: string;
  fullName?: string;
  email?: string;
  phone?: string;
}

export interface SupportCategoryInfo {
  id?: string;
  code: string;
  name: string;
  description?: string;
}

export interface ServicePlanInfo {
  id: string;
  code: string;
  name: string;
}

export interface SupportRequestAdminSummary {
  id: string;
  ticketCode: string;
  subject: string;
  categoryCode?: string;
  categoryName?: string;
  category?: SupportCategoryInfo;
  status: SupportRequestStatus;
  customerId: string;
  customerName?: string;
  customerPhone?: string;
  customerEmail?: string;
  customer?: UserSummaryInfo;
  contactPhone?: string;
  contactEmail?: string;
  assignedToId?: string;
  assignedToName?: string;
  assignedTo?: UserSummaryInfo;
  servicePlan?: ServicePlanInfo;
  createdAt: string;
  receivedAt?: string;
  assignedAt?: string;
  completedAt?: string;
  closedAt?: string;
}

export interface SupportRequestHistory {
  id: string;
  supportRequestId: string;
  fromStatus?: SupportRequestStatus;
  toStatus: SupportRequestStatus;
  actorId: string;
  actorName?: string;
  actor?: UserSummaryInfo;
  action?: string;
  note: string;
  createdAt: string;
}

export interface SupportRequestAdminDetail extends SupportRequestAdminSummary {
  content: string;
  resolution?: string;
  createdByName?: string;
  histories?: SupportRequestHistory[];
}

export interface SupportRequestAdminQuery {
  keyword?: string;
  status?: SupportRequestStatus | "";
  categoryCode?: string;
  assignedTo?: string;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface AssignSupportRequestPayload {
  assignedTo: string;
  note?: string;
}

export interface UpdateSupportRequestStatusPayload {
  status: SupportRequestStatus;
  note?: string;
  resolution?: string;
}

export interface AddSupportRequestHistoryPayload {
  note: string;
}
