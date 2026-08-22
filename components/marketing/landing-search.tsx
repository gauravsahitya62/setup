"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { MapPin, Search } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { FadeIn } from "@/components/marketing/fade-in";
import { SectionHeading } from "@/components/marketing/section-heading";
import { ROUTES } from "@/constants";

export function LandingSearch() {
  const router = useRouter();
  const [query, setQuery] = React.useState("");
  const [location, setLocation] = React.useState("");

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    const params = new URLSearchParams();
    if (query.trim()) params.set("q", query.trim());
    if (location.trim()) params.set("location", location.trim());
    const qs = params.toString();
    router.push(qs ? `${ROUTES.marketplace}?${qs}` : ROUTES.marketplace);
  };

  return (
    <section className="relative z-10 -mt-8 px-4 sm:-mt-10 sm:px-6 lg:px-8">
      <FadeIn className="mx-auto max-w-4xl">
        <form
          onSubmit={handleSubmit}
          className="border-border/70 bg-background/95 shadow-elevated rounded-2xl border p-3 backdrop-blur-md sm:p-4"
        >
          <div className="mb-4 px-1 sm:mb-5">
            <SectionHeading
              title="Search rentals and vendors"
              description="Start with what you need — chairs, lighting, tents — and where the event is happening."
            />
          </div>

          <div className="grid gap-2 sm:grid-cols-[1.4fr_1fr_auto]">
            <label className="relative block">
              <span className="sr-only">What do you need?</span>
              <Search className="text-muted-foreground pointer-events-none absolute top-1/2 left-3.5 size-4 -translate-y-1/2" />
              <Input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="What do you need?"
                className="border-border/80 bg-muted/40 h-12 rounded-xl pl-10 text-base shadow-none"
              />
            </label>

            <label className="relative block">
              <span className="sr-only">Event location</span>
              <MapPin className="text-muted-foreground pointer-events-none absolute top-1/2 left-3.5 size-4 -translate-y-1/2" />
              <Input
                value={location}
                onChange={(event) => setLocation(event.target.value)}
                placeholder="City or venue area"
                className="border-border/80 bg-muted/40 h-12 rounded-xl pl-10 text-base shadow-none"
              />
            </label>

            <Button
              type="submit"
              size="lg"
              className="h-12 rounded-xl px-6 text-base sm:min-w-[132px]"
            >
              Search
            </Button>
          </div>
        </form>
      </FadeIn>
    </section>
  );
}
