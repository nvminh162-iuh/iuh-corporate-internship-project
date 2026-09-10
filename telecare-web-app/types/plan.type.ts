export type BillingCycle = "DAY" | "MONTH" | "YEAR" | "ONE_TIME";

export interface PublicCategory {
  id: string;
  code: string;
  name: string;
  description?: string | null;
  displayOrder?: number | null;
}

export interface PublicPlanFeature {
  code: string;
  name: string;
  value: string;
  unit?: string | null;
  displayOrder?: number | null;
}

export interface PublicPlanSummary {
  id: string;
  code: string;
  slug: string;
  name: string;
  summary?: string | null;
  price: number;
  currency: string;
  billingCycle: BillingCycle;
  highlighted: boolean;
  displayOrder?: number | null;
  category: PublicCategory;
  featureCount: number;
}

export interface PublicPlanDetail {
  id: string;
  code: string;
  slug: string;
  name: string;
  summary?: string | null;
  description?: string | null;
  price: number;
  currency: string;
  billingCycle: BillingCycle;
  highlighted: boolean;
  displayOrder?: number | null;
  category: PublicCategory;
  features: PublicPlanFeature[];
}

export interface PublicPlanQuery {
  keyword?: string;
  categoryCode?: string;
  minPrice?: number;
  maxPrice?: number;
  billingCycle?: BillingCycle | "";
  highlighted?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

export interface PageResponse<T> {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  result: T[];
}

export interface ApiResponse<T> {
  code?: number;
  message?: string;
  result: T;
}
