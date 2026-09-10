import { useState, useEffect } from "react";
import { toast } from "sonner";
import { X, Save, PackagePlus, Edit3, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import PlanFeatureEditor from "./PlanFeatureEditor";
import type {
  BillingCycle,
  CreateServicePlanRequest,
  PlanFeatureRequest,
  PlanStatus,
  ServiceCategory,
  ServicePlanDetail,
  ServicePlanSummary,
  UpdateServicePlanRequest,
} from "@/types/plan.type";
import { createAdminPlan, getAdminPlanById, updateAdminPlan } from "@/services/admin-plan.service";
import { getApiErrorMessage } from "@/utils/userUtils";
import { sanitizeCode, sanitizeSlug } from "@/utils/planUtils";

interface PlanFormModalProps {
  isOpen: boolean;
  editingPlan: ServicePlanSummary | null;
  categories: ServiceCategory[];
  onClose: () => void;
  onSuccess: () => void;
}

interface FormState {
  code: string;
  slug: string;
  name: string;
  categoryId: string;
  summary: string;
  description: string;
  price: string;
  currency: string;
  billingCycle: BillingCycle;
  status: PlanStatus;
  highlighted: boolean;
  displayOrder: number;
  features: PlanFeatureRequest[];
}

const DEFAULT_FORM_STATE: FormState = {
  code: "",
  slug: "",
  name: "",
  categoryId: "",
  summary: "",
  description: "",
  price: "0",
  currency: "VND",
  billingCycle: "MONTH",
  status: "DRAFT",
  highlighted: false,
  displayOrder: 0,
  features: [],
};

export default function PlanFormModal({
  isOpen,
  editingPlan,
  categories,
  onClose,
  onSuccess,
}: PlanFormModalProps) {
  const [form, setForm] = useState<FormState>(DEFAULT_FORM_STATE);
  const [loading, setLoading] = useState(false);
  const [loadingDetails, setLoadingDetails] = useState(false);

  // Auto slug generation from plan name if slug is not manually set
  const [autoSlug, setAutoSlug] = useState(true);

  // Fetch full details if editing
  useEffect(() => {
    if (!isOpen) return;

    if (editingPlan) {
      setLoadingDetails(true);
      setAutoSlug(false);
      getAdminPlanById(editingPlan.id)
        .then((detail: ServicePlanDetail) => {
          setForm({
            code: detail.code || "",
            slug: detail.slug || "",
            name: detail.name || "",
            categoryId: detail.categoryId || "",
            summary: detail.summary || "",
            description: detail.description || "",
            price: detail.price !== undefined ? String(detail.price) : "0",
            currency: detail.currency || "VND",
            billingCycle: detail.billingCycle || "MONTH",
            status: detail.status || "DRAFT",
            highlighted: Boolean(detail.highlighted),
            displayOrder: detail.displayOrder || 0,
            features: (detail.features || []).map((f, idx) => ({
              code: f.code,
              name: f.name,
              value: f.value,
              unit: f.unit || "",
              displayOrder: f.displayOrder || idx + 1,
            })),
          });
        })
        .catch((err) => {
          toast.error(getApiErrorMessage(err, "Không thể tải chi tiết gói cước"));
        })
        .finally(() => setLoadingDetails(false));
    } else {
      setAutoSlug(true);
      setForm({
        ...DEFAULT_FORM_STATE,
        categoryId: categories.length > 0 ? categories[0].id : "",
      });
    }
  }, [isOpen, editingPlan, categories]);

  if (!isOpen) return null;

  const handleNameChange = (val: string) => {
    setForm((prev) => {
      const nextName = val;
      let nextSlug = prev.slug;
      if (autoSlug) {
        nextSlug = val
          .toLowerCase()
          .normalize("NFD")
          .replace(/[\u0300-\u036f]/g, "")
          .replace(/đ/g, "d")
          .replace(/[^a-z0-9\s-]/g, "")
          .trim()
          .replace(/\s+/g, "-");
      }
      return { ...prev, name: nextName, slug: nextSlug };
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // Client-side validations
    const cleanCode = sanitizeCode(form.code);
    const cleanSlug = sanitizeSlug(form.slug);
    const cleanName = form.name.trim();

    if (!cleanCode) {
      toast.error("Vui lòng nhập mã gói cước");
      return;
    }
    if (!cleanSlug) {
      toast.error("Vui lòng nhập slug gói cước");
      return;
    }
    if (cleanName.length < 3 || cleanName.length > 120) {
      toast.error("Tên gói cước phải từ 3 đến 120 ký tự");
      return;
    }
    if (!form.categoryId) {
      toast.error("Vui lòng chọn nhóm dịch vụ");
      return;
    }
    const numPrice = Number(form.price);
    if (Number.isNaN(numPrice) || numPrice < 0) {
      toast.error("Giá gói cước không hợp lệ");
      return;
    }

    // Validate feature code duplicates
    const featureCodes = new Set<string>();
    for (const feat of form.features) {
      const c = (feat.code || "").trim().toUpperCase();
      if (!c || !feat.name.trim() || !feat.value.trim()) {
        toast.error("Các dòng đặc điểm phải có đầy đủ Mã, Tên và Giá trị");
        return;
      }
      if (featureCodes.has(c)) {
        toast.error(`Mã đặc điểm ${c} bị trùng lặp`);
        return;
      }
      featureCodes.add(c);
    }

    setLoading(true);

    try {
      const payload: CreateServicePlanRequest | UpdateServicePlanRequest = {
        code: cleanCode,
        slug: cleanSlug,
        name: cleanName,
        summary: form.summary.trim() || undefined,
        description: form.description.trim() || undefined,
        price: numPrice,
        currency: (form.currency || "VND").toUpperCase().trim(),
        billingCycle: form.billingCycle,
        status: form.status,
        categoryId: form.categoryId,
        highlighted: form.highlighted,
        displayOrder: Number(form.displayOrder) || 0,
        features: form.features.map((f, idx) => ({
          code: sanitizeCode(f.code),
          name: f.name.trim(),
          value: f.value.trim(),
          unit: f.unit ? f.unit.trim() : undefined,
          displayOrder: f.displayOrder || idx + 1,
        })),
      };

      if (editingPlan) {
        await updateAdminPlan(editingPlan.id, payload);
        toast.success("Cập nhật gói cước thành công!");
      } else {
        await createAdminPlan(payload);
        toast.success("Tạo gói cước mới thành công!");
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
        className="relative w-full max-w-3xl bg-card border border-border rounded-3xl shadow-2xl my-8 overflow-hidden select-none"
        role="dialog"
        aria-modal="true"
      >
        {/* Header */}
        <div className="p-5 border-b border-border/80 flex items-center justify-between bg-muted/30">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-primary/10 text-primary flex items-center justify-center font-bold shrink-0">
              {editingPlan ? <Edit3 className="w-5 h-5" /> : <PackagePlus className="w-5 h-5" />}
            </div>
            <div>
              <h2 className="text-base font-bold text-foreground">
                {editingPlan ? "Chỉnh sửa gói cước" : "Tạo gói cước mới"}
              </h2>
              <p className="text-xs text-muted-foreground">
                {editingPlan
                  ? `Cập nhật thông tin gói ${editingPlan.code}`
                  : "Điền thông tin mô tả, giá cả và các đặc điểm ưu đãi gói cước"}
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
        {loadingDetails ? (
          <div className="p-12 text-center text-muted-foreground flex flex-col items-center gap-3">
            <Loader2 className="w-7 h-7 animate-spin text-primary" />
            <span className="text-xs">Đang tải thông tin chi tiết gói cước...</span>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="p-6 space-y-5 max-h-[78vh] overflow-y-auto">
            {/* Group 1: Basic Info */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Plan Name */}
              <div className="sm:col-span-2 space-y-1">
                <label className="text-xs font-bold text-foreground">
                  Tên gói cước <span className="text-destructive">*</span>
                </label>
                <Input
                  placeholder="VD: Home Net 1 Super"
                  value={form.name}
                  onChange={(e) => handleNameChange(e.target.value)}
                  maxLength={120}
                  required
                  className="h-9 text-xs rounded-xl"
                />
              </div>

              {/* Code */}
              <div className="space-y-1">
                <label className="text-xs font-bold text-foreground">
                  Mã gói cước <span className="text-destructive">*</span>
                </label>
                <Input
                  placeholder="VD: HOME_NET_1"
                  value={form.code}
                  onChange={(e) => setForm((prev) => ({ ...prev, code: e.target.value.toUpperCase() }))}
                  required
                  className="h-9 text-xs font-mono rounded-xl"
                />
              </div>

              {/* Slug */}
              <div className="space-y-1">
                <label className="text-xs font-bold text-foreground flex items-center justify-between">
                  <span>Slug danh mục <span className="text-destructive">*</span></span>
                  {!editingPlan && (
                    <button
                      type="button"
                      onClick={() => setAutoSlug(!autoSlug)}
                      className="text-[10px] text-primary hover:underline font-normal cursor-pointer"
                    >
                      {autoSlug ? "Tự động" : "Tự nhập"}
                    </button>
                  )}
                </label>
                <Input
                  placeholder="VD: home-net-1"
                  value={form.slug}
                  onChange={(e) => {
                    setAutoSlug(false);
                    setForm((prev) => ({ ...prev, slug: e.target.value.toLowerCase() }));
                  }}
                  required
                  className="h-9 text-xs font-mono rounded-xl"
                />
              </div>

              {/* Category */}
              <div className="space-y-1">
                <label className="text-xs font-bold text-foreground">
                  Nhóm dịch vụ <span className="text-destructive">*</span>
                </label>
                <select
                  value={form.categoryId}
                  onChange={(e) => setForm((prev) => ({ ...prev, categoryId: e.target.value }))}
                  required
                  className="w-full h-9 px-3 rounded-xl border border-border bg-card text-xs font-medium text-foreground outline-none cursor-pointer"
                >
                  <option value="" disabled>
                    -- Chọn nhóm dịch vụ --
                  </option>
                  {categories.map((cat) => (
                    <option key={cat.id} value={cat.id}>
                      {cat.name} ({cat.code})
                    </option>
                  ))}
                </select>
              </div>

              {/* Billing Cycle */}
              <div className="space-y-1">
                <label className="text-xs font-bold text-foreground">
                  Chu kỳ thanh toán <span className="text-destructive">*</span>
                </label>
                <select
                  value={form.billingCycle}
                  onChange={(e) => setForm((prev) => ({ ...prev, billingCycle: e.target.value as BillingCycle }))}
                  className="w-full h-9 px-3 rounded-xl border border-border bg-card text-xs font-medium text-foreground outline-none cursor-pointer"
                >
                  <option value="MONTH">Theo tháng (MONTH)</option>
                  <option value="YEAR">Theo năm (YEAR)</option>
                  <option value="DAY">Theo ngày (DAY)</option>
                  <option value="ONE_TIME">Thanh toán 1 lần (ONE_TIME)</option>
                </select>
              </div>

              {/* Price */}
              <div className="space-y-1">
                <label className="text-xs font-bold text-foreground">
                  Giá gói cước <span className="text-destructive">*</span>
                </label>
                <Input
                  type="number"
                  min="0"
                  step="1000"
                  placeholder="220000"
                  value={form.price}
                  onChange={(e) => setForm((prev) => ({ ...prev, price: e.target.value }))}
                  required
                  className="h-9 text-xs font-bold rounded-xl"
                />
              </div>

              {/* Currency */}
              <div className="space-y-1">
                <label className="text-xs font-bold text-foreground">Đơn vị tiền tệ</label>
                <Input
                  maxLength={3}
                  placeholder="VND"
                  value={form.currency}
                  onChange={(e) => setForm((prev) => ({ ...prev, currency: e.target.value.toUpperCase() }))}
                  className="h-9 text-xs font-bold rounded-xl"
                />
              </div>

              {/* Status */}
              <div className="space-y-1">
                <label className="text-xs font-bold text-foreground">Trạng thái phát hành</label>
                <select
                  value={form.status}
                  onChange={(e) => setForm((prev) => ({ ...prev, status: e.target.value as PlanStatus }))}
                  className="w-full h-9 px-3 rounded-xl border border-border bg-card text-xs font-medium text-foreground outline-none cursor-pointer"
                >
                  <option value="DRAFT">Bản nháp (DRAFT)</option>
                  <option value="PUBLISHED">Xuất bản (PUBLISHED)</option>
                  <option value="HIDDEN">Tạm ẩn (HIDDEN)</option>
                </select>
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
            </div>

            {/* Checkbox Highlighted */}
            <div className="flex items-center gap-2 pt-1">
              <input
                type="checkbox"
                id="highlighted-check"
                checked={form.highlighted}
                onChange={(e) => setForm((prev) => ({ ...prev, highlighted: e.target.checked }))}
                className="w-4 h-4 rounded border-border text-primary focus:ring-primary cursor-pointer"
              />
              <label htmlFor="highlighted-check" className="text-xs font-medium text-foreground cursor-pointer">
                Đánh dấu là <span className="font-bold text-amber-500">Gói cước Nổi Bật</span> (khuyến nghị cho trang chủ)
              </label>
            </div>

            {/* Summary */}
            <div className="space-y-1">
              <label className="text-xs font-bold text-foreground">Tóm tắt ngắn (Summary)</label>
              <Input
                placeholder="VD: Gói cước internet cáp quang tốc độ cao cho gia đình"
                value={form.summary}
                onChange={(e) => setForm((prev) => ({ ...prev, summary: e.target.value }))}
                maxLength={255}
                className="h-9 text-xs rounded-xl"
              />
            </div>

            {/* Description */}
            <div className="space-y-1">
              <label className="text-xs font-bold text-foreground">Mô tả chi tiết</label>
              <textarea
                placeholder="Mô tả chi tiết quyền lợi, điều kiện áp dụng..."
                value={form.description}
                onChange={(e) => setForm((prev) => ({ ...prev, description: e.target.value }))}
                rows={3}
                maxLength={5000}
                className="w-full p-3 rounded-xl border border-border bg-card text-xs font-medium text-foreground outline-none focus:border-primary transition-colors resize-none"
              />
            </div>

            {/* Plan Features Editor */}
            <div className="pt-2 border-t border-border/80">
              <PlanFeatureEditor
                features={form.features}
                onChange={(features) => setForm((prev) => ({ ...prev, features }))}
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
                    <span>{editingPlan ? "Lưu thay đổi" : "Tạo gói cước"}</span>
                  </>
                )}
              </Button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
