"use client";

import { cn } from "@/lib/utils";

interface CategoryChipsProps {
  categories: { slug: string; label: string }[];
  value: string;
  onChange: (slug: string) => void;
  className?: string;
}

export function CategoryChips({ categories, value, onChange, className }: CategoryChipsProps) {
  return (
    <div
      className={cn(
        "flex [scrollbar-width:none] gap-2 overflow-x-auto pb-1 [-ms-overflow-style:none] [&::-webkit-scrollbar]:hidden",
        className
      )}
    >
      {categories.map((category) => {
        const active = category.slug === value;
        return (
          <button
            key={category.slug}
            type="button"
            onClick={() => onChange(category.slug)}
            className={cn(
              "shrink-0 rounded-xl border px-4 py-2 text-sm font-medium transition-all",
              active
                ? "border-foreground bg-foreground text-background shadow-soft"
                : "border-border/80 bg-background text-muted-foreground hover:border-foreground/30 hover:text-foreground"
            )}
          >
            {category.label}
          </button>
        );
      })}
    </div>
  );
}
