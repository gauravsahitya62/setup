import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

const TONE_CLASS = {
  success: "bg-emerald-500/12 text-emerald-800 dark:text-emerald-300",
  warning: "bg-amber-500/12 text-amber-900 dark:text-amber-200",
  danger: "bg-rose-500/12 text-rose-800 dark:text-rose-300",
  info: "bg-sky-500/12 text-sky-900 dark:text-sky-200",
  neutral: "bg-muted text-muted-foreground",
} as const;

export type AdminStatusTone = keyof typeof TONE_CLASS;

interface AdminStatusBadgeProps {
  label: string;
  tone?: AdminStatusTone;
  className?: string;
}

export function AdminStatusBadge({ label, tone = "neutral", className }: AdminStatusBadgeProps) {
  return (
    <Badge variant="secondary" className={cn("rounded-md capitalize", TONE_CLASS[tone], className)}>
      {label}
    </Badge>
  );
}
