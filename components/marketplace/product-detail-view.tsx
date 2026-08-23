"use client";

import * as React from "react";
import Image from "next/image";
import Link from "next/link";
import {
  BadgeCheck,
  Check,
  FolderPlus,
  Heart,
  Loader2,
  Package,
  Truck,
  Wrench,
} from "lucide-react";

import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import { Badge } from "@/components/ui/badge";
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { AddToProjectDialog } from "@/components/marketplace/add-to-project-dialog";
import { MarketplaceProductGrid } from "@/components/marketplace/marketplace-product-grid";
import { Rating } from "@/components/shared/rating";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { ROUTES } from "@/constants";
import { track } from "@/lib/analytics";
import { useFavorites } from "@/hooks/use-favorites";
import {
  mapProductResponseToListing,
  mapProductSummaryToListing,
  marketplaceApi,
  type AvailabilityCheckDto,
  type ProductDto,
} from "@/services";
import type { MarketplaceListing } from "@/types";
import { cn, formatCurrency } from "@/lib/utils";

interface ProductDetailViewProps {
  product: ProductDto;
}

export function ProductDetailView({ product }: ProductDetailViewProps) {
  const listing = React.useMemo(() => mapProductResponseToListing(product), [product]);
  const images =
    product.images.length > 0
      ? product.images
      : product.featuredImage
        ? [
            {
              id: "featured",
              imageUrl: product.featuredImage,
              altText: product.name,
              displayOrder: 0,
              featured: true,
            },
          ]
        : [];

  const [activeImageIndex, setActiveImageIndex] = React.useState(0);
  const [addOpen, setAddOpen] = React.useState(false);
  const [related, setRelated] = React.useState<MarketplaceListing[]>([]);
  const [relatedLoading, setRelatedLoading] = React.useState(true);
  const [selectedVariantId, setSelectedVariantId] = React.useState(product.variants[0]?.id ?? "");
  const [startDate, setStartDate] = React.useState("");
  const [endDate, setEndDate] = React.useState("");
  const [quantity, setQuantity] = React.useState("1");
  const [availability, setAvailability] = React.useState<AvailabilityCheckDto | null>(null);
  const [checking, setChecking] = React.useState(false);

  const { favoriteIds, toggleFavorite } = useFavorites();
  const isFavorite = favoriteIds.has(product.id);
  const selectedVariant = product.variants.find((v) => v.id === selectedVariantId);

  React.useEffect(() => {
    track("product_viewed", { category: product.categorySlug });
  }, [product.id, product.categorySlug]);

  async function checkAvailability() {
    if (!selectedVariantId || !startDate || !endDate) return;
    setChecking(true);
    try {
      const result = await marketplaceApi.inventory.check(
        selectedVariantId,
        startDate,
        endDate,
        Number(quantity)
      );
      setAvailability(result);
      track("availability_checked", {
        available: result.available,
        requestedQuantity: Number(quantity) || 0,
        availableQuantity: result.availableQuantity,
      });
    } catch (error) {
      setAvailability(null);
      const message = error instanceof Error ? error.message : "Could not check availability";
      setAvailability({
        variantId: selectedVariantId,
        available: false,
        status: "UNAVAILABLE",
        requestedQuantity: Number(quantity) || 0,
        availableQuantity: 0,
        message,
        conflicts: [],
        calendar: [],
      });
    } finally {
      setChecking(false);
    }
  }

  React.useEffect(() => {
    let cancelled = false;
    async function loadRelated() {
      setRelatedLoading(true);
      try {
        const result = await marketplaceApi.search({
          categoryId: product.categoryId,
          size: 8,
        });
        if (!cancelled) {
          setRelated(
            result.content
              .filter((item) => item.id !== product.id)
              .slice(0, 4)
              .map(mapProductSummaryToListing)
          );
        }
      } catch {
        if (!cancelled) setRelated([]);
      } finally {
        if (!cancelled) setRelatedLoading(false);
      }
    }
    void loadRelated();
    return () => {
      cancelled = true;
    };
  }, [product.categoryId, product.id]);

  const activeImage = images[activeImageIndex] ?? images[0];

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <Breadcrumb className="mb-6">
        <BreadcrumbList>
          <BreadcrumbItem>
            <BreadcrumbLink asChild>
              <Link href={ROUTES.marketplace}>Marketplace</Link>
            </BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbSeparator />
          <BreadcrumbItem>
            <BreadcrumbLink asChild>
              <Link href={ROUTES.category(product.categorySlug)}>{product.categoryName}</Link>
            </BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbSeparator />
          <BreadcrumbItem>
            <BreadcrumbPage className="line-clamp-1">{product.name}</BreadcrumbPage>
          </BreadcrumbItem>
        </BreadcrumbList>
      </Breadcrumb>

      <div className="grid gap-10 lg:grid-cols-[1.1fr_0.9fr]">
        <div className="space-y-3">
          <div className="border-border/70 bg-muted relative aspect-[4/3] overflow-hidden rounded-2xl border">
            {activeImage ? (
              <Image
                src={activeImage.imageUrl}
                alt={activeImage.altText ?? product.name}
                fill
                sizes="(max-width: 1024px) 100vw, 55vw"
                className="object-cover"
                priority
              />
            ) : null}
            <Badge
              className={cn(
                "absolute top-3 left-3 rounded-lg border-0 shadow-sm",
                product.status === "PUBLISHED"
                  ? "bg-emerald-500/90 text-white"
                  : "bg-background/90 text-muted-foreground"
              )}
            >
              {product.status === "PUBLISHED" ? "Available" : "Unavailable"}
            </Badge>
          </div>

          {images.length > 1 ? (
            <div className="flex gap-2 overflow-x-auto pb-1">
              {images.map((image, index) => (
                <button
                  key={image.id}
                  type="button"
                  onClick={() => setActiveImageIndex(index)}
                  className={cn(
                    "relative size-20 shrink-0 overflow-hidden rounded-xl border-2 transition-colors",
                    index === activeImageIndex ? "border-foreground" : "border-transparent"
                  )}
                >
                  <Image
                    src={image.imageUrl}
                    alt={image.altText ?? product.name}
                    fill
                    sizes="80px"
                    className="object-cover"
                  />
                </button>
              ))}
            </div>
          ) : null}
        </div>

        <div className="space-y-6">
          <div className="space-y-2">
            <div className="flex flex-wrap items-center gap-2">
              <Badge variant="secondary" className="rounded-lg">
                {product.categoryName}
              </Badge>
              {product.featured ? (
                <Badge className="rounded-lg border-0 bg-amber-500/15 text-amber-700 dark:text-amber-300">
                  Featured
                </Badge>
              ) : null}
            </div>
            <h1 className="font-heading text-3xl font-semibold tracking-tight text-balance">
              {product.name}
            </h1>
            <Link
              href={`${ROUTES.vendors}/${product.vendorSlug}`}
              className="text-muted-foreground hover:text-foreground inline-flex items-center gap-1.5 text-sm font-medium transition-colors"
            >
              {product.vendorName}
              {product.verifiedVendor ? (
                <BadgeCheck className="size-4 text-sky-500" aria-label="Verified vendor" />
              ) : null}
            </Link>
            <Rating
              value={Number(product.vendorRating ?? 0)}
              reviewCount={product.reviewCount}
              size="md"
            />
          </div>

          {product.shortDescription || product.description ? (
            <p className="text-muted-foreground text-base leading-relaxed text-pretty">
              {product.description || product.shortDescription}
            </p>
          ) : null}

          <div className="border-border/70 bg-muted/20 rounded-2xl border p-4">
            <p className="text-3xl font-semibold tracking-tight">
              {formatCurrency(Number(product.pricePerDay ?? 0))}
              <span className="text-muted-foreground text-base font-normal"> / day</span>
            </p>
            <div className="text-muted-foreground mt-3 flex flex-wrap gap-x-4 gap-y-1 text-sm">
              <span>
                Min. rental: {product.minimumRentalDays} day
                {product.minimumRentalDays === 1 ? "" : "s"}
              </span>
              {product.securityDeposit ? (
                <span>Security deposit: {formatCurrency(Number(product.securityDeposit))}</span>
              ) : null}
            </div>
            <div className="mt-3 flex flex-wrap gap-2">
              {product.deliveryAvailable ? (
                <Badge variant="outline" className="gap-1 rounded-lg">
                  <Truck className="size-3.5" /> Delivery
                </Badge>
              ) : null}
              {product.setupAvailable ? (
                <Badge variant="outline" className="gap-1 rounded-lg">
                  <Wrench className="size-3.5" /> Setup
                </Badge>
              ) : null}
              {product.pickupAvailable ? (
                <Badge variant="outline" className="gap-1 rounded-lg">
                  <Package className="size-3.5" /> Pickup
                </Badge>
              ) : null}
            </div>
          </div>

          {product.variants.length > 0 ? (
            <div className="space-y-2">
              <p className="text-sm font-semibold">Available variants</p>
              <div className="space-y-2">
                {product.variants.map((variant) => (
                  <button
                    key={variant.id}
                    type="button"
                    onClick={() => {
                      setSelectedVariantId(variant.id);
                      setAvailability(null);
                    }}
                    className={cn(
                      "flex w-full items-center justify-between gap-3 rounded-xl border px-3 py-2.5 text-left text-sm",
                      variant.id === selectedVariantId ? "border-foreground" : "border-border/70"
                    )}
                  >
                    <div className="min-w-0">
                      <p className="truncate font-medium">{variant.variantName}</p>
                      <p className="text-muted-foreground text-xs">
                        SKU {variant.sku}
                        {variant.color ? ` · ${variant.color}` : ""}
                        {variant.material ? ` · ${variant.material}` : ""}
                      </p>
                    </div>
                    <div className="shrink-0 text-right">
                      <p className="font-medium tabular-nums">
                        {formatCurrency(Number(variant.pricePerDay ?? product.pricePerDay ?? 0))}
                        /day
                      </p>
                      <p className="text-muted-foreground text-xs tabular-nums">
                        {variant.availableQuantity} standing
                      </p>
                    </div>
                  </button>
                ))}
              </div>
            </div>
          ) : null}

          {selectedVariant ? (
            <form
              className="border-border/70 space-y-3 rounded-2xl border p-4"
              onSubmit={(e) => {
                e.preventDefault();
                void checkAvailability();
              }}
            >
              <p className="text-sm font-semibold">Check availability</p>
              <div className="grid gap-3 sm:grid-cols-2">
                <div className="space-y-1.5">
                  <Label htmlFor="rent-start">Start date</Label>
                  <Input
                    id="rent-start"
                    type="date"
                    required
                    value={startDate}
                    onChange={(e) => {
                      setStartDate(e.target.value);
                      setAvailability(null);
                    }}
                    className="rounded-xl"
                  />
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="rent-end">End date</Label>
                  <Input
                    id="rent-end"
                    type="date"
                    required
                    value={endDate}
                    onChange={(e) => {
                      setEndDate(e.target.value);
                      setAvailability(null);
                    }}
                    className="rounded-xl"
                  />
                </div>
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="rent-qty">Quantity</Label>
                <Input
                  id="rent-qty"
                  type="number"
                  min={1}
                  required
                  value={quantity}
                  onChange={(e) => {
                    setQuantity(e.target.value);
                    setAvailability(null);
                  }}
                  className="rounded-xl"
                />
              </div>
              <Button type="submit" variant="outline" className="rounded-xl" disabled={checking}>
                {checking ? <Loader2 className="size-4 animate-spin" /> : null}
                Check availability
              </Button>
              {availability ? (
                <div
                  className={cn(
                    "rounded-xl px-3 py-2 text-sm",
                    availability.status === "AVAILABLE" &&
                      "bg-emerald-500/12 text-emerald-800 dark:text-emerald-300",
                    availability.status === "PARTIAL" &&
                      "bg-amber-500/12 text-amber-900 dark:text-amber-200",
                    availability.status === "UNAVAILABLE" && "bg-muted text-muted-foreground"
                  )}
                >
                  <p className="font-medium">
                    {availability.status === "AVAILABLE"
                      ? "Available"
                      : availability.status === "PARTIAL"
                        ? "Partially available"
                        : "Unavailable"}
                  </p>
                  <p>{availability.message}</p>
                </div>
              ) : null}
            </form>
          ) : null}

          <div className="border-border/70 bg-background/95 shadow-elevated sticky bottom-4 flex flex-col gap-2 rounded-2xl border p-3 backdrop-blur sm:static sm:flex-row sm:border-0 sm:bg-transparent sm:p-0 sm:shadow-none sm:backdrop-blur-none">
            <Button
              size="lg"
              className="flex-1 rounded-xl"
              disabled={product.status !== "PUBLISHED"}
              onClick={() => setAddOpen(true)}
            >
              <FolderPlus className="size-4" />
              Add to Project
            </Button>
            {toggleFavorite ? (
              <Button
                size="lg"
                variant="outline"
                className="rounded-xl"
                onClick={() => toggleFavorite(listing)}
                aria-label={isFavorite ? "Remove from favorites" : "Save to favorites"}
              >
                <Heart className={cn("size-4", isFavorite && "fill-rose-500 text-rose-500")} />
                {isFavorite ? "Saved" : "Save"}
              </Button>
            ) : null}
          </div>
        </div>
      </div>

      {product.attributes.length > 0 || product.tags.length > 0 ? (
        <section className="mt-12 max-w-3xl">
          <h2 className="mb-4 text-xl font-semibold tracking-tight">Specifications</h2>
          {product.attributes.length > 0 ? (
            <Accordion
              type="single"
              collapsible
              defaultValue="specs"
              className="border-border/70 rounded-2xl border px-4"
            >
              <AccordionItem value="specs">
                <AccordionTrigger>Product details</AccordionTrigger>
                <AccordionContent>
                  <dl className="grid gap-2 sm:grid-cols-2">
                    {product.attributes.map((attribute) => (
                      <div
                        key={attribute.id}
                        className="flex items-center justify-between gap-3 text-sm"
                      >
                        <dt className="text-muted-foreground">{attribute.attributeName}</dt>
                        <dd className="font-medium">{attribute.attributeValue}</dd>
                      </div>
                    ))}
                  </dl>
                </AccordionContent>
              </AccordionItem>
            </Accordion>
          ) : null}
          {product.tags.length > 0 ? (
            <div className="mt-4 flex flex-wrap gap-2">
              {product.tags.map((tag) => (
                <Badge key={tag} variant="secondary" className="rounded-lg">
                  <Check className="size-3" />
                  {tag}
                </Badge>
              ))}
            </div>
          ) : null}
        </section>
      ) : null}

      <section className="mt-12">
        <div className="mb-5 flex items-center justify-between">
          <h2 className="text-xl font-semibold tracking-tight">More from this category</h2>
          <Button variant="ghost" size="sm" className="rounded-lg" asChild>
            <Link href={`${ROUTES.category(product.categorySlug)}`}>View category</Link>
          </Button>
        </div>
        {relatedLoading ? (
          <SkeletonLoader
            variant="card"
            count={4}
            className="grid gap-5 space-y-0 sm:grid-cols-2 lg:grid-cols-4"
          />
        ) : (
          <MarketplaceProductGrid
            listings={related}
            favoriteIds={favoriteIds}
            onToggleFavorite={toggleFavorite}
            emptyTitle="No related products yet"
            emptyDescription="Check back soon as more vendors publish inventory in this category."
          />
        )}
      </section>

      <AddToProjectDialog
        listing={listing}
        open={addOpen}
        onOpenChange={setAddOpen}
        variantId={selectedVariantId || undefined}
        quantity={Number(quantity) || 1}
      />
    </div>
  );
}
