import { useState, useEffect, useCallback } from "react";
import { toast } from "sonner";
import { FolderTree } from "lucide-react";
import { useAuth } from "@/features/auth/useAuth";
import {
  getAdminCategories,
  setAdminCategoryActive,
} from "@/services/admin-plan.service";
import type { ServiceCategory } from "@/types/plan.type";

import CategoryToolbar from "@/components/categories/CategoryToolbar";
import CategoryTable from "@/components/categories/CategoryTable";
import CategoryFormModal from "@/components/categories/CategoryFormModal";
import UserPagination from "@/components/users/UserPagination";
import { getApiErrorMessage } from "@/utils/userUtils";

export default function CategoriesPage() {
  const { profile } = useAuth();
  const isAdmin = profile?.role === "ADMIN" || true;

  // List & Pagination State
  const [categories, setCategories] = useState<ServiceCategory[]>([]);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  // Filters State
  const [searchQuery, setSearchQuery] = useState("");

  // Modals
  const [editingCategory, setEditingCategory] = useState<ServiceCategory | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);

  // Fetch Categories List
  const fetchCategories = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getAdminCategories(page, size);
      let list = data.result || [];
      if (searchQuery.trim()) {
        const q = searchQuery.trim().toLowerCase();
        list = list.filter(
          (c) =>
            c.code.toLowerCase().includes(q) ||
            c.name.toLowerCase().includes(q) ||
            (c.description || "").toLowerCase().includes(q),
        );
      }
      setCategories(list);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || (data.result?.length ?? 0));
    } catch (error) {
      console.error("Failed to load service categories:", error);
      toast.error("Không thể tải danh sách nhóm dịch vụ. Vui lòng thử lại!");
    } finally {
      setLoading(false);
    }
  }, [page, size, searchQuery]);

  useEffect(() => {
    fetchCategories();
  }, [fetchCategories]);

  // Enable / Disable toggle
  const handleToggleActive = async (category: ServiceCategory) => {
    const currentActive = category.active !== false;
    const targetActive = !currentActive;
    try {
      await setAdminCategoryActive(category.id, targetActive);
      toast.success(
        targetActive
          ? `Đã kích hoạt nhóm dịch vụ ${category.name}`
          : `Đã tạm ẩn nhóm dịch vụ ${category.name}`,
      );
      fetchCategories();
    } catch (err) {
      toast.error(getApiErrorMessage(err, "Không thể thay đổi trạng thái nhóm dịch vụ này"));
    }
  };

  const handleSearchChange = (query: string) => {
    setSearchQuery(query);
    setPage(1);
  };

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6 animate-in fade-in duration-200">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 select-none">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-2xl bg-primary/10 text-primary flex items-center justify-center font-bold shrink-0">
            <FolderTree className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-foreground">Quản lý nhóm dịch vụ</h1>
            <p className="text-xs text-muted-foreground mt-0.5">
              Phân loại danh mục dịch vụ viễn thông (Internet, Mobile Data, Truyền hình, v.v.)
            </p>
          </div>
        </div>
      </div>

      {/* Main Card Container */}
      <div className="bg-card border border-border rounded-3xl shadow-sm overflow-hidden">
        {/* Toolbar */}
        <CategoryToolbar
          searchQuery={searchQuery}
          loading={loading}
          canCreate={isAdmin}
          onSearchChange={handleSearchChange}
          onRefresh={fetchCategories}
          onCreateClick={() => {
            setEditingCategory(null);
            setIsFormOpen(true);
          }}
        />

        {/* Category Table */}
        <CategoryTable
          categories={categories}
          loading={loading}
          page={page}
          size={size}
          canUpdate={isAdmin}
          canDelete={isAdmin}
          onEdit={(cat) => {
            setEditingCategory(cat);
            setIsFormOpen(true);
          }}
          onToggleActive={handleToggleActive}
        />

        {/* Pagination */}
        {totalElements > 0 && (
          <UserPagination
            page={page}
            size={size}
            totalPages={totalPages}
            totalElements={totalElements}
            loading={loading}
            itemLabel="nhóm dịch vụ"
            onPageChange={setPage}
            onSizeChange={setSize}
          />
        )}
      </div>

      {/* Create / Edit Modal */}
      <CategoryFormModal
        isOpen={isFormOpen}
        editingCategory={editingCategory}
        onClose={() => {
          setIsFormOpen(false);
          setEditingCategory(null);
        }}
        onSuccess={fetchCategories}
      />
    </div>
  );
}
