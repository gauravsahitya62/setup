"use client";

import * as React from "react";
import Link from "next/link";
import { Menu, Plus } from "lucide-react";

import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
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
import { NotificationCenter } from "@/components/notifications/notification-center";
import { QuickAddProjectDialog } from "@/components/planner/quick-add-project-dialog";
import { PLANNER_USER } from "@/constants/planner";
import { ROUTES } from "@/constants";
import { cn } from "@/lib/utils";
import { useAuth } from "@/providers";
import { useUIStore } from "@/store";

interface PlannerNavbarProps {
  className?: string;
}

export function PlannerNavbar({ className }: PlannerNavbarProps) {
  const { setSidebarOpen } = useUIStore();
  const { logout } = useAuth();
  const [addOpen, setAddOpen] = React.useState(false);
  const initials = `${PLANNER_USER.firstName.charAt(0)}${PLANNER_USER.lastName.charAt(0)}`;

  return (
    <>
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

          <div className="hidden max-w-md min-w-0 flex-1 sm:block lg:max-w-lg">
            <SearchBar placeholder="Search projects, vendors, requests…" />
          </div>

          <div className="ml-auto flex items-center gap-1.5 sm:gap-2">
            <Button className="hidden rounded-xl sm:inline-flex" onClick={() => setAddOpen(true)}>
              <Plus className="size-4" />
              Quick Add Project
            </Button>
            <Button
              size="icon"
              className="rounded-xl sm:hidden"
              aria-label="Quick add project"
              onClick={() => setAddOpen(true)}
            >
              <Plus className="size-4" />
            </Button>

            <NotificationCenter />

            <ThemeToggle />

            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" className="gap-2 rounded-xl px-2">
                  <Avatar size="sm">
                    <AvatarImage src={PLANNER_USER.avatarUrl} alt={PLANNER_USER.firstName} />
                    <AvatarFallback>{initials}</AvatarFallback>
                  </Avatar>
                  <span className="hidden text-sm font-medium lg:inline">
                    {PLANNER_USER.firstName}
                  </span>
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56">
                <DropdownMenuLabel>
                  <div className="flex flex-col gap-0.5">
                    <span>
                      {PLANNER_USER.firstName} {PLANNER_USER.lastName}
                    </span>
                    <span className="text-muted-foreground text-xs font-normal">
                      {PLANNER_USER.companyName}
                    </span>
                  </div>
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem asChild>
                  <Link href={ROUTES.planner.settings}>Settings</Link>
                </DropdownMenuItem>
                <DropdownMenuItem asChild>
                  <Link href={ROUTES.planner.projects}>My projects</Link>
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

      <QuickAddProjectDialog open={addOpen} onOpenChange={setAddOpen} />
    </>
  );
}
