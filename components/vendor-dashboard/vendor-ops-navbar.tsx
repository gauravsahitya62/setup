"use client";

import Link from "next/link";
import { Menu, Plus } from "lucide-react";

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
import { useMyVendor } from "@/hooks/use-my-vendor";
import { useAuth } from "@/providers";
import { useUIStore } from "@/store";
import { cn } from "@/lib/utils";

interface VendorOpsNavbarProps {
  className?: string;
}

export function VendorOpsNavbar({ className }: VendorOpsNavbarProps) {
  const { setSidebarOpen } = useUIStore();
  const { logout, user } = useAuth();
  const { vendor } = useMyVendor();

  const firstName = user?.firstName ?? "Vendor";
  const lastName = user?.lastName ?? "";
  const companyName = vendor?.companyName ?? "Company profile";
  const publicSlug = vendor?.slug;
  const initials =
    `${firstName.charAt(0)}${lastName.charAt(0) || firstName.charAt(1) || "V"}`.toUpperCase();

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

          <NotificationCenter />

          <ThemeToggle />

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" className="gap-2 rounded-xl px-2">
                <Avatar size="sm">
                  <AvatarFallback>{initials}</AvatarFallback>
                </Avatar>
                <span className="hidden text-sm font-medium lg:inline">{firstName}</span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56">
              <DropdownMenuLabel>
                <div className="flex flex-col gap-0.5">
                  <span>
                    {firstName} {lastName}
                  </span>
                  <span className="text-muted-foreground text-xs font-normal">{companyName}</span>
                </div>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              {publicSlug ? (
                <DropdownMenuItem asChild>
                  <Link href={`${ROUTES.vendors}/${publicSlug}`}>Public profile</Link>
                </DropdownMenuItem>
              ) : (
                <DropdownMenuItem asChild>
                  <Link href={ROUTES.vendor.profileWizard}>Complete profile</Link>
                </DropdownMenuItem>
              )}
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
