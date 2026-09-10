import { useState, useEffect, useCallback } from "react";
import { toast } from "sonner";
import { Package } from "lucide-react";
import { useAuth } from "@/features/auth/useAuth";
import {
  getAllAdminCategories,
  getAdminPlans,
  updateAdminPlanStatus,
} from "@/services/admin-plan.service";
import type {
  PlanStatus,
  ServiceCategory,
  ServicePlanSummary,
} from "@/types/plan.type";

import PlanToolbar from "@/components/plans/PlanToolbar";
import PlanTable from "@/components/plans/PlanTable";
import PlanFormModal from "@/components/plans/PlanFormModal";
import PlanDetailsModal from "@/components/plans/PlanDetailsModal";
import DeletePlanDialog from "@/components/plans/DeletePlanDialog";
import UserPagination from "@/components/users/UserPagination";
import { getApiErrorMessage } from "@/utils/userUtils";

export default function PlansPage() {
  const { profile } = useAuth();
  const isAdmin = profile?.role === "ADMIN" || true; // Admin portal default access

  // Categories list for dropdown
  const [categories, setCategories] = useState<ServiceCategory[]>([]);

  // List & Pagination State
  const [plans, setPlans] = useState<ServicePlanSummary[]>([]);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  // Filters State
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedCategory, setSelectedCategory] = useState<string>("ALL");
  const [selectedStatus, setSelectedStatus] = useState<PlanStatus | "ALL">("ALL");

  // Modals & Selected Plan State
  const [selectedPlanForDetails, setSelectedPlanForDetails] = useState<ServicePlanSummary | null>(null);
  const [isDetailsOpen, setIsDetailsOpen] = useState(false);

  const [editingPlan, setEditingPlan] = useState<ServicePlanSummary | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);

  const [deletingPlan, setDeletingPlan] = useState<ServicePlanSummary | null>(null);
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);

  // Fetch Categories for Dropdown
  const fetchCategories = useCallback(async () => {
    try {
      const data = await getAllAdminCategories();
      setCategories(data || []);
    } catch {
      // Fallback silently if category endpoint fails
    }
  }, []);

  // Fetch Plans List
  const fetchPlans = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getAdminPlans({
        page,
        size,
        keyword: searchQuery,
        categoryId: selectedCategory,
        status: selectedStatus,
      });
      setPlans(data.result || []);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || (data.result?.length ?? 0));
    } catch (error) {
      console.error("Failed to load service plans:", error);
      toast.error("Không thể tải danh sách gói cước. Vui lòng thử lại!");
    } finally {
      setLoading(false);
    }
  }, [page, size, searchQuery, selectedCategory, selectedStatus]);

  useEffect(() => {
    fetchCategories();
  }, [fetchCategories]);

  useEffect(() => {
    fetchPlans();
  }, [fetchPlans]);

  // Handle Quick Status Change
  const handleQuickStatusChange = async (plan: ServicePlanSummary, newStatus: PlanStatus) => {
    try {
      await updateAdminPlanStatus(plan.id, newStatus);
      toast.success(`Đã cập nhật trạng thái gói ${plan.code} thành ${newStatus}`);
      fetchPlans();
    } catch (err) {
      toast.error(getApiErrorMessage(err, "Không thể chuyển trạng thái gói cước"));
    }
  };

  // Filter Reset Helpers
  const handleSearchChange = (query: string) => {
    setSearchQuery(query);
    setPage(1);
  };

  const handleCategoryChange = (categoryId: string) => {
    setSelectedCategory(categoryId);
    setPage(1);
  };

  const handleStatusChange = (status: PlanStatus | "ALL") => {
    setSelectedStatus(status);
    setPage(1);
  };

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6 animate-in fade-in duration-200">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 select-none">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-2xl bg-primary/10 text-primary flex items-center justify-center font-bold shrink-0">
            <Package className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-foreground">Quản lý gói cước viễn thông</h1>
            <p className="text-xs text-muted-foreground mt-0.5">
              Danh sách, tạo mới, cấu hình giá, chu kỳ thanh toán và các ưu đãi đặc điểm gói cước
            </p>
          </div>
        </div>
      </div>

      {/* Main Card Container */}
      <div className="bg-card border border-border rounded-3xl shadow-sm overflow-hidden">
        {/* Toolbar */}
        <PlanToolbar
          searchQuery={searchQuery}
          selectedCategory={selectedCategory}
          selectedStatus={selectedStatus}
          categories={categories}
          loading={loading}
          canCreate={isAdmin}
          onSearchChange={handleSearchChange}
          onCategoryChange={handleCategoryChange}
          onStatusChange={handleStatusChange}
          onRefresh={fetchPlans}
          onCreateClick={() => {
            setEditingPlan(null);
            setIsFormOpen(true);
          }}
        />

        {/* Plan Table */}
        <PlanTable
          plans={plans}
          loading={loading}
          page={page}
          size={size}
          canUpdate={isAdmin}
          canDelete={isAdmin}
          onViewDetails={(plan) => {
            setSelectedPlanForDetails(plan);
            setIsDetailsOpen(true);
          }}
          onEdit={(plan) => {
            setEditingPlan(plan);
            setIsFormOpen(true);
          }}
          onStatusChange={handleQuickStatusChange}
          onDelete={(plan) => {
            setDeletingPlan(plan);
            setIsDeleteOpen(true);
          }}
        />

        {/* Pagination */}
        {totalElements > 0 && (
          <UserPagination
            page={page}
            size={size}
            totalPages={totalPages}
            totalElements={totalElements}
            loading={loading}
            itemLabel="gói cước"
            onPageChange={setPage}
            onSizeChange={setSize}
          />
        )}
      </div>

      {/* Details Modal */}
      <PlanDetailsModal
        isOpen={isDetailsOpen}
        selectedPlan={selectedPlanForDetails}
        canUpdate={isAdmin}
        onClose={() => {
          setIsDetailsOpen(false);
          setSelectedPlanForDetails(null);
        }}
        onEdit={(plan) => {
          setEditingPlan(plan);
          setIsFormOpen(true);
        }}
      />

      {/* Create / Edit Form Modal */}
      <PlanFormModal
        isOpen={isFormOpen}
        editingPlan={editingPlan}
        categories={categories}
        onClose={() => {
          setIsFormOpen(false);
          setEditingPlan(null);
        }}
        onSuccess={fetchPlans}
      />

      {/* Soft Delete Dialog */}
      <DeletePlanDialog
        isOpen={isDeleteOpen}
        plan={deletingPlan}
        onClose={() => {
          setIsDeleteOpen(false);
          setDeletingPlan(null);
        }}
        onSuccess={fetchPlans}
      />
    </div>
  );
}
