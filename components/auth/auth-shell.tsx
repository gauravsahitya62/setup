import Link from "next/link";

import { cn } from "@/lib/utils";

interface AuthShellProps {
  title: string;
  description: string;
  children: React.ReactNode;
  footer?: React.ReactNode;
  className?: string;
}

export function AuthShell({ title, description, children, footer, className }: AuthShellProps) {
  return (
    <div className="relative flex min-h-screen items-center justify-center px-4 py-12">
      <div
        aria-hidden
        className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_at_top,_oklch(0.97_0.01_250),_transparent_55%),linear-gradient(to_bottom,_var(--background),_oklch(0.97_0.005_250))] dark:bg-[radial-gradient(ellipse_at_top,_oklch(0.28_0.02_250),_transparent_55%)]"
      />
      <div className={cn("relative w-full max-w-md space-y-8", className)}>
        <div className="space-y-3 text-center">
          <Link href="/" className="inline-flex items-center gap-2.5">
            <span className="bg-foreground text-background flex size-9 items-center justify-center rounded-xl text-sm font-semibold">
              F
            </span>
            <span className="text-xl font-semibold tracking-tight">FlowSet</span>
          </Link>
          <div className="space-y-1">
            <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
            <p className="text-muted-foreground text-sm">{description}</p>
          </div>
        </div>
        <div className="border-border/70 bg-card/90 rounded-2xl border p-6 shadow-sm backdrop-blur sm:p-8">
          {children}
        </div>
        {footer}
      </div>
    </div>
  );
}
