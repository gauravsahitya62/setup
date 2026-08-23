"use client";

import * as React from "react";
import Link from "next/link";

import { ProductDetailView } from "@/components/marketplace/product-detail-view";
import { Button } from "@/components/ui/button";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { ROUTES } from "@/constants";
import { marketplaceApi, type ProductDto } from "@/services";

export function ProductDetailLoader({ slug }: { slug: string }) {
  const [product, setProduct] = React.useState<ProductDto | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [missing, setMissing] = React.useState(false);

  React.useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setMissing(false);
      try {
        const result = await marketplaceApi.products
          .getBySlug(slug)
          .catch(() => marketplaceApi.products.getById(slug));
        if (cancelled) return;
        if (!result || result.status !== "PUBLISHED") {
          setMissing(true);
          setProduct(null);
        } else {
          setProduct({
            ...result,
            variants: result.variants ?? [],
            images: result.images ?? [],
            attributes: result.attributes ?? [],
            tags: result.tags ?? [],
          });
        }
      } catch {
        if (!cancelled) {
          setMissing(true);
          setProduct(null);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, [slug]);

  if (loading) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
        <SkeletonLoader variant="text" count={2} className="mb-8 max-w-xl" />
        <div className="grid gap-8 lg:grid-cols-2">
          <SkeletonLoader variant="card" count={1} />
          <SkeletonLoader variant="text" count={6} />
        </div>
      </div>
    );
  }

  if (missing || !product) {
    return (
      <div className="mx-auto flex max-w-lg flex-col items-center px-4 py-24 text-center">
        <p className="text-muted-foreground text-sm font-medium tracking-wide uppercase">404</p>
        <h1 className="mt-3 text-3xl font-semibold tracking-tight">Product not found</h1>
        <p className="text-muted-foreground mt-3 text-sm">
          This rental is unavailable or the link is out of date.
        </p>
        <Button asChild className="mt-8 rounded-xl">
          <Link href={ROUTES.marketplace}>Back to marketplace</Link>
        </Button>
      </div>
    );
  }

  return <ProductDetailView product={product} />;
}
