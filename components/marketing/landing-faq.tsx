"use client";

import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import { FadeIn } from "@/components/marketing/fade-in";
import { SectionHeading } from "@/components/marketing/section-heading";
import { FAQS } from "@/constants/landing";

export function LandingFaq() {
  return (
    <section className="mx-auto max-w-3xl px-4 py-20 sm:px-6 sm:py-24 lg:px-8">
      <FadeIn>
        <SectionHeading
          align="center"
          eyebrow="FAQ"
          title="Questions, answered"
          description="Everything you need to know before you start planning on FlowSet."
          className="mx-auto"
        />
      </FadeIn>

      <FadeIn delay={0.1} className="mt-10">
        <Accordion type="single" collapsible className="w-full">
          {FAQS.map((faq) => (
            <AccordionItem key={faq.question} value={faq.question}>
              <AccordionTrigger className="text-left text-base font-medium tracking-tight">
                {faq.question}
              </AccordionTrigger>
              <AccordionContent className="text-muted-foreground text-base leading-relaxed">
                {faq.answer}
              </AccordionContent>
            </AccordionItem>
          ))}
        </Accordion>
      </FadeIn>
    </section>
  );
}
