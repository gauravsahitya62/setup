"use client";

import * as React from "react";
import Link from "next/link";
import { BadgeCheck, Flag, MessageSquare, Send, Share2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Rating } from "@/components/shared/rating";
import type { VendorProfile } from "@/types";
import { ROUTES } from "@/constants";
import { cn } from "@/lib/utils";

interface VendorBookingCtaProps {
  vendor: VendorProfile;
  className?: string;
  sticky?: boolean;
}

export function VendorBookingCta({ vendor, className, sticky = false }: VendorBookingCtaProps) {
  const verified = vendor.verificationStatus === "verified";

  const handleRequest = () => {
    toast.success("Booking request started", {
      description: `Continue in your planner workspace to request ${vendor.name}.`,
    });
  };

  const handleShare = async () => {
    const url = typeof window !== "undefined" ? window.location.href : "";
    try {
      if (navigator.share) {
        await navigator.share({ title: vendor.name, url });
      } else if (navigator.clipboard) {
        await navigator.clipboard.writeText(url);
        toast.success("Link copied");
      }
    } catch {
      toast.message("Share cancelled");
    }
  };

  const handleReport = () => {
    toast.message("Report submitted", {
      description: "Our trust & safety team will review this vendor profile.",
    });
  };

  const contactHref = vendor.social.website ?? ROUTES.planner.root;

  return (
    <Card
      className={cn(
        sticky &&
          "shadow-elevated md:shadow-soft fixed inset-x-0 bottom-0 z-40 rounded-none border-x-0 border-b-0 md:static md:rounded-2xl md:border",
        className
      )}
    >
      <CardContent className={cn("p-4 sm:p-5", sticky && "mx-auto max-w-7xl md:max-w-none")}>
        <div
          className={cn(
            sticky
              ? "flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between md:flex-col md:items-stretch"
              : "space-y-4"
          )}
        >
          <div className={cn(!sticky && "space-y-2", sticky && "min-w-0")}>
            <div className="flex flex-wrap items-center gap-2">
              <p className="font-semibold tracking-tight">{vendor.name}</p>
              {verified ? (
                <BadgeCheck className="size-4 text-sky-500" aria-label="Verified" />
              ) : null}
            </div>
            <Rating value={vendor.rating} reviewCount={vendor.reviewCount} />
            {typeof vendor.responseTimeHours === "number" ? (
              <p className="text-muted-foreground text-xs">
                Typical response · ~{vendor.responseTimeHours} hours
              </p>
            ) : null}
          </div>

          <div className={cn("flex gap-2", sticky ? "shrink-0 flex-wrap" : "flex-col")}>
            <Button className="rounded-xl" onClick={handleRequest}>
              <Send className="size-4" />
              Request booking
            </Button>
            <Button variant="outline" className="rounded-xl" asChild>
              <a
                href={contactHref}
                target={vendor.social.website ? "_blank" : undefined}
                rel="noopener noreferrer"
              >
                <MessageSquare className="size-4" />
                Contact
              </a>
            </Button>
            {!sticky ? (
              <>
                <Button variant="outline" className="rounded-xl" onClick={() => void handleShare()}>
                  <Share2 className="size-4" />
                  Share
                </Button>
                <Button variant="ghost" className="rounded-xl" onClick={handleReport}>
                  <Flag className="size-4" />
                  Report vendor
                </Button>
                <Button variant="link" className="justify-start px-0" asChild>
                  <Link href={ROUTES.planner.root}>Open planner workspace</Link>
                </Button>
              </>
            ) : null}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
