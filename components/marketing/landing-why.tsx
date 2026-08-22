"use client";

import { Check } from "lucide-react";

import { FadeIn } from "@/components/marketing/fade-in";
import { SectionHeading } from "@/components/marketing/section-heading";
import { WHY_FLOWSET } from "@/constants/landing";

export function LandingWhy() {
  return (
    <section className="mx-auto max-w-7xl px-4 py-20 sm:px-6 sm:py-24 lg:px-8">
      <div className="grid gap-12 lg:grid-cols-[0.9fr_1.1fr] lg:items-center lg:gap-16">
        <FadeIn>
          <SectionHeading
            eyebrow="Why FlowSet"
            title="The operating system for event rentals"
            description="FlowSet brings discovery, comparison, and booking requests into one calm workspace — so planners spend less time chasing vendors and more time designing events."
          />
        </FadeIn>

        <div className="grid gap-4 sm:grid-cols-2">
          {WHY_FLOWSET.map((item, index) => (
            <FadeIn key={item.title} delay={index * 0.07}>
              <div className="border-border/70 bg-background shadow-soft h-full rounded-2xl border p-6">
                <div className="bg-muted mb-4 flex size-9 items-center justify-center rounded-xl">
                  <Check className="text-foreground size-4" />
                </div>
                <h3 className="text-base font-semibold tracking-tight">{item.title}</h3>
                <p className="text-muted-foreground mt-2 text-sm leading-relaxed">
                  {item.description}
                </p>
              </div>
            </FadeIn>
          ))}
        </div>
      </div>
    </section>
  );
}
