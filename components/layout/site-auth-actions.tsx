"use client";

import Link from "next/link";

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
import { NotificationCenter } from "@/components/notifications/notification-center";
import { ROUTES } from "@/constants";
import { accountNavForRole } from "@/lib/auth/account-nav";
import { useAuth } from "@/providers";

export function SiteAuthActions({
  onNavigate,
  compact = false,
}: {
  onNavigate?: () => void;
  compact?: boolean;
}) {
  const { isAuthenticated, isLoading, user, role, logout } = useAuth();

  if (isLoading) {
    return <div className="bg-muted/70 h-9 w-28 animate-pulse rounded-xl" aria-hidden />;
  }

  if (!isAuthenticated || !user || role === "guest") {
    return (
      <div className={compact ? "flex flex-col gap-2" : "flex items-center gap-2"}>
        <Button
          variant={compact ? "outline" : "ghost"}
          className={compact ? "rounded-xl" : "hidden rounded-xl sm:inline-flex"}
          asChild
        >
          <Link href={ROUTES.auth.signIn} onClick={onNavigate}>
            Sign in
          </Link>
        </Button>
        <Button className={compact ? "rounded-xl" : "hidden rounded-xl sm:inline-flex"} asChild>
          <Link href={ROUTES.auth.signUp} onClick={onNavigate}>
            Start Planning
          </Link>
        </Button>
      </div>
    );
  }

  const nav = accountNavForRole(role);
  const firstName = user.firstName || "Account";
  const lastName = user.lastName || "";
  const initials =
    `${firstName.charAt(0)}${lastName.charAt(0) || firstName.charAt(1) || "U"}`.toUpperCase();

  if (compact) {
    return (
      <div className="flex flex-col gap-2">
        <Button className="rounded-xl" asChild>
          <Link href={nav.dashboardHref} onClick={onNavigate}>
            {nav.dashboardLabel}
          </Link>
        </Button>
        {nav.extraHref ? (
          <Button variant="outline" className="rounded-xl" asChild>
            <Link href={nav.extraHref} onClick={onNavigate}>
              {nav.extraLabel}
            </Link>
          </Button>
        ) : null}
        <Button
          variant="ghost"
          className="rounded-xl"
          onClick={() => {
            onNavigate?.();
            void logout();
          }}
        >
          Sign out
        </Button>
      </div>
    );
  }

  return (
    <div className="flex items-center gap-1.5 sm:gap-2">
      <NotificationCenter />
      <Button className="hidden rounded-xl sm:inline-flex" asChild>
        <Link href={nav.dashboardHref}>{nav.dashboardLabel}</Link>
      </Button>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" className="gap-2 rounded-xl px-2">
            <Avatar size="sm">
              <AvatarImage src={user.avatarUrl} alt={firstName} />
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
              <span className="text-muted-foreground text-xs font-normal">
                {user.companyName ?? user.email}
              </span>
            </div>
          </DropdownMenuLabel>
          <DropdownMenuSeparator />
          <DropdownMenuItem asChild>
            <Link href={nav.dashboardHref}>{nav.dashboardLabel}</Link>
          </DropdownMenuItem>
          {nav.extraHref ? (
            <DropdownMenuItem asChild>
              <Link href={nav.extraHref}>{nav.extraLabel}</Link>
            </DropdownMenuItem>
          ) : null}
          <DropdownMenuItem asChild>
            <Link href={nav.settingsHref}>Settings</Link>
          </DropdownMenuItem>
          <DropdownMenuSeparator />
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
  );
}
