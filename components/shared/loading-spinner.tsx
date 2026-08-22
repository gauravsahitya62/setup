import { Loader2 } from "lucide-react";

import { cn } from "@/lib/utils";

interface LoadingSpinnerProps {
  className?: string;
  size?: "sm" | "md" | "lg";
  label?: string;
}

const sizeMap = {
  sm: "size-4",
  md: "size-6",
  lg: "size-8",
} as const;

export function LoadingSpinner({ className, size = "md", label = "Loading" }: LoadingSpinnerProps) {
  return (
    <div
      role="status"
      aria-live="polite"
      className={cn("text-muted-foreground flex items-center justify-center gap-2", className)}
    >
      <Loader2 className={cn("animate-spin", sizeMap[size])} />
      <span className="sr-only">{label}</span>
    </div>
  );
}
