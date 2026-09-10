import React from "react";

export default function PlanCardSkeleton() {
  return (
    <div className="bg-card/80 dark:bg-card/40 rounded-3xl border border-border/60 p-6 flex flex-col justify-between space-y-6 shadow-sm animate-pulse">
      <div className="space-y-4">
        {/* Header Badge & Category */}
        <div className="flex items-center justify-between">
          <div className="h-5 w-24 bg-muted rounded-full" />
          <div className="h-5 w-16 bg-muted/60 rounded-full" />
        </div>

        {/* Package Name */}
        <div className="h-7 w-3/4 bg-muted rounded-xl" />

        {/* Summary */}
        <div className="space-y-2">
          <div className="h-4 w-full bg-muted/50 rounded-md" />
          <div className="h-4 w-2/3 bg-muted/50 rounded-md" />
        </div>

        {/* Price & Billing Cycle */}
        <div className="pt-2 border-t border-border/40 flex items-baseline gap-2">
          <div className="h-8 w-32 bg-primary/20 rounded-lg" />
          <div className="h-4 w-12 bg-muted/60 rounded-md" />
        </div>
      </div>

      {/* Action Buttons */}
      <div className="pt-4 border-t border-border/40 grid grid-cols-2 gap-3">
        <div className="h-11 bg-muted rounded-2xl" />
        <div className="h-11 bg-primary/30 rounded-2xl" />
      </div>
    </div>
  );
}
