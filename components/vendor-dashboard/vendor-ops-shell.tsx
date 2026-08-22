import { VendorOpsNavbar } from "@/components/vendor-dashboard/vendor-ops-navbar";
import { VendorOpsSidebar } from "@/components/vendor-dashboard/vendor-ops-sidebar";
import { cn } from "@/lib/utils";

interface VendorOpsShellProps {
  children: React.ReactNode;
  className?: string;
}

export function VendorOpsShell({ children, className }: VendorOpsShellProps) {
  return (
    <div className="bg-background flex min-h-screen">
      <VendorOpsSidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <VendorOpsNavbar />
        <main className={cn("flex-1 overflow-x-hidden", className)}>
          <div className="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
}
