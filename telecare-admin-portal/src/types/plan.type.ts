export type PlanStatus = "DRAFT" | "PUBLISHED" | "HIDDEN";

export type BillingCycle = "DAY" | "MONTH" | "YEAR" | "ONE_TIME";

export interface ServiceCategory {
  id: string;
  code: string;
  name: string;
  description?: string | null;
  displayOrder?: number | null;
  active?: boolean;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: {
    id?: string | null;
    fullName?: string | null;
    username?: string | null;
    phone?: string | null;
    email?: string | null;
  } | string | null;
  updatedBy?: {
    id?: string | null;
    fullName?: string | null;
    username?: string | null;
    phone?: string | null;
    email?: string | null;
  } | string | null;
}

export interface PlanFeature {
  id?: string;
  code: string;
  name: string;
  value: string;
  unit?: string | null;
  displayOrder?: number | null;
}

export interface ServicePlanSummary {
  id: string;
  code: string;
  slug: string;
  name: string;
  summary?: string | null;
  price: number;
  currency: string;
  billingCycle: BillingCycle;
  status: PlanStatus;
  categoryId: string;
  categoryName?: string | null;
  categoryCode?: string | null;
  highlighted?: boolean;
  displayOrder?: number | null;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface ServicePlanDetail extends ServicePlanSummary {
  description?: string | null;
  features: PlanFeature[];
  createdBy?: {
    id?: string | null;
    fullName?: string | null;
    username?: string | null;
    phone?: string | null;
    email?: string | null;
  } | string | null;
  updatedBy?: {
    id?: string | null;
    fullName?: string | null;
    username?: string | null;
    phone?: string | null;
    email?: string | null;
  } | string | null;
}

export interface PlanFeatureRequest {
  code: string;
  name: string;
  value: string;
  unit?: string;
  displayOrder?: number;
}

export interface CreateServicePlanRequest {
  code: string;
  slug: string;
  name: string;
  summary?: string;
  description?: string;
  price: number;
  currency?: string;
  billingCycle: BillingCycle;
  status?: PlanStatus;
  categoryId: string;
  highlighted?: boolean;
  displayOrder?: number;
  features?: PlanFeatureRequest[];
}

export interface UpdateServicePlanRequest {
  code: string;
  slug: string;
  name: string;
  summary?: string;
  description?: string;
  price: number;
  currency?: string;
  billingCycle: BillingCycle;
  status?: PlanStatus;
  categoryId: string;
  highlighted?: boolean;
  displayOrder?: number;
  features?: PlanFeatureRequest[];
}

export interface UpdatePlanStatusRequest {
  status: PlanStatus;
}

export interface CreateCategoryRequest {
  code: string;
  name: string;
  description?: string;
  displayOrder?: number;
}

export interface UpdateCategoryRequest {
  code: string;
  name: string;
  description?: string;
  displayOrder?: number;
}

export interface ServicePlanQuery {
  keyword?: string;
  categoryId?: string;
  status?: PlanStatus | "ALL";
  active?: boolean | "ALL";
  page?: number;
  size?: number;
}
