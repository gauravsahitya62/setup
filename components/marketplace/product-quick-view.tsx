"use client";

import Image from "next/image";
import Link from "next/link";
import { BadgeCheck, FolderPlus, MapPin } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Rating } from "@/components/shared/rating";
import type { MarketplaceListing } from "@/types";
import { ROUTES } from "@/constants";
import { formatCurrency } from "@/lib/utils";

interface ProductQuickViewProps {
  listing: MarketplaceListing | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onAddToProject: (listing: MarketplaceListing) => void;
}

export function ProductQuickView({
  listing,
  open,
  onOpenChange,
  onAddToProject,
}: ProductQuickViewProps) {
  if (!listing) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl gap-0 overflow-hidden p-0 sm:max-w-3xl">
        <div className="grid md:grid-cols-2">
          <div className="bg-muted relative aspect-[4/3] md:aspect-auto md:min-h-[360px]">
            {listing.imageUrl ? (
              <Image
                src={listing.imageUrl}
                alt={listing.name}
                fill
                sizes="(max-width: 768px) 100vw, 50vw"
                className="object-cover"
              />
            ) : null}
          </div>

          <div className="flex flex-col p-6">
            <DialogHeader className="text-left">
              <div className="mb-2 flex flex-wrap gap-2">
                <Badge variant="secondary" className="rounded-lg">
                  {listing.categoryName}
                </Badge>
                <Badge
                  className={
                    listing.available
                      ? "rounded-lg border-0 bg-emerald-500/15 text-emerald-700 dark:text-emerald-300"
                      : "rounded-lg"
                  }
                  variant={listing.available ? "secondary" : "outline"}
                >
                  {listing.available ? "Available" : "Unavailable"}
                </Badge>
              </div>
              <DialogTitle className="text-2xl tracking-tight">{listing.name}</DialogTitle>
              <DialogDescription className="text-base leading-relaxed">
                {listing.description}
              </DialogDescription>
            </DialogHeader>

            <div className="mt-5 space-y-4">
              <Link
                href={`${ROUTES.vendors}/${listing.vendorSlug}`}
                className="inline-flex items-center gap-1.5 text-sm font-medium hover:underline"
              >
                {listing.vendorName}
                {listing.vendorVerified ? (
                  <BadgeCheck className="size-4 text-sky-500" aria-label="Verified" />
                ) : null}
              </Link>

              <Rating value={listing.rating} reviewCount={listing.reviewCount} size="md" />

              <div className="text-muted-foreground flex items-center gap-2 text-sm">
                <MapPin className="size-3.5" />
                {listing.city}
              </div>

              <div>
                <p className="text-2xl font-semibold tracking-tight">
                  {formatCurrency(listing.dailyRate, listing.currency)}
                  <span className="text-muted-foreground text-sm font-normal"> / day</span>
                </p>
              </div>
            </div>

            <div className="mt-auto flex flex-col gap-2 pt-8 sm:flex-row">
              <Button
                className="flex-1 rounded-xl"
                disabled={!listing.available}
                onClick={() => {
                  onAddToProject(listing);
                  onOpenChange(false);
                }}
              >
                <FolderPlus className="size-4" />
                Add to Project
              </Button>
              <Button variant="outline" className="flex-1 rounded-xl" asChild>
                <Link href={`${ROUTES.marketplace}/${listing.slug}`}>View details</Link>
              </Button>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
