"use client";

import { FadeIn } from "@/components/marketing/fade-in";
import { TESTIMONIALS } from "@/constants/landing";

export function LandingTestimonials() {
  return (
    <section className="bg-foreground text-background py-20 sm:py-24">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <FadeIn>
          <div className="mx-auto max-w-2xl text-center">
            <p className="text-background/55 mb-3 text-sm font-medium tracking-wide">
              Testimonials
            </p>
            <h2 className="font-heading text-background text-3xl font-semibold tracking-tight text-balance sm:text-4xl">
              Planners who switched to FlowSet
            </h2>
            <p className="text-background/65 mt-4 text-base leading-relaxed text-pretty sm:text-lg">
              Teams use FlowSet to source faster and keep vendor conversations in one place.
            </p>
          </div>
        </FadeIn>

        <div className="mt-14 grid gap-6 md:grid-cols-3">
          {TESTIMONIALS.map((item, index) => (
            <FadeIn key={item.name} delay={index * 0.08}>
              <figure className="border-background/10 bg-background/5 flex h-full flex-col rounded-2xl border p-6 backdrop-blur-sm">
                <blockquote className="text-background/85 flex-1 text-base leading-relaxed">
                  “{item.quote}”
                </blockquote>
                <figcaption className="mt-8">
                  <p className="text-background text-sm font-semibold tracking-tight">
                    {item.name}
                  </p>
                  <p className="text-background/55 mt-1 text-sm">{item.role}</p>
                </figcaption>
              </figure>
            </FadeIn>
          ))}
        </div>
      </div>
    </section>
  );
}
