import { useState } from "react";
import { toast } from "sonner";
import { AlertTriangle, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { ServicePlanSummary } from "@/types/plan.type";
import { deleteAdminPlan } from "@/services/admin-plan.service";
import { getApiErrorMessage } from "@/utils/userUtils";

interface DeletePlanDialogProps {
  isOpen: boolean;
  plan: ServicePlanSummary | null;
  onClose: () => void;
  onSuccess: () => void;
}

export default function DeletePlanDialog({
  isOpen,
  plan,
  onClose,
  onSuccess,
}: DeletePlanDialogProps) {
  const [loading, setLoading] = useState(false);

  if (!isOpen || !plan) return null;

  const handleDelete = async () => {
    setLoading(true);
    try {
      await deleteAdminPlan(plan.id);
      toast.success(`Đã xóa mềm gói cước ${plan.code} thành công!`);
      onSuccess();
      onClose();
    } catch (err) {
      toast.error(getApiErrorMessage(err, "Không thể xóa gói cước này"));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in duration-150">
      <div
        className="relative w-full max-w-md bg-card border border-border rounded-3xl shadow-2xl p-6 select-none"
        role="dialog"
        aria-modal="true"
      >
        <div className="flex items-center gap-3 text-destructive mb-4">
          <div className="w-10 h-10 rounded-2xl bg-destructive/10 flex items-center justify-center shrink-0">
            <AlertTriangle className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-base font-bold text-foreground">Xác nhận xóa mềm gói cước</h3>
            <p className="text-xs text-muted-foreground">Thao tác này sẽ ẩn gói cước khỏi hệ thống</p>
          </div>
        </div>

        <p className="text-xs text-muted-foreground mb-6 leading-relaxed">
          Bạn có chắc chắn muốn xóa mềm gói cước <span className="font-bold text-foreground">{plan.name}</span> ({plan.code}) không? Gói cước sau khi xóa mềm sẽ không thể chỉnh sửa hoặc xuất bản lại.
        </p>

        <div className="flex items-center justify-end gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={onClose}
            disabled={loading}
            className="rounded-xl text-xs cursor-pointer"
          >
            Hủy bỏ
          </Button>
          <Button
            type="button"
            variant="destructive"
            size="sm"
            onClick={handleDelete}
            disabled={loading}
            className="rounded-xl text-xs font-bold gap-2 cursor-pointer"
          >
            {loading ? (
              <>
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                <span>Đang xóa...</span>
              </>
            ) : (
              <span>Xác nhận xóa</span>
            )}
          </Button>
        </div>
      </div>
    </div>
  );
}
