import { useMemo } from "react";
import { Plus, Trash2, ArrowUp, ArrowDown, AlertTriangle, ListFilter } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type { PlanFeatureRequest } from "@/types/plan.type";

interface PlanFeatureEditorProps {
  features: PlanFeatureRequest[];
  onChange: (newFeatures: PlanFeatureRequest[]) => void;
}

export default function PlanFeatureEditor({ features, onChange }: PlanFeatureEditorProps) {
  // Detect duplicate feature codes
  const duplicateCodes = useMemo(() => {
    const counts = new Map<string, number>();
    features.forEach((f) => {
      const code = (f.code || "").trim().toUpperCase();
      if (code) {
        counts.set(code, (counts.get(code) || 0) + 1);
      }
    });
    const dupes = new Set<string>();
    counts.forEach((count, code) => {
      if (count > 1) dupes.add(code);
    });
    return dupes;
  }, [features]);

  const handleAddFeature = () => {
    const nextOrder = features.length + 1;
    const newFeature: PlanFeatureRequest = {
      code: "",
      name: "",
      value: "",
      unit: "",
      displayOrder: nextOrder,
    };
    onChange([...features, newFeature]);
  };

  const handleUpdateFeature = (index: number, field: keyof PlanFeatureRequest, value: string | number) => {
    const updated = [...features];
    let val = value;
    if (field === "code" && typeof value === "string") {
      val = value.toUpperCase().trimStart();
    }
    updated[index] = { ...updated[index], [field]: val };
    onChange(updated);
  };

  const handleRemoveFeature = (index: number) => {
    const filtered = features.filter((_, i) => i !== index);
    // Re-index display order
    const reindexed = filtered.map((item, idx) => ({ ...item, displayOrder: idx + 1 }));
    onChange(reindexed);
  };

  const handleMove = (index: number, direction: "UP" | "DOWN") => {
    if (direction === "UP" && index === 0) return;
    if (direction === "DOWN" && index === features.length - 1) return;

    const targetIndex = direction === "UP" ? index - 1 : index + 1;
    const updated = [...features];
    const temp = updated[index];
    updated[index] = updated[targetIndex];
    updated[targetIndex] = temp;

    // Re-index display order
    const reindexed = updated.map((item, idx) => ({ ...item, displayOrder: idx + 1 }));
    onChange(reindexed);
  };

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <label className="text-xs font-bold text-foreground flex items-center gap-1.5">
          <ListFilter className="w-3.5 h-3.5 text-primary" />
          <span>Danh sách đặc điểm & ưu đãi ({features.length})</span>
        </label>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={handleAddFeature}
          className="h-8 text-xs rounded-xl gap-1.5 cursor-pointer hover:bg-primary/10 hover:text-primary border-dashed"
        >
          <Plus className="w-3.5 h-3.5" />
          <span>Thêm đặc điểm</span>
        </Button>
      </div>

      {/* Duplicate warning notification */}
      {duplicateCodes.size > 0 && (
        <div className="p-3 rounded-xl bg-destructive/10 border border-destructive/20 text-destructive text-xs flex items-center gap-2">
          <AlertTriangle className="w-4 h-4 shrink-0" />
          <span>
            Cảnh báo: Có mã đặc điểm bị trùng nhau ({Array.from(duplicateCodes).join(", ")}). Vui lòng điều chỉnh trước khi lưu.
          </span>
        </div>
      )}

      {features.length === 0 ? (
        <div className="p-6 text-center rounded-2xl border border-dashed border-border bg-muted/20 text-muted-foreground text-xs">
          Chưa có đặc điểm nào. Bấm &quot;Thêm đặc điểm&quot; để nhập ưu đãi (tốc độ, dung lượng, phút gọi, v.v.).
        </div>
      ) : (
        <div className="space-y-2.5">
          {features.map((feature, idx) => {
            const isCodeDuplicate = duplicateCodes.has((feature.code || "").trim().toUpperCase());

            return (
              <div
                key={idx}
                className={`p-3 rounded-2xl border transition-all ${
                  isCodeDuplicate
                    ? "border-destructive bg-destructive/5"
                    : "border-border/80 bg-card hover:border-border"
                }`}
              >
                <div className="grid grid-cols-1 sm:grid-cols-12 gap-2.5 items-center">
                  {/* Order & Move buttons */}
                  <div className="sm:col-span-1 flex items-center gap-1">
                    <span className="w-5 text-center font-bold text-muted-foreground text-xs">
                      {idx + 1}
                    </span>
                    <div className="flex flex-col gap-0.5">
                      <button
                        type="button"
                        onClick={() => handleMove(idx, "UP")}
                        disabled={idx === 0}
                        className="p-0.5 rounded text-muted-foreground hover:text-foreground disabled:opacity-30 cursor-pointer"
                        title="Di chuyển lên"
                      >
                        <ArrowUp className="w-3 h-3" />
                      </button>
                      <button
                        type="button"
                        onClick={() => handleMove(idx, "DOWN")}
                        disabled={idx === features.length - 1}
                        className="p-0.5 rounded text-muted-foreground hover:text-foreground disabled:opacity-30 cursor-pointer"
                        title="Di chuyển xuống"
                      >
                        <ArrowDown className="w-3 h-3" />
                      </button>
                    </div>
                  </div>

                  {/* Feature Code */}
                  <div className="sm:col-span-3">
                    <Input
                      placeholder="Mã (VD: SPEED)"
                      value={feature.code}
                      onChange={(e) => handleUpdateFeature(idx, "code", e.target.value)}
                      className={`h-8 text-xs font-mono rounded-lg ${
                        isCodeDuplicate ? "border-destructive text-destructive font-bold" : ""
                      }`}
                    />
                  </div>

                  {/* Feature Name */}
                  <div className="sm:col-span-3">
                    <Input
                      placeholder="Tên (VD: Tốc độ tải)"
                      value={feature.name}
                      onChange={(e) => handleUpdateFeature(idx, "name", e.target.value)}
                      className="h-8 text-xs rounded-lg"
                    />
                  </div>

                  {/* Feature Value */}
                  <div className="sm:col-span-3">
                    <Input
                      placeholder="Giá trị (VD: 150 Mbps)"
                      value={feature.value}
                      onChange={(e) => handleUpdateFeature(idx, "value", e.target.value)}
                      className="h-8 text-xs rounded-lg"
                    />
                  </div>

                  {/* Feature Unit & Delete */}
                  <div className="sm:col-span-2 flex items-center gap-1.5">
                    <Input
                      placeholder="Đơn vị"
                      value={feature.unit || ""}
                      onChange={(e) => handleUpdateFeature(idx, "unit", e.target.value)}
                      className="h-8 text-xs rounded-lg flex-1"
                    />
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon-sm"
                      onClick={() => handleRemoveFeature(idx)}
                      className="h-8 w-8 text-muted-foreground hover:text-destructive hover:bg-destructive/10 rounded-lg cursor-pointer shrink-0"
                      title="Xóa dòng đặc điểm này"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </Button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
