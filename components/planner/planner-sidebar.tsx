"use client";

import * as React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  Bell,
  ChevronLeft,
  ChevronRight,
  ClipboardList,
  FolderKanban,
  Heart,
  LayoutDashboard,
  LifeBuoy,
  Package,
  Settings,
  Star,
  Store,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { SIDEBAR_COLLAPSED_WIDTH, SIDEBAR_WIDTH, ROUTES } from "@/constants";
import { cn } from "@/lib/utils";
import { useUIStore } from "@/store";

export const PLANNER_NAV_ITEMS = [
  { href: ROUTES.planner.root, label: "Dashboard", icon: LayoutDashboard },
  { href: ROUTES.planner.projects, label: "Projects", icon: FolderKanban },
  { href: ROUTES.planner.marketplace, label: "Marketplace", icon: Store },
  { href: ROUTES.planner.favorites, label: "Favorites", icon: Heart },
  { href: ROUTES.planner.bookingRequests, label: "Booking Requests", icon: ClipboardList },
  { href: ROUTES.planner.orders, label: "Orders", icon: Package },
  { href: ROUTES.planner.reviews, label: "Reviews", icon: Star },
  { href: ROUTES.planner.support, label: "Support", icon: LifeBuoy },
  { href: ROUTES.planner.notifications, label: "Notifications", icon: Bell },
  { href: ROUTES.planner.settings, label: "Settings", icon: Settings },
] as const;

function NavList({ collapsed, onNavigate }: { collapsed?: boolean; onNavigate?: () => void }) {
  const pathname = usePathname();

  return (
    <nav className="flex flex-col gap-1">
      {PLANNER_NAV_ITEMS.map((item) => {
        const Icon = item.icon;
        const active =
          item.href === ROUTES.planner.root
            ? pathname === item.href
            : pathname === item.href || pathname.startsWith(`${item.href}/`);

        return (
          <Link
            key={item.href}
            href={item.href}
            title={collapsed ? item.label : undefined}
            onClick={onNavigate}
            className={cn(
              "flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors",
              active
                ? "bg-sidebar-accent text-sidebar-accent-foreground shadow-sm"
                : "text-sidebar-foreground/70 hover:bg-sidebar-accent/70 hover:text-sidebar-accent-foreground",
              collapsed && "justify-center px-0"
            )}
          >
            <Icon className="size-4 shrink-0" />
            {!collapsed && <span>{item.label}</span>}
          </Link>
        );
      })}
    </nav>
  );
}

export function PlannerSidebar() {
  const { sidebarCollapsed, toggleSidebarCollapsed, sidebarOpen, setSidebarOpen } = useUIStore();

  return (
    <>
      <aside
        style={{
          width: sidebarCollapsed ? SIDEBAR_COLLAPSED_WIDTH : SIDEBAR_WIDTH,
        }}
        className="border-border/60 bg-sidebar sticky top-0 hidden h-screen shrink-0 border-r transition-[width] duration-200 ease-out md:flex md:flex-col"
      >
        <div className="flex h-16 items-center gap-2.5 px-4">
          <Link href={ROUTES.home} className="flex min-w-0 items-center gap-2.5">
            <span className="bg-foreground text-background flex size-8 shrink-0 items-center justify-center rounded-xl text-sm font-semibold">
              F
            </span>
            {!sidebarCollapsed && (
              <span className="truncate text-lg font-semibold tracking-tight">FlowSet</span>
            )}
          </Link>
        </div>

        <Separator />

        <div className="flex items-center justify-between gap-2 px-3 py-3">
          {!sidebarCollapsed && (
            <p className="text-muted-foreground px-2 text-xs font-medium tracking-wide uppercase">
              Planner
            </p>
          )}
          <Button
            variant="ghost"
            size="icon-sm"
            onClick={toggleSidebarCollapsed}
            aria-label={sidebarCollapsed ? "Expand sidebar" : "Collapse sidebar"}
            className={cn(sidebarCollapsed && "mx-auto")}
          >
            {sidebarCollapsed ? (
              <ChevronRight className="size-4" />
            ) : (
              <ChevronLeft className="size-4" />
            )}
          </Button>
        </div>

        <ScrollArea className="flex-1 px-2 pb-4">
          <NavList collapsed={sidebarCollapsed} />
        </ScrollArea>
      </aside>

      <Sheet open={sidebarOpen} onOpenChange={setSidebarOpen}>
        <SheetContent side="left" className="w-[300px] p-0 sm:w-[320px]">
          <SheetHeader className="border-border/60 border-b px-4 py-4 text-left">
            <SheetTitle className="flex items-center gap-2.5">
              <span className="bg-foreground text-background flex size-8 items-center justify-center rounded-xl text-sm font-semibold">
                F
              </span>
              FlowSet
            </SheetTitle>
          </SheetHeader>
          <div className="px-2 py-4">
            <NavList onNavigate={() => setSidebarOpen(false)} />
          </div>
        </SheetContent>
      </Sheet>
    </>
  );
}
