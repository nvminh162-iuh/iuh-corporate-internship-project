import { useState, useEffect } from "react";
import { X, Tag, Star, Calendar, ShieldCheck, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import PlanStatusBadge from "./PlanStatusBadge";
import type { ServicePlanDetail, ServicePlanSummary } from "@/types/plan.type";
import { getAdminPlanById } from "@/services/admin-plan.service";
import { formatBillingCycle, formatCurrency } from "@/utils/planUtils";
import { formatAuditActor, formatDate } from "@/utils/userUtils";

interface PlanDetailsModalProps {
  isOpen: boolean;
  selectedPlan: ServicePlanSummary | null;
  onClose: () => void;
  onEdit?: (plan: ServicePlanSummary) => void;
  canUpdate?: boolean;
}

export default function PlanDetailsModal({
  isOpen,
  selectedPlan,
  onClose,
  onEdit,
  canUpdate,
}: PlanDetailsModalProps) {
  const [detail, setDetail] = useState<ServicePlanDetail | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!isOpen || !selectedPlan) {
      setDetail(null);
      return;
    }
    setLoading(true);
    getAdminPlanById(selectedPlan.id)
      .then((data) => setDetail(data))
      .catch(() => setDetail(null))
      .finally(() => setLoading(false));
  }, [isOpen, selectedPlan]);

  if (!isOpen || !selectedPlan) return null;

  const currentData = detail || selectedPlan;

  return (
    <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto animate-in fade-in duration-150">
      <div
        className="relative w-full max-w-2xl bg-card border border-border rounded-3xl shadow-2xl my-8 overflow-hidden select-none"
        role="dialog"
        aria-modal="true"
      >
        {/* Header */}
        <div className="p-5 border-b border-border/80 flex items-center justify-between bg-muted/30">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-primary/10 text-primary flex items-center justify-center font-bold shrink-0">
              <Tag className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2 flex-wrap">
                <h2 className="text-base font-bold text-foreground">{currentData.name}</h2>
                <PlanStatusBadge status={currentData.status} />
                {currentData.highlighted && (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-amber-500/15 text-amber-600 dark:text-amber-400 text-xs font-bold">
                    <Star className="w-3 h-3 fill-amber-500 text-amber-500" />
                    <span>Nổi bật</span>
                  </span>
                )}
              </div>
              <p className="text-xs font-mono text-muted-foreground mt-0.5">
                {currentData.code} • {currentData.slug}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="p-2 rounded-xl text-muted-foreground hover:text-foreground hover:bg-muted/70 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        {loading ? (
          <div className="p-12 text-center text-muted-foreground flex flex-col items-center gap-3">
            <Loader2 className="w-7 h-7 animate-spin text-primary" />
            <span className="text-xs">Đang tải thông tin chi tiết...</span>
          </div>
        ) : (
          <div className="p-6 space-y-6 max-h-[75vh] overflow-y-auto">
            {/* Price & Billing */}
            <div className="p-4 rounded-2xl bg-muted/40 border border-border flex flex-wrap items-center justify-between gap-4">
              <div>
                <span className="text-xs text-muted-foreground font-medium block">Giá cước</span>
                <span className="text-xl font-extrabold text-primary">
                  {formatCurrency(currentData.price, currentData.currency)}
                </span>
              </div>
              <div>
                <span className="text-xs text-muted-foreground font-medium block">Chu kỳ</span>
                <span className="text-sm font-bold text-foreground">
                  {formatBillingCycle(currentData.billingCycle)}
                </span>
              </div>
              <div>
                <span className="text-xs text-muted-foreground font-medium block">Nhóm dịch vụ</span>
                <span className="text-sm font-bold text-foreground">
                  {currentData.categoryName || currentData.categoryCode || "N/A"}
                </span>
              </div>
            </div>

            {/* Summary */}
            {currentData.summary && (
              <div className="space-y-1">
                <span className="text-xs font-bold text-foreground block">Tóm tắt ngắn</span>
                <p className="text-xs text-muted-foreground bg-card p-3 rounded-xl border border-border">
                  {currentData.summary}
                </p>
              </div>
            )}

            {/* Description */}
            {detail?.description && (
              <div className="space-y-1">
                <span className="text-xs font-bold text-foreground block">Mô tả chi tiết</span>
                <p className="text-xs text-muted-foreground whitespace-pre-wrap bg-card p-3 rounded-xl border border-border leading-relaxed">
                  {detail.description}
                </p>
              </div>
            )}

            {/* Features Table */}
            <div className="space-y-2">
              <span className="text-xs font-bold text-foreground block">
                Danh sách đặc điểm & ưu đãi ({detail?.features?.length || 0})
              </span>
              {detail?.features && detail.features.length > 0 ? (
                <div className="overflow-x-auto rounded-2xl border border-border">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="border-b border-border bg-muted/30 text-muted-foreground font-semibold">
                        <th className="py-2.5 px-3">Mã</th>
                        <th className="py-2.5 px-3">Tên đặc điểm</th>
                        <th className="py-2.5 px-3">Giá trị</th>
                        <th className="py-2.5 px-3">Đơn vị</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-border/60">
                      {detail.features.map((f, idx) => (
                        <tr key={f.id || idx} className="hover:bg-muted/30">
                          <td className="py-2 px-3 font-mono font-bold text-foreground">{f.code}</td>
                          <td className="py-2 px-3 font-medium text-foreground">{f.name}</td>
                          <td className="py-2 px-3 font-semibold text-primary">{f.value}</td>
                          <td className="py-2 px-3 text-muted-foreground">{f.unit || "-"}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="p-4 text-center text-xs text-muted-foreground rounded-xl border border-dashed border-border bg-muted/20">
                  Gói cước chưa có thông tin đặc điểm.
                </div>
              )}
            </div>

            {/* Audit Metadata */}
            <div className="p-4 rounded-2xl bg-muted/20 border border-border/70 text-xs space-y-2 select-text">
              <div className="flex items-center gap-2 text-muted-foreground font-semibold mb-1">
                <ShieldCheck className="w-4 h-4 text-primary" />
                <span>Thông tin kiểm toán (Audit metadata)</span>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-[11px]">
                <div className="flex items-center gap-1.5 text-muted-foreground">
                  <Calendar className="w-3.5 h-3.5 shrink-0" />
                  <span>Tạo lúc:</span>
                  <span className="font-semibold text-foreground">{formatDate(currentData.createdAt)}</span>
                </div>
                <div className="text-muted-foreground">
                  <span>Tạo bởi:</span>{" "}
                  <span className="font-semibold text-foreground">
                    {formatAuditActor(detail?.createdBy)}
                  </span>
                </div>
                <div className="flex items-center gap-1.5 text-muted-foreground">
                  <Calendar className="w-3.5 h-3.5 shrink-0" />
                  <span>Cập nhật:</span>
                  <span className="font-semibold text-foreground">{formatDate(currentData.updatedAt)}</span>
                </div>
                <div className="text-muted-foreground">
                  <span>Cập nhật bởi:</span>{" "}
                  <span className="font-semibold text-foreground">
                    {formatAuditActor(detail?.updatedBy)}
                  </span>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Footer */}
        <div className="p-4 border-t border-border flex items-center justify-between">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={onClose}
            className="rounded-xl text-xs cursor-pointer"
          >
            Đóng
          </Button>

          {canUpdate && currentData.active && onEdit && (
            <Button
              type="button"
              size="sm"
              onClick={() => {
                onClose();
                onEdit(currentData);
              }}
              className="rounded-xl gap-2 text-xs font-bold cursor-pointer"
            >
              <span>Chỉnh sửa gói cước</span>
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}
