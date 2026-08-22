import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";

import { RootShell } from "@/components/layout";
import { ConnectedProductGrid } from "@/components/marketplace";
import { Badge } from "@/components/ui/badge";
import { ROUTES } from "@/constants";
import { createPageMetadata } from "@/lib/metadata";
import { fetchCategoryBySlug, fetchSearch } from "@/lib/marketplace-server";
import { mapProductSummaryToListing } from "@/services/marketplace-api";

interface CategoryPageProps {
  params: Promise<{ slug: string }>;
}

export const dynamic = "force-dynamic";

export async function generateMetadata({ params }: CategoryPageProps): Promise<Metadata> {
  const { slug } = await params;
  const category = await fetchCategoryBySlug(slug);

  if (!category) {
    return createPageMetadata({ title: "Category not found", path: `/categories/${slug}` });
  }

  return createPageMetadata({
    title: category.name,
    description: category.description ?? undefined,
    path: `/categories/${slug}`,
    image: category.imageUrl ?? undefined,
  });
}

export default async function CategoryPage({ params }: CategoryPageProps) {
  const { slug } = await params;
  const category = await fetchCategoryBySlug(slug);

  if (!category) {
    notFound();
  }

  const popular = await fetchSearch({ categoryId: category.id, size: 12 });
  const listings = popular.content.map(mapProductSummaryToListing);

  return (
    <RootShell>
      <div className="border-border/60 bg-muted/30 relative overflow-hidden border-b">
        {category.imageUrl ? (
          <Image
            src={category.imageUrl}
            alt={category.name}
            fill
            sizes="100vw"
            className="object-cover opacity-25"
          />
        ) : null}
        <div className="relative mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
          <p className="text-muted-foreground text-sm font-medium">Category</p>
          <h1 className="font-heading mt-2 text-3xl font-semibold tracking-tight sm:text-4xl">
            {category.name}
          </h1>
          {category.description ? (
            <p className="text-muted-foreground mt-3 max-w-2xl text-base leading-relaxed">
              {category.description}
            </p>
          ) : null}
          <p className="text-muted-foreground mt-4 text-sm tabular-nums">
            {popular.totalElements} rental{popular.totalElements === 1 ? "" : "s"} available
          </p>
        </div>
      </div>

      <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
        {category.children.length > 0 ? (
          <section className="mb-10">
            <h2 className="mb-4 text-lg font-semibold tracking-tight">Subcategories</h2>
            <div className="flex flex-wrap gap-2">
              {category.children.map((child) => (
                <Link key={child.id} href={ROUTES.category(child.slug)}>
                  <Badge
                    variant="secondary"
                    className="hover:bg-foreground hover:text-background rounded-xl px-4 py-2 text-sm font-medium transition-colors"
                  >
                    {child.name}
                  </Badge>
                </Link>
              ))}
            </div>
          </section>
        ) : null}

        <section>
          <div className="mb-5 flex items-center justify-between">
            <h2 className="text-lg font-semibold tracking-tight">Popular in {category.name}</h2>
            <Link
              href={ROUTES.marketplace}
              className="text-muted-foreground hover:text-foreground text-sm font-medium transition-colors"
            >
              Browse full marketplace
            </Link>
          </div>
          <ConnectedProductGrid
            listings={listings}
            emptyTitle="No rentals yet in this category"
            emptyDescription="Vendors are still stocking this category — check back soon."
          />
        </section>
      </div>
    </RootShell>
  );
}
