import { Eye, Edit, Trash2, Tag, Star, PackageCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import PlanStatusBadge from "./PlanStatusBadge";
import type { PlanStatus, ServicePlanSummary } from "@/types/plan.type";
import { formatBillingCycle, formatCurrency } from "@/utils/planUtils";
import { formatDate } from "@/utils/userUtils";

interface PlanTableProps {
  plans: ServicePlanSummary[];
  loading: boolean;
  page: number;
  size: number;
  canUpdate: boolean;
  canDelete: boolean;
  onViewDetails: (plan: ServicePlanSummary) => void;
  onEdit: (plan: ServicePlanSummary) => void;
  onStatusChange: (plan: ServicePlanSummary, newStatus: PlanStatus) => void;
  onDelete: (plan: ServicePlanSummary) => void;
}

export default function PlanTable({
  plans,
  loading,
  page,
  size,
  canUpdate,
  canDelete,
  onViewDetails,
  onEdit,
  onStatusChange,
  onDelete,
}: PlanTableProps) {
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

  if (plans.length === 0) {
    return (
      <div className="py-16 text-center select-none">
        <div className="w-14 h-14 mx-auto mb-3 rounded-2xl bg-muted/60 flex items-center justify-center text-muted-foreground">
          <PackageCheck className="w-7 h-7" />
        </div>
        <h3 className="text-sm font-bold text-foreground">Không tìm thấy gói cước nào</h3>
        <p className="text-xs text-muted-foreground mt-1 max-w-sm mx-auto">
          Thử thay đổi từ khóa tìm kiếm hoặc các bộ lọc trạng thái và nhóm dịch vụ.
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
            <th className="py-3 px-4">Mã & Tên gói cước</th>
            <th className="py-3 px-4">Nhóm dịch vụ</th>
            <th className="py-3 px-4">Giá & Chu kỳ</th>
            <th className="py-3 px-4">Trạng thái</th>
            <th className="py-3 px-4">Cập nhật gần nhất</th>
            <th className="py-3 px-4 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-border/60">
          {plans.map((plan, index) => {
            const stt = (page - 1) * size + index + 1;
            const isSoftDeleted = !plan.active;

            return (
              <tr
                key={plan.id}
                className={`hover:bg-muted/40 transition-colors ${
                  isSoftDeleted ? "opacity-60 bg-muted/20" : ""
                }`}
              >
                {/* STT */}
                <td className="py-3.5 px-4 text-center font-semibold text-muted-foreground">
                  {stt}
                </td>

                {/* Name & Code */}
                <td className="py-3.5 px-4">
                  <div className="flex items-start gap-2.5">
                    <div className="w-8 h-8 rounded-xl bg-primary/10 text-primary flex items-center justify-center shrink-0 mt-0.5 font-bold">
                      <Tag className="w-4 h-4" />
                    </div>
                    <div className="min-w-0">
                      <div className="flex items-center gap-1.5 flex-wrap">
                        <span className="font-bold text-foreground hover:text-primary transition-colors cursor-pointer" onClick={() => onViewDetails(plan)}>
                          {plan.name}
                        </span>
                        {plan.highlighted && (
                          <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 rounded-md bg-amber-500/15 text-amber-600 dark:text-amber-400 text-[10px] font-bold">
                            <Star className="w-3 h-3 fill-amber-500 text-amber-500" />
                            <span>Nổi bật</span>
                          </span>
                        )}
                        {isSoftDeleted && (
                          <span className="px-1.5 py-0.5 rounded-md bg-destructive/15 text-destructive text-[10px] font-bold">
                            Đã xóa
                          </span>
                        )}
                      </div>
                      <div className="text-[11px] font-mono text-muted-foreground mt-0.5">
                        {plan.code} • <span className="text-muted-foreground/80">{plan.slug}</span>
                      </div>
                    </div>
                  </div>
                </td>

                {/* Category */}
                <td className="py-3.5 px-4">
                  <span className="inline-flex items-center px-2.5 py-1 rounded-lg bg-muted/60 font-semibold text-foreground text-xs">
                    {plan.categoryName || plan.categoryCode || "Chưa gán"}
                  </span>
                </td>

                {/* Price & Billing Cycle */}
                <td className="py-3.5 px-4">
                  <div className="font-bold text-foreground text-xs">
                    {formatCurrency(plan.price, plan.currency)}
                  </div>
                  <div className="text-[11px] text-muted-foreground font-medium">
                    {formatBillingCycle(plan.billingCycle)}
                  </div>
                </td>

                {/* Status */}
                <td className="py-3.5 px-4">
                  <div className="flex items-center gap-2">
                    <PlanStatusBadge status={plan.status} />

                    {/* Quick status dropdown if allowed */}
                    {canUpdate && !isSoftDeleted && (
                      <select
                        value={plan.status}
                        onChange={(e) => onStatusChange(plan, e.target.value as PlanStatus)}
                        className="h-7 px-1.5 rounded-lg border border-border/80 bg-background text-[11px] font-semibold text-muted-foreground hover:text-foreground outline-none cursor-pointer"
                        title="Đổi nhanh trạng thái"
                      >
                        <option value="DRAFT">Nháp</option>
                        <option value="PUBLISHED">Xuất bản</option>
                        <option value="HIDDEN">Ẩn</option>
                      </select>
                    )}
                  </div>
                </td>

                {/* Last Updated */}
                <td className="py-3.5 px-4 text-muted-foreground text-[11px] font-medium">
                  {formatDate(plan.updatedAt || plan.createdAt)}
                </td>

                {/* Action Buttons */}
                <td className="py-3.5 px-4 text-right">
                  <div className="flex items-center justify-end gap-1">
                    {/* View Details */}
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      onClick={() => onViewDetails(plan)}
                      className="rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted cursor-pointer"
                      title="Xem chi tiết"
                    >
                      <Eye className="w-4 h-4" />
                    </Button>

                    {/* Edit Plan */}
                    {canUpdate && !isSoftDeleted && (
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={() => onEdit(plan)}
                        className="rounded-lg text-muted-foreground hover:text-primary hover:bg-primary/10 cursor-pointer"
                        title="Chỉnh sửa gói cước"
                      >
                        <Edit className="w-4 h-4" />
                      </Button>
                    )}

                    {/* Soft Delete Plan */}
                    {canDelete && !isSoftDeleted && (
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={() => onDelete(plan)}
                        className="rounded-lg text-muted-foreground hover:text-destructive hover:bg-destructive/10 cursor-pointer"
                        title="Xóa mềm gói cước"
                      >
                        <Trash2 className="w-4 h-4" />
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
