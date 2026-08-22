"use client";

import Link from "next/link";
import { Building2, FileText, ImageIcon, MapPin, Settings, Share2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { NotificationPreferencesForm } from "@/components/notifications/notification-preferences-form";
import { ROUTES } from "@/constants";

const LINKS = [
  {
    href: ROUTES.vendor.profile,
    title: "Company profile",
    description: "View status, publish state, and core business details.",
    icon: Building2,
  },
  {
    href: ROUTES.vendor.profileWizard,
    title: "Profile wizard",
    description: "Guided setup for business info, media, and publish.",
    icon: Settings,
  },
  {
    href: ROUTES.vendor.serviceAreas,
    title: "Service areas",
    description: "Cities and states you fulfill.",
    icon: MapPin,
  },
  {
    href: ROUTES.vendor.portfolio,
    title: "Portfolio",
    description: "Past event imagery for trust.",
    icon: ImageIcon,
  },
  {
    href: ROUTES.vendor.documents,
    title: "Documents",
    description: "GST, license, PAN, and identity uploads.",
    icon: FileText,
  },
  {
    href: ROUTES.vendor.social,
    title: "Social links",
    description: "Instagram, LinkedIn, and more.",
    icon: Share2,
  },
] as const;

export default function VendorSettingsPage() {
  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">Settings</h1>
        <p className="text-muted-foreground text-sm">
          Manage your storefront and in-app notification preferences.
        </p>
      </div>

      <NotificationPreferencesForm />

      <div className="grid gap-4 sm:grid-cols-2">
        {LINKS.map((item) => {
          const Icon = item.icon;
          return (
            <Card key={item.href} className="rounded-2xl shadow-sm">
              <CardHeader>
                <div className="bg-muted text-muted-foreground mb-2 flex size-9 items-center justify-center rounded-xl">
                  <Icon className="size-4" />
                </div>
                <CardTitle className="text-base">{item.title}</CardTitle>
                <CardDescription>{item.description}</CardDescription>
              </CardHeader>
              <CardContent>
                <Button className="rounded-xl" asChild>
                  <Link href={item.href}>Open</Link>
                </Button>
              </CardContent>
            </Card>
          );
        })}
      </div>
    </div>
  );
}
