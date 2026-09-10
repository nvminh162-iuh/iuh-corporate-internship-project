import React from "react";
import Link from "next/link";
import { Check, Sparkles, ArrowRight } from "lucide-react";
import type { PublicPlanSummary } from "@/types/plan.type";
import { formatVND, getBillingCycleLabel, getCategoryLabel } from "@/utils/planUtils";

interface PlanCardProps {
  plan: PublicPlanSummary;
  onSubscribe?: (plan: PublicPlanSummary) => void;
}

export default function PlanCard({ plan, onSubscribe }: PlanCardProps) {
  const categoryLabel = getCategoryLabel(plan.category?.code, plan.category?.name);

  return (
    <div
      className={`group relative rounded-3xl border transition-all duration-300 flex flex-col justify-between p-6 bg-card hover:bg-card/95 hover:shadow-xl hover:-translate-y-1 ${
        plan.highlighted
          ? "border-primary/40 shadow-lg shadow-primary/5 dark:shadow-primary/10 ring-1 ring-primary/20"
          : "border-border/80 shadow-xs hover:border-primary/30"
      }`}
    >
      {/* Top Banner Badges */}
      <div className="flex items-center justify-between gap-2 mb-4">
        <span className="inline-flex items-center px-3 py-1 rounded-full text-[11px] font-bold bg-primary/10 text-primary uppercase tracking-wider border border-primary/20">
          {categoryLabel}
        </span>
        {plan.highlighted && (
          <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full text-[11px] font-extrabold bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20">
            <Sparkles className="w-3.5 h-3.5 text-amber-500" />
            Gói Nổi Bật
          </span>
        )}
      </div>

      {/* Package Main Info */}
      <div className="space-y-3 flex-1">
        <div>
          <span className="text-[11px] font-mono font-medium text-muted-foreground uppercase">
            Mã gói: {plan.code}
          </span>
          <h3 className="text-xl font-extrabold text-foreground group-hover:text-primary transition-colors line-clamp-1">
            {plan.name}
          </h3>
        </div>

        {plan.summary && (
          <p className="text-xs text-muted-foreground line-clamp-2 leading-relaxed">
            {plan.summary}
          </p>
        )}
      </div>

      {/* Price & Billing Cycle Box */}
      <div className="my-6 pt-4 border-t border-border/50 flex items-baseline gap-1.5">
        <span className="text-2xl sm:text-3xl font-black text-primary tracking-tight">
          {formatVND(plan.price)}
        </span>
        <span className="text-xs font-medium text-muted-foreground">
          / {getBillingCycleLabel(plan.billingCycle)}
        </span>
      </div>

      {/* Action Buttons */}
      <div className="pt-2 grid grid-cols-2 gap-3">
        <Link
          href={`/plans/${plan.slug}`}
          className="h-11 rounded-2xl border border-border bg-muted/40 hover:bg-muted text-foreground hover:text-primary text-xs font-bold transition-all flex items-center justify-center gap-1.5 group/btn"
        >
          <span>Xem chi tiết</span>
          <ArrowRight className="w-3.5 h-3.5 group-hover/btn:translate-x-0.5 transition-transform" />
        </Link>

        <button
          type="button"
          onClick={() => onSubscribe?.(plan)}
          className="h-11 rounded-2xl bg-primary hover:bg-primary/90 text-primary-foreground text-xs font-extrabold shadow-md shadow-primary/20 transition-all cursor-pointer flex items-center justify-center gap-1"
        >
          <Check className="w-4 h-4" />
          <span>Đăng ký</span>
        </button>
      </div>
    </div>
  );
}
