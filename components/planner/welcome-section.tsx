"use client";

import { ArrowUpRight, Plus } from "lucide-react";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { PLANNER_STATS } from "@/constants/planner";
import { ROUTES } from "@/constants";
import { useAuth } from "@/providers";

interface WelcomeSectionProps {
  onAddProject?: () => void;
  projectCount?: number;
}

export function WelcomeSection({ onAddProject, projectCount }: WelcomeSectionProps) {
  const { user } = useAuth();
  const hour = new Date().getHours();
  const greeting = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
  const firstName = user?.firstName ?? "there";
  const stats = [
    { label: "Projects", value: projectCount != null ? String(projectCount) : "—" },
    ...PLANNER_STATS.filter((stat) => stat.label !== "Active projects"),
  ].slice(0, 4);

  return (
    <section className="border-border/60 from-muted/50 via-background to-background shadow-soft animate-in fade-in slide-in-from-bottom-3 overflow-hidden rounded-3xl border bg-gradient-to-br p-6 duration-500 sm:p-8">
      <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
        <div className="max-w-2xl">
          <p className="text-muted-foreground text-sm font-medium">{greeting}</p>
          <h1 className="font-heading mt-2 text-3xl font-semibold tracking-tight text-balance sm:text-4xl">
            Welcome back, {firstName}
          </h1>
          <p className="text-muted-foreground mt-3 text-base leading-relaxed text-pretty">
            Create a project workspace, shortlist products from the marketplace, and save vendors —
            inventory is reserved only after booking.
          </p>
          <div className="mt-6 flex flex-wrap gap-3">
            <Button className="rounded-xl" asChild>
              <Link href={ROUTES.planner.projectNew}>
                <Plus className="size-4" />
                New project
              </Link>
            </Button>
            <Button variant="outline" className="rounded-xl" onClick={onAddProject}>
              Quick add
            </Button>
            <Button variant="outline" className="rounded-xl" asChild>
              <Link href={ROUTES.planner.marketplace}>
                Browse marketplace
                <ArrowUpRight className="size-4" />
              </Link>
            </Button>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:min-w-[420px]">
          {stats.map((stat, index) => (
            <div
              key={stat.label}
              className="border-border/60 bg-background/80 shadow-soft animate-in fade-in slide-in-from-bottom-2 fill-mode-both rounded-2xl border px-4 py-3 duration-500"
              style={{ animationDelay: `${80 + index * 50}ms` }}
            >
              <p className="text-2xl font-semibold tracking-tight tabular-nums">{stat.value}</p>
              <p className="text-muted-foreground mt-1 text-xs">{stat.label}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
