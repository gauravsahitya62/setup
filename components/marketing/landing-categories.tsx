"use client";

import Image from "next/image";
import Link from "next/link";

import { FadeIn } from "@/components/marketing/fade-in";
import { SectionHeading } from "@/components/marketing/section-heading";
import { LANDING_CATEGORIES } from "@/constants/landing";
import { ROUTES } from "@/constants";
import { formatNumber } from "@/lib/utils";

export function LandingCategories() {
  return (
    <section className="mx-auto max-w-7xl px-4 py-20 sm:px-6 sm:py-24 lg:px-8">
      <FadeIn>
        <SectionHeading
          eyebrow="Categories"
          title="Browse by what your event needs"
          description="From seating to structures, explore rental categories curated for professional productions."
        />
      </FadeIn>

      <div className="mt-12 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {LANDING_CATEGORIES.map((category, index) => (
          <FadeIn key={category.id} delay={index * 0.05}>
            <Link
              href={`${ROUTES.categories}/${category.slug}`}
              className="group focus-visible:ring-ring relative block overflow-hidden rounded-2xl focus-visible:ring-2 focus-visible:outline-none"
            >
              <div className="relative aspect-[4/3]">
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
                <h3 className="text-lg font-semibold tracking-tight text-white">{category.name}</h3>
                <p className="mt-1 text-sm text-white/75">
                  {formatNumber(category.productCount)} products
                </p>
              </div>
            </Link>
          </FadeIn>
        ))}
      </div>
    </section>
  );
}
