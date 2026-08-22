"use client";

import Link from "next/link";

import { Button } from "@/components/ui/button";
import { ProjectCard } from "@/components/planner/project-card";
import type { Project } from "@/types";
import { ROUTES } from "@/constants";

interface ProjectCardsSectionProps {
  projects: Project[];
}

export function ProjectCardsSection({ projects }: ProjectCardsSectionProps) {
  return (
    <section>
      <div className="mb-5 flex items-end justify-between gap-4">
        <div>
          <h2 className="text-xl font-semibold tracking-tight">Your projects</h2>
          <p className="text-muted-foreground mt-1 text-sm">
            Active productions with rental sourcing underway.
          </p>
        </div>
        <Button variant="outline" size="sm" className="rounded-xl" asChild>
          <Link href={ROUTES.planner.projects}>View all</Link>
        </Button>
      </div>

      <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-4">
        {projects.map((project, index) => (
          <div
            key={project.id}
            className="animate-in fade-in slide-in-from-bottom-3 fill-mode-both duration-500"
            style={{ animationDelay: `${index * 60}ms` }}
          >
            <ProjectCard project={project} href={ROUTES.planner.project(project.id)} />
          </div>
        ))}
      </div>
    </section>
  );
}
