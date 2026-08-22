"use client";

import Link from "next/link";
import { Menu, Shield } from "lucide-react";

import { NotificationCenter } from "@/components/notifications/notification-center";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { SearchBar } from "@/components/shared/search-bar";
import { ThemeToggle } from "@/components/shared/theme-toggle";
import { ROUTES } from "@/constants";
import { useAuth } from "@/providers";
import { useUIStore } from "@/store";
import { cn } from "@/lib/utils";

interface AdminNavbarProps {
  className?: string;
}

export function AdminNavbar({ className }: AdminNavbarProps) {
  const { setSidebarOpen } = useUIStore();
  const { logout, user } = useAuth();
  const initials = `${(user?.firstName ?? "A").charAt(0)}${(user?.lastName ?? "D").charAt(0)}`;

  return (
    <header
      className={cn(
        "border-border/60 bg-background/85 sticky top-0 z-40 border-b backdrop-blur-xl",
        className
      )}
    >
      <div className="flex h-16 items-center gap-3 px-4 sm:px-6">
        <Button
          variant="ghost"
          size="icon"
          className="md:hidden"
          aria-label="Open sidebar"
          onClick={() => setSidebarOpen(true)}
        >
          <Menu className="size-4" />
        </Button>

        <div className="text-muted-foreground hidden items-center gap-2 text-sm lg:flex">
          <Shield className="size-3.5" />
          <span>Enterprise ops</span>
        </div>

        <div className="hidden max-w-md min-w-0 flex-1 sm:block lg:max-w-lg">
          <SearchBar placeholder="Search vendors, users, tickets…" />
        </div>

        <div className="ml-auto flex items-center gap-1.5 sm:gap-2">
          <NotificationCenter />

          <ThemeToggle />

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" className="gap-2 rounded-xl px-2">
                <Avatar size="sm">
                  <AvatarFallback>{initials}</AvatarFallback>
                </Avatar>
                <span className="hidden text-sm font-medium lg:inline">
                  {user?.firstName ?? "Admin"}
                </span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56">
              <DropdownMenuLabel>
                <div className="flex flex-col gap-0.5">
                  <span>
                    {user?.firstName} {user?.lastName}
                  </span>
                  <span className="text-muted-foreground text-xs font-normal">Administrator</span>
                </div>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem asChild>
                <Link href={ROUTES.admin.settings}>Settings</Link>
              </DropdownMenuItem>
              <DropdownMenuItem asChild>
                <Link href={ROUTES.admin.audit}>Audit logs</Link>
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem asChild>
                <Link href={ROUTES.home}>Back to site</Link>
              </DropdownMenuItem>
              <DropdownMenuItem
                onSelect={(event) => {
                  event.preventDefault();
                  void logout();
                }}
              >
                Sign out
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
    </header>
  );
}
