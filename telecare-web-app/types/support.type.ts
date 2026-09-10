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
  status: string;
  createdAt: string;
  updatedAt?: string;
}
