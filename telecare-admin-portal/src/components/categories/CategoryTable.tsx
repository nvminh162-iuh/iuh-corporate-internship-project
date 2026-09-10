import { Edit, FolderTree, Power } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import type { ServiceCategory } from "@/types/plan.type";
import { formatDate } from "@/utils/userUtils";

interface CategoryTableProps {
  categories: ServiceCategory[];
  loading: boolean;
  page: number;
  size: number;
  canUpdate: boolean;
  canDelete: boolean;
  onEdit: (category: ServiceCategory) => void;
  onToggleActive: (category: ServiceCategory) => void;
}

export default function CategoryTable({
  categories,
  loading,
  page,
  size,
  canUpdate,
  canDelete,
  onEdit,
  onToggleActive,
}: CategoryTableProps) {
  if (loading) {
    return (
      <div className="p-6 space-y-4">
        {Array.from({ length: 5 }).map((_, idx) => (
          <div key={idx} className="flex items-center gap-4">
            <Skeleton className="h-10 w-10 rounded-lg shrink-0" />
            <div className="space-y-2 flex-1">
              <Skeleton className="h-4 w-1/3" />
              <Skeleton className="h-3 w-1/4" />
            </div>
            <Skeleton className="h-8 w-24 rounded-lg shrink-0" />
          </div>
        ))}
      </div>
    );
  }

  if (categories.length === 0) {
    return (
      <div className="py-16 text-center select-none">
        <div className="w-14 h-14 mx-auto mb-3 rounded-2xl bg-muted/60 flex items-center justify-center text-muted-foreground">
          <FolderTree className="w-7 h-7" />
        </div>
        <h3 className="text-sm font-bold text-foreground">Không tìm thấy nhóm dịch vụ nào</h3>
        <p className="text-xs text-muted-foreground mt-1 max-w-sm mx-auto">
          Thử thay đổi từ khóa tìm kiếm hoặc tạo thêm nhóm dịch vụ mới.
        </p>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-xs border-collapse">
        <thead>
          <tr className="border-b border-border bg-muted/30 text-muted-foreground font-semibold select-none">
            <th className="py-3 px-4 w-12 text-center">STT</th>
            <th className="py-3 px-4">Mã nhóm</th>
            <th className="py-3 px-4">Tên nhóm dịch vụ</th>
            <th className="py-3 px-4">Mô tả</th>
            <th className="py-3 px-4 text-center">Thứ tự</th>
            <th className="py-3 px-4">Trạng thái</th>
            <th className="py-3 px-4">Ngày tạo</th>
            <th className="py-3 px-4 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-border/60">
          {categories.map((category, index) => {
            const stt = (page - 1) * size + index + 1;
            const isActive = category.active !== false;

            return (
              <tr
                key={category.id}
                className={`hover:bg-muted/40 transition-colors ${
                  !isActive ? "opacity-60 bg-muted/20" : ""
                }`}
              >
                {/* STT */}
                <td className="py-3.5 px-4 text-center font-semibold text-muted-foreground">
                  {stt}
                </td>

                {/* Code */}
                <td className="py-3.5 px-4 font-mono font-bold text-foreground">
                  {category.code}
                </td>

                {/* Name */}
                <td className="py-3.5 px-4 font-bold text-foreground">
                  {category.name}
                </td>

                {/* Description */}
                <td className="py-3.5 px-4 text-muted-foreground max-w-xs truncate">
                  {category.description || "-"}
                </td>

                {/* Display Order */}
                <td className="py-3.5 px-4 text-center font-semibold text-muted-foreground">
                  {category.displayOrder ?? 0}
                </td>

                {/* Status */}
                <td className="py-3.5 px-4">
                  {isActive ? (
                    <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-bold border border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400">
                      <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                      <span>Hoạt động</span>
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-bold border border-slate-500/30 bg-slate-500/10 text-slate-600 dark:text-slate-400">
                      <span className="w-1.5 h-1.5 rounded-full bg-slate-400" />
                      <span>Tạm ẩn</span>
                    </span>
                  )}
                </td>

                {/* Created Date */}
                <td className="py-3.5 px-4 text-muted-foreground text-[11px] font-medium">
                  {formatDate(category.createdAt)}
                </td>

                {/* Actions */}
                <td className="py-3.5 px-4 text-right">
                  <div className="flex items-center justify-end gap-1">
                    {/* Edit */}
                    {canUpdate && (
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={() => onEdit(category)}
                        className="rounded-lg text-muted-foreground hover:text-primary hover:bg-primary/10 cursor-pointer"
                        title="Chỉnh sửa nhóm dịch vụ"
                      >
                        <Edit className="w-4 h-4" />
                      </Button>
                    )}

                    {/* Enable / Disable */}
                    {canDelete && (
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={() => onToggleActive(category)}
                        className={`rounded-lg cursor-pointer ${
                          isActive
                            ? "text-muted-foreground hover:text-destructive hover:bg-destructive/10"
                            : "text-muted-foreground hover:text-emerald-600 hover:bg-emerald-500/10"
                        }`}
                        title={isActive ? "Tạm ẩn nhóm" : "Kích hoạt nhóm"}
                      >
                        <Power className="w-4 h-4" />
                      </Button>
                    )}
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
