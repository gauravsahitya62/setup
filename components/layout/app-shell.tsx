import { Sidebar, type SidebarItem } from "@/components/layout/sidebar";
import { cn } from "@/lib/utils";

interface AppShellProps {
  children: React.ReactNode;
  sidebarItems?: SidebarItem[];
  className?: string;
}

export function AppShell({ children, sidebarItems, className }: AppShellProps) {
  return (
    <div className={cn("flex min-h-[calc(100vh-4rem)]", className)}>
      <Sidebar items={sidebarItems} />
      <div className="flex-1 overflow-x-hidden">
        <div className="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 lg:px-8">{children}</div>
      </div>
    </div>
  );
}
