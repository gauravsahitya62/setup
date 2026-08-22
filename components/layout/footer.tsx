import Link from "next/link";

import { Separator } from "@/components/ui/separator";
import { ROUTES } from "@/constants";
import { cn } from "@/lib/utils";

interface FooterProps {
  className?: string;
}

const footerLinks = {
  Product: [
    { href: ROUTES.marketplace, label: "Marketplace" },
    { href: ROUTES.vendors, label: "Vendors" },
    { href: ROUTES.categories, label: "Categories" },
  ],
  Company: [
    { href: "/about", label: "About" },
    { href: "/careers", label: "Careers" },
    { href: "/contact", label: "Contact" },
  ],
  Legal: [
    { href: "/privacy", label: "Privacy" },
    { href: "/terms", label: "Terms" },
  ],
};

export function Footer({ className }: FooterProps) {
  return (
    <footer className={cn("border-border/60 bg-muted/30 border-t", className)}>
      <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
        <div className="grid gap-10 md:grid-cols-4">
          <div className="md:col-span-1">
            <Link href={ROUTES.home} className="flex items-center gap-2.5">
              <span className="bg-primary text-primary-foreground flex size-8 items-center justify-center rounded-xl text-sm font-semibold">
                F
              </span>
              <span className="text-lg font-semibold tracking-tight">FlowSet</span>
            </Link>
            <p className="text-muted-foreground mt-4 max-w-xs text-sm leading-relaxed">
              Powering every event. Discover verified rental vendors and request bookings in one
              place.
            </p>
          </div>

          {Object.entries(footerLinks).map(([group, links]) => (
            <div key={group}>
              <p className="text-sm font-semibold tracking-tight">{group}</p>
              <ul className="mt-4 space-y-2.5">
                {links.map((link) => (
                  <li key={link.href}>
                    <Link
                      href={link.href}
                      className="text-muted-foreground hover:text-foreground text-sm transition-colors"
                    >
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <Separator className="my-8" />

        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-muted-foreground text-sm">
            © {new Date().getFullYear()} FlowSet. All rights reserved.
          </p>
          <p className="text-muted-foreground text-sm">Built for the event industry.</p>
        </div>
      </div>
    </footer>
  );
}
