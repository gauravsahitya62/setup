import { PlannerNavbar } from "@/components/planner/planner-navbar";
import { PlannerSidebar } from "@/components/planner/planner-sidebar";
import { cn } from "@/lib/utils";

interface PlannerShellProps {
  children: React.ReactNode;
  className?: string;
}

export function PlannerShell({ children, className }: PlannerShellProps) {
  return (
    <div className="bg-background flex min-h-screen">
      <PlannerSidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <PlannerNavbar />
        <main className={cn("flex-1 overflow-x-hidden", className)}>
          <div className="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
}
