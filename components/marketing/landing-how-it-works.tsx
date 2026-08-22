"use client";

import { FadeIn } from "@/components/marketing/fade-in";
import { SectionHeading } from "@/components/marketing/section-heading";
import { HOW_IT_WORKS } from "@/constants/landing";

export function LandingHowItWorks() {
  return (
    <section className="border-border/60 bg-muted/25 border-y py-20 sm:py-24">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <FadeIn>
          <SectionHeading
            align="center"
            eyebrow="How FlowSet works"
            title="From shortlist to booking request"
            description="A simple path designed around how planners already source rental inventory."
            className="mx-auto"
          />
        </FadeIn>

        <div className="mt-14 grid gap-10 md:grid-cols-3 md:gap-8">
          {HOW_IT_WORKS.map((item, index) => (
            <FadeIn key={item.step} delay={index * 0.1}>
              <div className="relative">
                <p className="text-muted-foreground text-sm font-medium tracking-[0.18em]">
                  {item.step}
                </p>
                <h3 className="mt-4 text-xl font-semibold tracking-tight">{item.title}</h3>
                <p className="text-muted-foreground mt-3 text-base leading-relaxed">
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
