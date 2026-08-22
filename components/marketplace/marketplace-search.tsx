"use client";

import * as React from "react";
import { MapPin, Search, X } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

interface MarketplaceSearchProps {
  query: string;
  city: string;
  cities: readonly string[];
  onQueryChange: (value: string) => void;
  onCityChange: (value: string) => void;
  onSubmit?: () => void;
  className?: string;
}

export function MarketplaceSearch({
  query,
  city,
  cities,
  onQueryChange,
  onCityChange,
  onSubmit,
  className,
}: MarketplaceSearchProps) {
  return (
    <form
      className={cn(
        "border-border/70 bg-background shadow-elevated rounded-2xl border p-2 sm:p-3",
        className
      )}
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit?.();
      }}
    >
      <div className="grid gap-2 lg:grid-cols-[1.5fr_1fr_auto]">
        <label className="relative block">
          <span className="sr-only">Search rentals</span>
          <Search className="text-muted-foreground pointer-events-none absolute top-1/2 left-4 size-4 -translate-y-1/2" />
          <Input
            value={query}
            onChange={(event) => onQueryChange(event.target.value)}
            placeholder="Search chairs, lighting, tents…"
            className="bg-muted/40 h-14 rounded-xl border-transparent pr-10 pl-11 text-base shadow-none"
          />
          {query ? (
            <Button
              type="button"
              variant="ghost"
              size="icon-xs"
              className="absolute top-1/2 right-3 -translate-y-1/2"
              onClick={() => onQueryChange("")}
              aria-label="Clear search"
            >
              <X className="size-3.5" />
            </Button>
          ) : null}
        </label>

        <label className="relative block">
          <span className="sr-only">City</span>
          <MapPin className="text-muted-foreground pointer-events-none absolute top-1/2 left-4 z-10 size-4 -translate-y-1/2" />
          <select
            value={city}
            onChange={(event) => onCityChange(event.target.value)}
            className="bg-muted/40 text-foreground focus-visible:border-ring focus-visible:ring-ring/50 h-14 w-full appearance-none rounded-xl border border-transparent pr-4 pl-11 text-base transition-colors outline-none focus-visible:ring-3"
          >
            <option value="all">Any city</option>
            {cities.map((item) => (
              <option key={item} value={item}>
                {item}
              </option>
            ))}
          </select>
        </label>

        <Button type="submit" size="lg" className="h-14 rounded-xl px-8 text-base">
          Search
        </Button>
      </div>
    </form>
  );
}
