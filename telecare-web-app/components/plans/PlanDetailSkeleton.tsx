import React from "react";

export default function PlanDetailSkeleton() {
  return (
    <div className="max-w-5xl mx-auto px-4 py-8 space-y-8 animate-pulse">
      {/* Banner Skeleton */}
      <div className="bg-card/80 dark:bg-card/40 rounded-3xl border border-border/60 p-8 space-y-6 shadow-sm">
        <div className="flex items-center gap-3">
          <div className="h-6 w-28 bg-primary/20 rounded-full" />
          <div className="h-6 w-20 bg-muted rounded-full" />
        </div>
        <div className="h-10 w-2/3 bg-muted rounded-2xl" />
        <div className="h-5 w-full max-w-2xl bg-muted/60 rounded-xl" />
        <div className="h-10 w-48 bg-primary/30 rounded-xl" />
      </div>

      {/* Grid Features & Description Skeleton */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        <div className="md:col-span-2 bg-card/80 dark:bg-card/40 rounded-3xl border border-border/60 p-6 space-y-4 shadow-sm">
          <div className="h-6 w-40 bg-muted rounded-lg" />
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="h-20 bg-muted/50 rounded-2xl p-4" />
            ))}
          </div>
          <div className="h-6 w-32 bg-muted rounded-lg pt-4" />
          <div className="space-y-2">
            <div className="h-4 w-full bg-muted/40 rounded-md" />
            <div className="h-4 w-5/6 bg-muted/40 rounded-md" />
            <div className="h-4 w-4/6 bg-muted/40 rounded-md" />
          </div>
        </div>

        <div className="bg-card/80 dark:bg-card/40 rounded-3xl border border-border/60 p-6 space-y-6 shadow-sm h-fit">
          <div className="h-6 w-36 bg-muted rounded-lg" />
          <div className="h-12 w-full bg-primary/20 rounded-2xl" />
          <div className="h-12 w-full bg-muted rounded-2xl" />
        </div>
      </div>
    </div>
  );
}
