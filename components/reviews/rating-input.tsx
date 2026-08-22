"use client";

import { Star } from "lucide-react";

import { cn } from "@/lib/utils";

interface RatingInputProps {
  value: number | null | undefined;
  onChange: (value: number) => void;
  disabled?: boolean;
  label?: string;
  required?: boolean;
}

export function RatingInput({ value, onChange, disabled, label, required }: RatingInputProps) {
  const current = value ?? 0;
  return (
    <fieldset className="space-y-1.5" disabled={disabled}>
      {label ? (
        <legend className="text-sm font-medium">
          {label}
          {required ? <span className="text-destructive"> *</span> : null}
        </legend>
      ) : null}
      <div className="flex items-center gap-1" role="radiogroup" aria-label={label ?? "Rating"}>
        {[1, 2, 3, 4, 5].map((stars) => {
          const filled = stars <= current;
          return (
            <button
              key={stars}
              type="button"
              role="radio"
              aria-checked={current === stars}
              aria-label={`${stars} star${stars === 1 ? "" : "s"}`}
              className="text-muted-foreground focus-visible:ring-ring rounded-md p-0.5 transition-colors hover:text-amber-400 focus-visible:ring-2 focus-visible:outline-none"
              onClick={() => onChange(stars)}
            >
              <Star
                className={cn(
                  "size-6",
                  filled ? "fill-amber-400 text-amber-400" : "fill-muted text-muted"
                )}
              />
            </button>
          );
        })}
      </div>
    </fieldset>
  );
}
