"use client";

import * as React from "react";
import { Search, X } from "lucide-react";

import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

interface SearchBarProps {
  value?: string;
  defaultValue?: string;
  placeholder?: string;
  onChange?: (value: string) => void;
  onSubmit?: (value: string) => void;
  className?: string;
}

export function SearchBar({
  value,
  defaultValue = "",
  placeholder = "Search…",
  onChange,
  onSubmit,
  className,
}: SearchBarProps) {
  const [internal, setInternal] = React.useState(defaultValue);
  const current = value ?? internal;

  const handleChange = (next: string) => {
    if (value === undefined) setInternal(next);
    onChange?.(next);
  };

  return (
    <form
      className={cn("relative w-full", className)}
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit?.(current);
      }}
    >
      <Search className="text-muted-foreground pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2" />
      <Input
        value={current}
        onChange={(event) => handleChange(event.target.value)}
        placeholder={placeholder}
        className="h-10 rounded-xl pr-10 pl-9 shadow-sm"
        aria-label={placeholder}
      />
      {current ? (
        <Button
          type="button"
          variant="ghost"
          size="icon-xs"
          className="absolute top-1/2 right-2 -translate-y-1/2"
          onClick={() => handleChange("")}
          aria-label="Clear search"
        >
          <X className="size-3.5" />
        </Button>
      ) : null}
    </form>
  );
}
