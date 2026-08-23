"use client";

import * as React from "react";
import Link from "next/link";

import { FadeIn } from "@/components/marketing/fade-in";
import { SectionHeading } from "@/components/marketing/section-heading";
import { EmptyState } from "@/components/shared/empty-state";
import { ROUTES } from "@/constants";
import { marketplaceApi, type CategoryDto } from "@/services";

export function LandingCategories() {
  const [categories, setCategories] = React.useState<CategoryDto[]>([]);
  const [loaded, setLoaded] = React.useState(false);

  React.useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const tree = await marketplaceApi.categories.tree();
        if (!cancelled) setCategories(tree.filter((category) => !category.parentCategoryId));
      } catch {
        if (!cancelled) setCategories([]);
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
      <FadeIn>
        <SectionHeading
          eyebrow="Categories"
          title="Browse by what your event needs"
          description="From seating to structures, explore rental categories curated for professional productions."
        />
      </FadeIn>

      <div className="mt-12">
        {!loaded ? (
          <p className="text-muted-foreground text-sm">Loading categories…</p>
        ) : categories.length === 0 ? (
          <EmptyState
            title="Categories coming soon"
            description="Marketplace categories appear here after the catalog is published."
          />
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {categories.map((category, index) => (
              <FadeIn key={category.id} delay={index * 0.05}>
                <Link
                  href={`${ROUTES.categories}/${category.slug}`}
                  className="group border-border/70 bg-muted/30 hover:bg-muted/50 focus-visible:ring-ring relative block overflow-hidden rounded-2xl border p-5 focus-visible:ring-2 focus-visible:outline-none"
                >
                  <h3 className="text-lg font-semibold tracking-tight">{category.name}</h3>
                  {category.description ? (
                    <p className="text-muted-foreground mt-1 text-sm">{category.description}</p>
                  ) : null}
                </Link>
              </FadeIn>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
