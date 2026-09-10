import { useState, useEffect } from "react";
import { toast } from "sonner";
import { X, Save, FolderPlus, Edit3, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type { CreateCategoryRequest, ServiceCategory, UpdateCategoryRequest } from "@/types/plan.type";
import { createAdminCategory, updateAdminCategory } from "@/services/admin-plan.service";
import { getApiErrorMessage } from "@/utils/userUtils";
import { sanitizeCode } from "@/utils/planUtils";

interface CategoryFormModalProps {
  isOpen: boolean;
  editingCategory: ServiceCategory | null;
  onClose: () => void;
  onSuccess: () => void;
}

interface FormState {
  code: string;
  name: string;
  description: string;
  displayOrder: number;
}

const DEFAULT_FORM_STATE: FormState = {
  code: "",
  name: "",
  description: "",
  displayOrder: 0,
};

export default function CategoryFormModal({
  isOpen,
  editingCategory,
  onClose,
  onSuccess,
}: CategoryFormModalProps) {
  const [form, setForm] = useState<FormState>(DEFAULT_FORM_STATE);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!isOpen) return;

    if (editingCategory) {
      setForm({
        code: editingCategory.code || "",
        name: editingCategory.name || "",
        description: editingCategory.description || "",
        displayOrder: editingCategory.displayOrder || 0,
      });
    } else {
      setForm(DEFAULT_FORM_STATE);
    }
  }, [isOpen, editingCategory]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const cleanCode = sanitizeCode(form.code);
    const cleanName = form.name.trim();

    if (!cleanCode) {
      toast.error("Vui lòng nhập mã nhóm dịch vụ");
      return;
    }
    if (cleanName.length < 3 || cleanName.length > 120) {
      toast.error("Tên nhóm dịch vụ phải từ 3 đến 120 ký tự");
      return;
    }

    setLoading(true);

    try {
      const payload: CreateCategoryRequest | UpdateCategoryRequest = {
        code: cleanCode,
        name: cleanName,
        description: form.description.trim() || undefined,
        displayOrder: Number(form.displayOrder) || 0,
      };

      if (editingCategory) {
        await updateAdminCategory(editingCategory.id, payload);
        toast.success("Cập nhật nhóm dịch vụ thành công!");
      } else {
        await createAdminCategory(payload);
        toast.success("Tạo nhóm dịch vụ mới thành công!");
      }

      onSuccess();
      onClose();
    } catch (err) {
      toast.error(getApiErrorMessage(err, "Thao tác thất bại. Vui lòng kiểm tra lại!"));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto animate-in fade-in duration-150">
      <div
        className="relative w-full max-w-lg bg-card border border-border rounded-3xl shadow-2xl my-8 overflow-hidden select-none"
        role="dialog"
        aria-modal="true"
      >
        {/* Header */}
        <div className="p-5 border-b border-border/80 flex items-center justify-between bg-muted/30">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-primary/10 text-primary flex items-center justify-center font-bold shrink-0">
              {editingCategory ? <Edit3 className="w-5 h-5" /> : <FolderPlus className="w-5 h-5" />}
            </div>
            <div>
              <h2 className="text-base font-bold text-foreground">
                {editingCategory ? "Chỉnh sửa nhóm dịch vụ" : "Tạo nhóm dịch vụ mới"}
              </h2>
              <p className="text-xs text-muted-foreground">
                {editingCategory
                  ? `Cập nhật nhóm ${editingCategory.code}`
                  : "Tạo nhóm phân loại mới cho các gói cước"}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            disabled={loading}
            className="p-2 rounded-xl text-muted-foreground hover:text-foreground hover:bg-muted/70 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {/* Code */}
          <div className="space-y-1">
            <label className="text-xs font-bold text-foreground">
              Mã nhóm dịch vụ <span className="text-destructive">*</span>
            </label>
            <Input
              placeholder="VD: INTERNET"
              value={form.code}
              onChange={(e) => setForm((prev) => ({ ...prev, code: e.target.value.toUpperCase() }))}
              required
              className="h-9 text-xs font-mono rounded-xl"
            />
          </div>

          {/* Name */}
          <div className="space-y-1">
            <label className="text-xs font-bold text-foreground">
              Tên nhóm dịch vụ <span className="text-destructive">*</span>
            </label>
            <Input
              placeholder="VD: Internet Broadband"
              value={form.name}
              onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
              maxLength={120}
              required
              className="h-9 text-xs rounded-xl"
            />
          </div>

          {/* Display Order */}
          <div className="space-y-1">
            <label className="text-xs font-bold text-foreground">Thứ tự hiển thị</label>
            <Input
              type="number"
              placeholder="0"
              value={form.displayOrder}
              onChange={(e) => setForm((prev) => ({ ...prev, displayOrder: Number(e.target.value) }))}
              className="h-9 text-xs rounded-xl"
            />
          </div>

          {/* Description */}
          <div className="space-y-1">
            <label className="text-xs font-bold text-foreground">Mô tả nhóm</label>
            <textarea
              placeholder="Mô tả các loại dịch vụ thuộc nhóm này..."
              value={form.description}
              onChange={(e) => setForm((prev) => ({ ...prev, description: e.target.value }))}
              rows={3}
              maxLength={5000}
              className="w-full p-3 rounded-xl border border-border bg-card text-xs font-medium text-foreground outline-none focus:border-primary transition-colors resize-none"
            />
          </div>

          {/* Footer Buttons */}
          <div className="pt-4 border-t border-border flex items-center justify-end gap-2">
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
              type="submit"
              size="sm"
              disabled={loading}
              className="rounded-xl gap-2 text-xs font-bold cursor-pointer"
            >
              {loading ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  <span>Đang lưu...</span>
                </>
              ) : (
                <>
                  <Save className="w-3.5 h-3.5" />
                  <span>{editingCategory ? "Lưu thay đổi" : "Tạo nhóm"}</span>
                </>
              )}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
