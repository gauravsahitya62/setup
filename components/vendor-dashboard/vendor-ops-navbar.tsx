"use client";

import Link from "next/link";
import { Bell, Menu, Plus } from "lucide-react";

import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
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
import { VENDOR_OPERATOR, getVendorDashboardProfile } from "@/constants/vendor-dashboard";
import { ROUTES } from "@/constants";
import { useAuth } from "@/providers";
import { useUIStore } from "@/store";
import { cn } from "@/lib/utils";

interface VendorOpsNavbarProps {
  className?: string;
}

export function VendorOpsNavbar({ className }: VendorOpsNavbarProps) {
  const { setSidebarOpen } = useUIStore();
  const { logout } = useAuth();
  const profile = getVendorDashboardProfile();
  const initials = `${VENDOR_OPERATOR.firstName.charAt(0)}${VENDOR_OPERATOR.lastName.charAt(0)}`;

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

        <div className="hidden max-w-md min-w-0 flex-1 sm:block lg:max-w-lg">
          <SearchBar placeholder="Search inventory, requests, bookings…" />
        </div>

        <div className="ml-auto flex items-center gap-1.5 sm:gap-2">
          <Button className="hidden rounded-xl sm:inline-flex" asChild>
            <Link href={ROUTES.vendor.catalog}>
              <Plus className="size-4" />
              Add product
            </Link>
          </Button>
          <Button size="icon" className="rounded-xl sm:hidden" asChild aria-label="Add product">
            <Link href={ROUTES.vendor.catalog}>
              <Plus className="size-4" />
            </Link>
          </Button>

          <Button
            variant="ghost"
            size="icon"
            className="relative rounded-xl"
            aria-label="Notifications"
          >
            <Bell className="size-4" />
            <Badge className="absolute -top-1 -right-1 h-5 min-w-5 justify-center rounded-md px-1 text-[10px]">
              3
            </Badge>
          </Button>

          <ThemeToggle />

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" className="gap-2 rounded-xl px-2">
                <Avatar size="sm">
                  <AvatarFallback>{initials}</AvatarFallback>
                </Avatar>
                <span className="hidden text-sm font-medium lg:inline">
                  {VENDOR_OPERATOR.firstName}
                </span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56">
              <DropdownMenuLabel>
                <div className="flex flex-col gap-0.5">
                  <span>
                    {VENDOR_OPERATOR.firstName} {VENDOR_OPERATOR.lastName}
                  </span>
                  <span className="text-muted-foreground text-xs font-normal">{profile.name}</span>
                </div>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem asChild>
                <Link href={`${ROUTES.vendors}/${profile.slug}`}>Public profile</Link>
              </DropdownMenuItem>
              <DropdownMenuItem asChild>
                <Link href={ROUTES.vendor.settings}>Settings</Link>
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
