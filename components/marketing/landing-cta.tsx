"use client";

import Link from "next/link";
import { motion } from "framer-motion";

import { Button } from "@/components/ui/button";
import { ROUTES } from "@/constants";

export function LandingCta() {
  return (
    <section className="mx-auto max-w-7xl px-4 pb-20 sm:px-6 sm:pb-24 lg:px-8">
      <motion.div
        initial={{ opacity: 0, y: 24 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-80px" }}
        transition={{ duration: 0.55, ease: [0.22, 1, 0.36, 1] }}
        className="overflow-hidden rounded-3xl bg-gradient-to-br from-neutral-950 via-neutral-900 to-neutral-800 px-6 py-14 text-center sm:px-10 sm:py-16"
      >
        <div className="mx-auto max-w-2xl">
          <h2 className="text-3xl font-semibold tracking-tight text-balance text-white sm:text-4xl">
            Ready to power your next event?
          </h2>
          <p className="mt-4 text-base leading-relaxed text-pretty text-white/70 sm:text-lg">
            Create a project, discover verified vendors, and request bookings in minutes.
          </p>
          <div className="mt-8 flex flex-col items-center justify-center gap-3 sm:flex-row">
            <Button
              asChild
              size="lg"
              className="h-12 rounded-xl bg-white px-6 text-base text-neutral-950 hover:bg-white/90"
            >
              <Link href={ROUTES.auth.signUp}>Start Planning</Link>
            </Button>
            <Button
              asChild
              size="lg"
              variant="outline"
              className="h-12 rounded-xl border-white/25 bg-transparent px-6 text-base text-white hover:bg-white/10 hover:text-white"
            >
              <Link href={ROUTES.marketplace}>Browse Marketplace</Link>
            </Button>
          </div>
        </div>
      </motion.div>
    </section>
  );
}
