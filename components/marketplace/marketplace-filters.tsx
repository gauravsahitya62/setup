"use client";

import * as React from "react";
import { BadgeCheck, SlidersHorizontal, X } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { Slider } from "@/components/ui/slider";
import {
  Sheet,
  SheetContent,
  SheetFooter,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { DEFAULT_MARKETPLACE_FILTERS, MARKETPLACE_PRICE_BOUNDS } from "@/constants/marketplace";
import type { MarketplaceFilters } from "@/types";
import { cn, formatCurrency } from "@/lib/utils";

interface MarketplaceFiltersProps {
  filters: MarketplaceFilters;
  onChange: (next: MarketplaceFilters) => void;
  onReset: () => void;
  className?: string;
}

const RATING_OPTIONS = [
  { value: 0, label: "Any" },
  { value: 4, label: "4.0+" },
  { value: 4.5, label: "4.5+" },
  { value: 4.8, label: "4.8+" },
] as const;

function FilterFields({
  draft,
  setDraft,
}: {
  draft: MarketplaceFilters;
  setDraft: React.Dispatch<React.SetStateAction<MarketplaceFilters>>;
}) {
  return (
    <div className="space-y-8">
      <div className="space-y-3">
        <Label className="text-sm font-semibold">Price per day</Label>
        <Slider
          min={MARKETPLACE_PRICE_BOUNDS.min}
          max={MARKETPLACE_PRICE_BOUNDS.max}
          step={5}
          value={[draft.priceMin, draft.priceMax]}
          onValueChange={(value) => {
            const [min, max] = value;
            setDraft((prev) => ({
              ...prev,
              priceMin: min ?? prev.priceMin,
              priceMax: max ?? prev.priceMax,
            }));
          }}
        />
        <div className="text-muted-foreground flex items-center justify-between text-sm">
          <span>{formatCurrency(draft.priceMin)}</span>
          <span>{formatCurrency(draft.priceMax)}</span>
        </div>
      </div>

      <div className="space-y-3">
        <Label className="text-sm font-semibold">Minimum rating</Label>
        <div className="flex flex-wrap gap-2">
          {RATING_OPTIONS.map((option) => {
            const active = draft.minRating === option.value;
            return (
              <button
                key={option.value}
                type="button"
                onClick={() => setDraft((prev) => ({ ...prev, minRating: option.value }))}
                className={cn(
                  "rounded-xl border px-3 py-1.5 text-sm font-medium transition-colors",
                  active
                    ? "border-foreground bg-foreground text-background"
                    : "border-border/80 text-muted-foreground hover:text-foreground"
                )}
              >
                {option.label}
              </button>
            );
          })}
        </div>
      </div>

      <div className="space-y-4">
        <label className="flex items-center gap-3">
          <Checkbox
            checked={draft.availableOnly}
            onCheckedChange={(checked) =>
              setDraft((prev) => ({ ...prev, availableOnly: checked === true }))
            }
          />
          <span className="text-sm">Available only</span>
        </label>

        <label className="flex items-center gap-3">
          <Checkbox
            checked={draft.verifiedOnly}
            onCheckedChange={(checked) =>
              setDraft((prev) => ({ ...prev, verifiedOnly: checked === true }))
            }
          />
          <span className="flex items-center gap-1.5 text-sm">
            Verified vendors
            <BadgeCheck className="size-3.5 text-sky-500" />
          </span>
        </label>
      </div>
    </div>
  );
}

export function MarketplaceFiltersBar({
  filters,
  onChange,
  onReset,
  className,
}: MarketplaceFiltersProps) {
  const [open, setOpen] = React.useState(false);
  const [draft, setDraft] = React.useState(filters);

  React.useEffect(() => {
    if (open) setDraft(filters);
  }, [open, filters]);

  const activeFilterCount = [
    filters.availableOnly,
    filters.verifiedOnly,
    filters.minRating > 0,
    filters.priceMin > DEFAULT_MARKETPLACE_FILTERS.priceMin ||
      filters.priceMax < DEFAULT_MARKETPLACE_FILTERS.priceMax,
  ].filter(Boolean).length;

  const applyDraft = () => {
    onChange(draft);
    setOpen(false);
  };

  return (
    <div className={cn("flex flex-wrap items-center gap-2", className)}>
      <Sheet open={open} onOpenChange={setOpen}>
        <SheetTrigger asChild>
          <Button variant="outline" className="rounded-xl">
            <SlidersHorizontal className="size-4" />
            Filters
            {activeFilterCount > 0 ? (
              <span className="bg-foreground text-background ml-1 flex size-5 items-center justify-center rounded-md text-[11px] font-semibold">
                {activeFilterCount}
              </span>
            ) : null}
          </Button>
        </SheetTrigger>
        <SheetContent side="right" className="w-full sm:max-w-md">
          <SheetHeader>
            <SheetTitle>Advanced filters</SheetTitle>
          </SheetHeader>
          <div className="flex-1 overflow-y-auto px-4 py-2">
            <FilterFields draft={draft} setDraft={setDraft} />
          </div>
          <SheetFooter className="gap-2 sm:flex-row">
            <Button
              variant="outline"
              className="rounded-xl"
              onClick={() => {
                setDraft(DEFAULT_MARKETPLACE_FILTERS);
                onReset();
                setOpen(false);
              }}
            >
              Reset
            </Button>
            <Button className="rounded-xl" onClick={applyDraft}>
              Apply filters
            </Button>
          </SheetFooter>
        </SheetContent>
      </Sheet>

      <button
        type="button"
        onClick={() => onChange({ ...filters, availableOnly: !filters.availableOnly })}
        className={cn(
          "rounded-xl border px-3 py-2 text-sm font-medium transition-colors",
          filters.availableOnly
            ? "border-foreground bg-foreground text-background"
            : "border-border/80 text-muted-foreground hover:text-foreground"
        )}
      >
        Available
      </button>

      <button
        type="button"
        onClick={() => onChange({ ...filters, verifiedOnly: !filters.verifiedOnly })}
        className={cn(
          "inline-flex items-center gap-1.5 rounded-xl border px-3 py-2 text-sm font-medium transition-colors",
          filters.verifiedOnly
            ? "border-foreground bg-foreground text-background"
            : "border-border/80 text-muted-foreground hover:text-foreground"
        )}
      >
        <BadgeCheck className="size-3.5" />
        Verified
      </button>

      {RATING_OPTIONS.filter((option) => option.value > 0).map((option) => (
        <button
          key={option.value}
          type="button"
          onClick={() =>
            onChange({
              ...filters,
              minRating: filters.minRating === option.value ? 0 : option.value,
            })
          }
          className={cn(
            "rounded-xl border px-3 py-2 text-sm font-medium transition-colors",
            filters.minRating === option.value
              ? "border-foreground bg-foreground text-background"
              : "border-border/80 text-muted-foreground hover:text-foreground"
          )}
        >
          {option.label}
        </button>
      ))}

      {activeFilterCount > 0 ? (
        <Button variant="ghost" size="sm" className="rounded-xl" onClick={onReset}>
          <X className="size-3.5" />
          Clear
        </Button>
      ) : null}
    </div>
  );
}
