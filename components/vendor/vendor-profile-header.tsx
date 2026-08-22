"use client";

import * as React from "react";
import Image from "next/image";
import { BadgeCheck, Clock3, MapPin } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Rating } from "@/components/shared/rating";
import { SaveVendorDialog } from "@/components/planner/save-vendor-dialog";
import type { VendorProfile } from "@/types";
import { formatNumber } from "@/lib/utils";

interface VendorProfileHeaderProps {
  vendor: VendorProfile;
}

export function VendorProfileHeader({ vendor }: VendorProfileHeaderProps) {
  const [saveOpen, setSaveOpen] = React.useState(false);
  const initials = vendor.name
    .split(" ")
    .map((part) => part.charAt(0))
    .join("")
    .slice(0, 2)
    .toUpperCase();

  const verified = vendor.verificationStatus === "verified";

  return (
    <section className="border-border/60 bg-card shadow-soft overflow-hidden rounded-3xl border">
      <div className="relative h-48 sm:h-64 md:h-80">
        {vendor.coverImageUrl ? (
          <Image
            src={vendor.coverImageUrl}
            alt={`${vendor.name} banner`}
            fill
            priority
            sizes="100vw"
            className="object-cover"
          />
        ) : (
          <div className="from-muted to-muted/40 size-full bg-gradient-to-br" />
        )}
        <div className="absolute inset-0 bg-gradient-to-t from-black/55 via-black/15 to-transparent" />
      </div>

      <div className="relative px-5 pb-6 sm:px-8 sm:pb-8">
        <div className="-mt-12 flex flex-col gap-5 sm:-mt-14 sm:flex-row sm:items-end sm:justify-between">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-end">
            <div className="border-background bg-muted shadow-elevated relative size-24 overflow-hidden rounded-2xl border-4 sm:size-28">
              {vendor.logoUrl ? (
                <Image
                  src={vendor.logoUrl}
                  alt={`${vendor.name} logo`}
                  fill
                  sizes="112px"
                  className="object-cover"
                />
              ) : (
                <div className="flex size-full items-center justify-center text-xl font-semibold">
                  {initials}
                </div>
              )}
            </div>

            <div className="min-w-0 pb-1">
              <div className="flex flex-wrap items-center gap-2">
                <h1 className="font-heading text-2xl font-semibold tracking-tight sm:text-3xl">
                  {vendor.name}
                </h1>
                {verified ? (
                  <Badge className="rounded-lg border-0 bg-sky-500/15 text-sky-700 dark:text-sky-300">
                    <BadgeCheck className="size-3.5" />
                    Verified
                  </Badge>
                ) : null}
              </div>
              {vendor.tagline ? (
                <p className="text-muted-foreground mt-1 text-sm sm:text-base">{vendor.tagline}</p>
              ) : null}
              <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-2">
                <Rating value={vendor.rating} reviewCount={vendor.reviewCount} size="md" />
                <span className="text-muted-foreground inline-flex items-center gap-1.5 text-sm">
                  <MapPin className="size-3.5" />
                  {vendor.location}
                </span>
                {typeof vendor.responseTimeHours === "number" ? (
                  <span className="text-muted-foreground inline-flex items-center gap-1.5 text-sm">
                    <Clock3 className="size-3.5" />
                    Responds in ~{vendor.responseTimeHours}h
                  </span>
                ) : null}
              </div>
            </div>
          </div>

          <div className="flex flex-wrap gap-2">
            <Button className="rounded-xl" onClick={() => setSaveOpen(true)}>
              Save to project
            </Button>
            {vendor.categories.map((category) => (
              <Badge key={category} variant="secondary" className="rounded-lg">
                {category}
              </Badge>
            ))}
          </div>
        </div>

        <div className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
          {[
            { label: "Completed events", value: formatNumber(vendor.completedEvents) },
            { label: "Years in business", value: String(vendor.yearsInBusiness) },
            {
              label: "Response time",
              value: vendor.responseTimeHours ? `~${vendor.responseTimeHours}h` : "—",
            },
            { label: "Service cities", value: String(vendor.serviceCities.length) },
          ].map((stat) => (
            <div
              key={stat.label}
              className="border-border/60 bg-muted/30 rounded-2xl border px-4 py-3"
            >
              <p className="text-xl font-semibold tracking-tight tabular-nums sm:text-2xl">
                {stat.value}
              </p>
              <p className="text-muted-foreground mt-1 text-xs">{stat.label}</p>
            </div>
          ))}
        </div>
      </div>
      <SaveVendorDialog
        vendorId={vendor.id}
        vendorName={vendor.name}
        open={saveOpen}
        onOpenChange={setSaveOpen}
      />
    </section>
  );
}
