"use client";

import * as React from "react";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { FadeIn } from "@/components/marketing/fade-in";
import { SectionHeading } from "@/components/marketing/section-heading";
import { ProductCard } from "@/components/vendor/product-card";
import { EmptyState } from "@/components/shared/empty-state";
import { ROUTES } from "@/constants";
import { marketplaceApi, type ProductSummaryDto } from "@/services";
import type { Product } from "@/types";

function toProduct(summary: ProductSummaryDto): Product {
  const now = new Date().toISOString();
  return {
    id: summary.id,
    vendorId: summary.vendorId,
    name: summary.name,
    slug: summary.slug,
    description: summary.shortDescription ?? "",
    imageUrl: summary.featuredImage ?? undefined,
    categoryId: summary.categoryId,
    dailyRate: Number(summary.pricePerDay ?? 0),
    currency: "INR",
    available: summary.available,
    createdAt: now,
    updatedAt: now,
  };
}

export function LandingRentals() {
  const [products, setProducts] = React.useState<Product[]>([]);
  const [loaded, setLoaded] = React.useState(false);

  React.useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const page = await marketplaceApi.search({ sort: "POPULARITY", size: 4 });
        if (!cancelled) setProducts(page.content.map(toProduct));
      } catch {
        if (!cancelled) setProducts([]);
      } finally {
        if (!cancelled) setLoaded(true);
      }
    }
    void load();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <section className="mx-auto max-w-7xl px-4 py-20 sm:px-6 sm:py-24 lg:px-8">
      <FadeIn className="flex flex-col gap-6 sm:flex-row sm:items-end sm:justify-between">
        <SectionHeading
          eyebrow="Popular rentals"
          title="Inventory planners reach for first"
          description="A snapshot of high-demand pieces across seating, tables, lighting, and lounge."
        />
        <Button asChild variant="outline" className="self-start rounded-xl sm:self-auto">
          <Link href={ROUTES.marketplace}>Browse marketplace</Link>
        </Button>
      </FadeIn>

      <div className="mt-12">
        {!loaded ? (
          <p className="text-muted-foreground text-sm">Loading rentals…</p>
        ) : products.length === 0 ? (
          <EmptyState
            title="Rentals coming soon"
            description="Published inventory will appear here as vendors publish their catalogs."
          />
        ) : (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {products.map((product, index) => (
              <FadeIn key={product.id} delay={index * 0.06}>
                <ProductCard product={product} href={ROUTES.product(product.slug)} />
              </FadeIn>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
