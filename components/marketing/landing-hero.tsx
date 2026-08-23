"use client";

import Image from "next/image";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { LANDING_HERO } from "@/constants/landing";
import { ROUTES } from "@/constants";
import { accountNavForRole } from "@/lib/auth/account-nav";
import { useAuth } from "@/providers";

export function LandingHero() {
  const { isAuthenticated, isLoading, role } = useAuth();
  const nav = accountNavForRole(role);
  const primaryHref = !isLoading && isAuthenticated ? nav.dashboardHref : ROUTES.auth.signUp;
  const primaryLabel = !isLoading && isAuthenticated ? nav.dashboardLabel : "Start Planning";
  return (
    <section className="relative isolate min-h-[88vh] overflow-hidden">
      <Image
        src={LANDING_HERO.image}
        alt={LANDING_HERO.imageAlt}
        fill
        priority
        sizes="100vw"
        className="object-cover"
      />
      <div className="absolute inset-0 bg-gradient-to-b from-black/60 via-black/50 to-black/75" />
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,transparent_0%,rgba(0,0,0,0.35)_100%)]" />

      <div className="relative mx-auto flex min-h-[88vh] max-w-7xl flex-col justify-center px-4 pt-24 pb-28 sm:px-6 sm:pb-32 lg:px-8">
        <div className="animate-in fade-in slide-in-from-bottom-4 fill-mode-both max-w-3xl duration-700">
          <p className="text-5xl font-semibold tracking-tight text-white sm:text-6xl md:text-7xl lg:text-8xl">
            {LANDING_HERO.brand}
          </p>
          <p className="mt-4 text-lg font-medium tracking-wide text-white/80 sm:text-xl md:text-2xl">
            {LANDING_HERO.tagline}
          </p>
        </div>

        <div className="animate-in fade-in slide-in-from-bottom-4 fill-mode-both mt-10 max-w-2xl duration-700 [animation-delay:120ms]">
          <h1 className="font-heading text-2xl font-semibold tracking-tight text-balance text-white sm:text-3xl md:text-4xl">
            {LANDING_HERO.headline}
          </h1>
          <p className="mt-4 max-w-xl text-base leading-relaxed text-pretty text-white/75 sm:text-lg">
            {LANDING_HERO.description}
          </p>

          <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:items-center">
            <Button
              asChild
              size="lg"
              className="h-12 rounded-xl bg-white px-6 text-base text-neutral-950 hover:bg-white/90"
            >
              <Link href={primaryHref}>{primaryLabel}</Link>
            </Button>
            <Button
              asChild
              size="lg"
              variant="outline"
              className="h-12 rounded-xl border-white/30 bg-white/5 px-6 text-base text-white backdrop-blur-sm hover:bg-white/10 hover:text-white"
            >
              <Link href={ROUTES.marketplace}>Browse Marketplace</Link>
            </Button>
          </div>
        </div>
      </div>
    </section>
  );
}
