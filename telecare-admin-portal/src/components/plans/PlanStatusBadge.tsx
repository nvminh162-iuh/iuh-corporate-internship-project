import type { PlanStatus } from "@/types/plan.type";
import { getPlanStatusInfo } from "@/utils/planUtils";

interface PlanStatusBadgeProps {
  status?: PlanStatus | null;
  className?: string;
}

export default function PlanStatusBadge({ status, className = "" }: PlanStatusBadgeProps) {
  const info = getPlanStatusInfo(status);

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold border transition-colors ${info.className} ${className}`}
    >
      <span className={`w-1.5 h-1.5 rounded-full ${info.dotClassName}`} />
      <span>{info.label}</span>
    </span>
  );
}
