"use client";

import * as React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  LayoutDashboard,
  MessageSquare,
  Package,
  Settings,
  Store,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { SIDEBAR_COLLAPSED_WIDTH, SIDEBAR_WIDTH } from "@/constants";
import { cn } from "@/lib/utils";
import { useUIStore } from "@/store";

export interface SidebarItem {
  href: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
}

const defaultItems: SidebarItem[] = [
  { href: "/planner", label: "Dashboard", icon: LayoutDashboard },
  { href: "/planner/projects", label: "Projects", icon: CalendarDays },
  { href: "/marketplace", label: "Marketplace", icon: Store },
  { href: "/planner/bookings", label: "Bookings", icon: Package },
  { href: "/planner/messages", label: "Messages", icon: MessageSquare },
  { href: "/settings", label: "Settings", icon: Settings },
];

interface SidebarProps {
  items?: SidebarItem[];
  className?: string;
}

export function Sidebar({ items = defaultItems, className }: SidebarProps) {
  const pathname = usePathname();
  const { sidebarCollapsed, toggleSidebarCollapsed } = useUIStore();

  return (
    <aside
      style={{
        width: sidebarCollapsed ? SIDEBAR_COLLAPSED_WIDTH : SIDEBAR_WIDTH,
      }}
      className={cn(
        "border-border/60 bg-sidebar sticky top-16 hidden h-[calc(100vh-4rem)] shrink-0 border-r transition-[width] duration-200 ease-out md:flex md:flex-col",
        className
      )}
    >
      <div className="flex items-center justify-between gap-2 px-3 py-4">
        {!sidebarCollapsed && (
          <p className="text-muted-foreground px-2 text-xs font-medium tracking-wide uppercase">
            Workspace
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

      <Separator />

      <ScrollArea className="flex-1 px-2 py-3">
        <nav className="flex flex-col gap-1">
          {items.map((item) => {
            const Icon = item.icon;
            const active = pathname === item.href || pathname.startsWith(`${item.href}/`);

            return (
              <Link
                key={item.href}
                href={item.href}
                title={sidebarCollapsed ? item.label : undefined}
                className={cn(
                  "flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors",
                  active
                    ? "bg-sidebar-accent text-sidebar-accent-foreground shadow-sm"
                    : "text-sidebar-foreground/70 hover:bg-sidebar-accent/70 hover:text-sidebar-accent-foreground",
                  sidebarCollapsed && "justify-center px-0"
                )}
              >
                <Icon className="size-4 shrink-0" />
                {!sidebarCollapsed && <span>{item.label}</span>}
              </Link>
            );
          })}
        </nav>
      </ScrollArea>
    </aside>
  );
}
