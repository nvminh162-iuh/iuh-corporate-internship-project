import { useState, useEffect } from "react";
import { Search, RotateCw, Plus, Filter } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type { PlanStatus, ServiceCategory } from "@/types/plan.type";

interface PlanToolbarProps {
  searchQuery: string;
  selectedCategory: string;
  selectedStatus: PlanStatus | "ALL";
  categories: ServiceCategory[];
  loading: boolean;
  canCreate: boolean;
  onSearchChange: (query: string) => void;
  onCategoryChange: (categoryId: string) => void;
  onStatusChange: (status: PlanStatus | "ALL") => void;
  onRefresh: () => void;
  onCreateClick: () => void;
}

export default function PlanToolbar({
  searchQuery,
  selectedCategory,
  selectedStatus,
  categories,
  loading,
  canCreate,
  onSearchChange,
  onCategoryChange,
  onStatusChange,
  onRefresh,
  onCreateClick,
}: PlanToolbarProps) {
  const [localSearch, setLocalSearch] = useState(searchQuery);

  // Sync external searchQuery state if reset externally
  useEffect(() => {
    setLocalSearch(searchQuery);
  }, [searchQuery]);

  // Debounce search change by 400ms
  useEffect(() => {
    const handler = setTimeout(() => {
      if (localSearch !== searchQuery) {
        onSearchChange(localSearch);
      }
    }, 400);

    return () => clearTimeout(handler);
  }, [localSearch, searchQuery, onSearchChange]);

  return (
    <div className="p-4 border-b border-border flex flex-col lg:flex-row lg:items-center justify-between gap-4">
      {/* Search & Filters Group */}
      <div className="flex flex-wrap items-center gap-3 flex-1 min-w-0">
        {/* Search Input */}
        <div className="relative flex-1 min-w-[200px] max-w-md">
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Tìm theo tên hoặc mã gói cước..."
            value={localSearch}
            onChange={(e) => setLocalSearch(e.target.value)}
            className="pl-9 pr-4 h-9 rounded-xl bg-muted/30 border-border text-xs focus:bg-background transition-colors"
          />
        </div>

        {/* Category Filter */}
        <div className="flex items-center gap-2">
          <Filter className="w-3.5 h-3.5 text-muted-foreground hidden sm:block" />
          <select
            value={selectedCategory}
            onChange={(e) => onCategoryChange(e.target.value)}
            className="h-9 px-3 rounded-xl border border-border bg-card text-xs font-medium text-foreground outline-none cursor-pointer hover:bg-muted/50 transition-colors"
          >
            <option value="ALL">Tất cả nhóm dịch vụ</option>
            {categories.map((cat) => (
              <option key={cat.id} value={cat.id}>
                {cat.name} ({cat.code})
              </option>
            ))}
          </select>
        </div>

        {/* Status Filter */}
        <div className="flex items-center gap-2">
          <select
            value={selectedStatus}
            onChange={(e) => onStatusChange(e.target.value as PlanStatus | "ALL")}
            className="h-9 px-3 rounded-xl border border-border bg-card text-xs font-medium text-foreground outline-none cursor-pointer hover:bg-muted/50 transition-colors"
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="DRAFT">Bản nháp</option>
            <option value="PUBLISHED">Đã xuất bản</option>
            <option value="HIDDEN">Tạm ẩn</option>
          </select>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="flex items-center gap-2 shrink-0">
        <Button
          variant="outline"
          size="sm"
          onClick={onRefresh}
          disabled={loading}
          className="rounded-xl gap-2 cursor-pointer text-xs"
        >
          <RotateCw className={`w-3.5 h-3.5 ${loading ? "animate-spin" : ""}`} />
          <span>Làm mới</span>
        </Button>

        {canCreate && (
          <Button
            size="sm"
            onClick={onCreateClick}
            className="rounded-xl gap-2 cursor-pointer text-xs font-semibold"
          >
            <Plus className="w-4 h-4" />
            <span>Tạo gói cước</span>
          </Button>
        )}
      </div>
    </div>
  );
}
