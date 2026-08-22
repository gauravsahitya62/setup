import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";

import { RootShell } from "@/components/layout";
import { EmptyState } from "@/components/shared/empty-state";
import { ROUTES } from "@/constants";
import { createPageMetadata } from "@/lib/metadata";
import { fetchCategoryTree } from "@/lib/marketplace-server";

export const metadata: Metadata = createPageMetadata({
  title: "Categories",
  description: "Browse FlowSet rental categories — seating, lighting, structures, and more.",
  path: "/categories",
});

export const dynamic = "force-dynamic";

export default async function CategoriesIndexPage() {
  const categories = await fetchCategoryTree();

  return (
    <RootShell>
      <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
        <div className="max-w-2xl">
          <p className="text-muted-foreground text-sm font-medium">Categories</p>
          <h1 className="font-heading mt-2 text-3xl font-semibold tracking-tight sm:text-4xl">
            Browse by category
          </h1>
          <p className="text-muted-foreground mt-3 text-base leading-relaxed">
            From seating to structures, explore rental categories curated for professional
            productions.
          </p>
        </div>

        {categories.length === 0 ? (
          <div className="mt-10">
            <EmptyState
              title="No categories yet"
              description="Categories will appear here once the catalog is seeded."
            />
          </div>
        ) : (
          <div className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {categories.map((category) => (
              <Link
                key={category.id}
                href={ROUTES.category(category.slug)}
                className="group border-border/70 focus-visible:ring-ring relative block overflow-hidden rounded-2xl border focus-visible:ring-2 focus-visible:outline-none"
              >
                <div className="bg-muted relative aspect-[4/3]">
                  {category.imageUrl ? (
                    <Image
                      src={category.imageUrl}
                      alt={category.name}
                      fill
                      sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 33vw"
                      className="object-cover transition-transform duration-700 ease-out group-hover:scale-[1.04]"
                    />
                  ) : null}
                  <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/25 to-transparent" />
                </div>
                <div className="absolute inset-x-0 bottom-0 p-5">
                  <h3 className="text-lg font-semibold tracking-tight text-white">
                    {category.name}
                  </h3>
                  {category.children.length > 0 ? (
                    <p className="mt-1 text-sm text-white/75">
                      {category.children.length} subcategor
                      {category.children.length === 1 ? "y" : "ies"}
                    </p>
                  ) : null}
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </RootShell>
  );
}
