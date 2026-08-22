"use client";

import Image from "next/image";
import Link from "next/link";
import { BadgeCheck, Eye, FolderPlus, Heart } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Rating } from "@/components/shared/rating";
import type { MarketplaceListing } from "@/types";
import { ROUTES } from "@/constants";
import { cn, formatCurrency } from "@/lib/utils";

interface MarketplaceProductCardProps {
  listing: MarketplaceListing;
  onQuickView: (listing: MarketplaceListing) => void;
  onAddToProject: (listing: MarketplaceListing) => void;
  isFavorite?: boolean;
  onToggleFavorite?: (listing: MarketplaceListing) => void;
  className?: string;
}

export function MarketplaceProductCard({
  listing,
  onQuickView,
  onAddToProject,
  isFavorite,
  onToggleFavorite,
  className,
}: MarketplaceProductCardProps) {
  return (
    <article
      className={cn(
        "group border-border/70 bg-card shadow-soft flex h-full flex-col overflow-hidden rounded-2xl border transition-all duration-300 hover:-translate-y-1 hover:shadow-md",
        className
      )}
    >
      <div className="bg-muted relative aspect-[4/3] overflow-hidden">
        {listing.imageUrl ? (
          <Image
            src={listing.imageUrl}
            alt={listing.name}
            fill
            sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 25vw"
            className="object-cover transition-transform duration-700 ease-out group-hover:scale-[1.04]"
          />
        ) : null}

        <Badge
          className={cn(
            "absolute top-3 left-3 rounded-lg border-0 shadow-sm",
            listing.available
              ? "bg-emerald-500/90 text-white"
              : "bg-background/90 text-muted-foreground"
          )}
        >
          {listing.available ? "Available" : "Unavailable"}
        </Badge>

        {onToggleFavorite ? (
          <Button
            type="button"
            size="icon-sm"
            variant="secondary"
            className="bg-background/90 absolute top-3 right-3 rounded-full shadow-sm backdrop-blur-sm"
            aria-label={isFavorite ? "Remove from favorites" : "Save to favorites"}
            onClick={(event) => {
              event.preventDefault();
              event.stopPropagation();
              onToggleFavorite(listing);
            }}
          >
            <Heart
              className={cn(
                "size-3.5",
                isFavorite ? "fill-rose-500 text-rose-500" : "text-foreground"
              )}
            />
          </Button>
        ) : null}

        <div className="absolute inset-x-3 bottom-3 flex gap-2 opacity-0 transition-opacity duration-300 group-hover:opacity-100 max-sm:opacity-100">
          <Button
            size="sm"
            variant="secondary"
            className="bg-background/95 shadow-soft flex-1 rounded-xl backdrop-blur-sm"
            onClick={() => onQuickView(listing)}
          >
            <Eye className="size-3.5" />
            Quick View
          </Button>
          <Button
            size="sm"
            className="shadow-soft flex-1 rounded-xl"
            disabled={!listing.available}
            onClick={() => onAddToProject(listing)}
          >
            <FolderPlus className="size-3.5" />
            Add
          </Button>
        </div>
      </div>

      <div className="flex flex-1 flex-col gap-3 p-4">
        <div className="space-y-1">
          <Link
            href={`${ROUTES.marketplace}/${listing.slug}`}
            className="line-clamp-1 text-base font-semibold tracking-tight transition-colors hover:underline"
          >
            {listing.name}
          </Link>
          <Link
            href={`${ROUTES.vendors}/${listing.vendorSlug}`}
            className="text-muted-foreground hover:text-foreground inline-flex items-center gap-1 text-sm transition-colors"
          >
            {listing.vendorName}
            {listing.vendorVerified ? (
              <BadgeCheck className="size-3.5 text-sky-500" aria-label="Verified vendor" />
            ) : null}
          </Link>
        </div>

        <Rating value={listing.rating} reviewCount={listing.reviewCount} />

        <div className="mt-auto flex items-end justify-between gap-3 pt-1">
          <div>
            <p className="text-base font-semibold tracking-tight">
              {formatCurrency(listing.dailyRate, listing.currency)}
              <span className="text-muted-foreground text-sm font-normal"> / day</span>
            </p>
            <p className="text-muted-foreground text-xs">{listing.city}</p>
          </div>
          <div className="flex gap-1.5 sm:hidden">
            <Button
              size="icon-sm"
              variant="outline"
              className="rounded-xl"
              aria-label="Quick view"
              onClick={() => onQuickView(listing)}
            >
              <Eye className="size-3.5" />
            </Button>
            <Button
              size="icon-sm"
              className="rounded-xl"
              aria-label="Add to project"
              disabled={!listing.available}
              onClick={() => onAddToProject(listing)}
            >
              <FolderPlus className="size-3.5" />
            </Button>
          </div>
        </div>
      </div>
    </article>
  );
}
