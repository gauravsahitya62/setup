"use client";

import * as React from "react";
import Link from "next/link";
import { Menu, X } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet";
import { ThemeToggle } from "@/components/shared/theme-toggle";
import { SiteAuthActions } from "@/components/layout/site-auth-actions";
import { ROUTES } from "@/constants";
import { cn } from "@/lib/utils";

const navLinks = [
  { href: ROUTES.marketplace, label: "Marketplace" },
  { href: ROUTES.vendors, label: "Vendors" },
  { href: ROUTES.categories, label: "Categories" },
];

interface NavbarProps {
  className?: string;
}

export function Navbar({ className }: NavbarProps) {
  const [open, setOpen] = React.useState(false);

  return (
    <header
      className={cn(
        "border-border/50 bg-background/75 sticky top-0 z-50 w-full border-b backdrop-blur-xl",
        className
      )}
    >
      <div className="mx-auto flex h-16 max-w-7xl items-center gap-4 px-4 sm:px-6 lg:px-8">
        <Link href={ROUTES.home} className="flex items-center gap-2.5">
          <span className="bg-foreground text-background flex size-8 items-center justify-center rounded-xl text-sm font-semibold">
            F
          </span>
          <span className="text-lg font-semibold tracking-tight">FlowSet</span>
        </Link>

        <nav className="ml-8 hidden items-center gap-1 md:flex">
          {navLinks.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className="text-muted-foreground hover:bg-muted/80 hover:text-foreground rounded-xl px-3 py-2 text-sm font-medium transition-colors"
            >
              {link.label}
            </Link>
          ))}
        </nav>

        <div className="ml-auto flex items-center gap-2">
          <ThemeToggle />
          <SiteAuthActions />

          <Sheet open={open} onOpenChange={setOpen}>
            <SheetTrigger asChild>
              <Button variant="ghost" size="icon" className="md:hidden" aria-label="Open menu">
                {open ? <X className="size-4" /> : <Menu className="size-4" />}
              </Button>
            </SheetTrigger>
            <SheetContent side="right" className="w-[300px] sm:w-[340px]">
              <SheetHeader>
                <SheetTitle>Menu</SheetTitle>
              </SheetHeader>
              <nav className="mt-6 flex flex-col gap-1">
                {navLinks.map((link) => (
                  <Link
                    key={link.href}
                    href={link.href}
                    onClick={() => setOpen(false)}
                    className="text-muted-foreground hover:bg-muted hover:text-foreground rounded-xl px-3 py-2.5 text-sm font-medium transition-colors"
                  >
                    {link.label}
                  </Link>
                ))}
              </nav>
              <div className="mt-6">
                <SiteAuthActions compact onNavigate={() => setOpen(false)} />
              </div>
            </SheetContent>
          </Sheet>
        </div>
      </div>
    </header>
  );
}
