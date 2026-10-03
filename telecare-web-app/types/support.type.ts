export type SupportRequestStatus =
  | "NEW"
  | "RECEIVED"
  | "IN_PROGRESS"
  | "WAITING_CUSTOMER"
  | "COMPLETED"
  | "CLOSED";

export interface SupportCategory {
  id: string;
  code: string;
  name: string;
  description?: string;
  displayOrder?: number;
}

export interface CreateSupportRequestInput {
  categoryCode: string;
  servicePlanId?: string;
  subject: string;
  content: string;
  contactPhone?: string;
  contactEmail?: string;
}

export interface SupportRequest {
  id: string;
  ticketCode: string;
  customerId: string;
  category: SupportCategory;
  servicePlan?: {
    id: string;
    code: string;
    name: string;
  };
  subject: string;
  content: string;
  contactPhone?: string;
  contactEmail?: string;
  status: SupportRequestStatus | string;
  createdAt: string;
  updatedAt?: string;
}

export interface CustomerSupportRequestSummary {
  id: string;
  ticketCode: string;
  category: SupportCategory;
  subject: string;
  status: SupportRequestStatus | string;
  createdAt: string;
  updatedAt?: string;
}

export interface CustomerSupportRequestDetail {
  id: string;
  ticketCode: string;
  category: SupportCategory;
  servicePlan?: {
    id: string;
    code: string;
    name: string;
  };
  subject: string;
  content: string;
  contactPhone?: string;
  contactEmail?: string;
  status: SupportRequestStatus | string;
  resolution?: string;
  receivedAt?: string;
  completedAt?: string;
  closedAt?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface CustomerSupportRequestHistory {
  id: string;
  fromStatus?: SupportRequestStatus | string;
  toStatus: SupportRequestStatus | string;
  action: string;
  message: string;
  createdAt: string;
}

export interface CustomerSupportRequestQuery {
  page?: number;
  size?: number;
  status?: string;
  keyword?: string;
  fromDate?: string;
  toDate?: string;
  sort?: string;
}
