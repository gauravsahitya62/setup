"use client";

import * as React from "react";
import Link from "next/link";
import { FolderKanban, Plus } from "lucide-react";

import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/shared/empty-state";
import { SearchBar } from "@/components/shared/search-bar";
import { AppPagination } from "@/components/shared/pagination";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { ProjectCard } from "@/components/planner/project-card";
import { ROUTES } from "@/constants";
import {
  EVENT_TYPE_LABELS,
  EVENT_TYPES,
  PROJECT_STATUS_LABELS,
  PROJECT_STATUSES,
} from "@/lib/projects/labels";
import { projectErrorMessage } from "@/lib/projects/activity";
import { ApiClientError, mapProjectSummary, projectApi } from "@/services";
import type { EventType, Project, ProjectStatus } from "@/types";

export function PlannerProjectsList() {
  const [query, setQuery] = React.useState("");
  const [status, setStatus] = React.useState<ProjectStatus | "">("");
  const [eventType, setEventType] = React.useState<EventType | "">("");
  const [page, setPage] = React.useState(0);
  const [projects, setProjects] = React.useState<Project[]>([]);
  const [totalPages, setTotalPages] = React.useState(0);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await projectApi.list({
        q: query,
        status,
        eventType,
        sort: "eventDate",
        direction: "asc",
        page,
        size: 12,
      });
      setProjects(result.content.map(mapProjectSummary));
      setTotalPages(result.totalPages);
    } catch (err) {
      setError(
        projectErrorMessage(
          err instanceof ApiClientError ? err : undefined,
          "Could not load projects"
        )
      );
      setProjects([]);
    } finally {
      setLoading(false);
    }
  }, [query, status, eventType, page]);

  React.useEffect(() => {
    void load();
  }, [load]);

  return (
    <div className="space-y-6 lg:space-y-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Projects</h1>
          <p className="text-muted-foreground mt-1 text-sm">
            Event workspaces for products, vendors, mood boards, and notes.
          </p>
        </div>
        <Button className="rounded-xl" asChild>
          <Link href={ROUTES.planner.projectNew}>
            <Plus className="size-3.5" />
            New project
          </Link>
        </Button>
      </div>

      <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
        <SearchBar
          value={query}
          placeholder="Search by name"
          onChange={(value) => {
            setPage(0);
            setQuery(value);
          }}
          className="lg:max-w-sm"
        />
        <select
          value={status}
          onChange={(event) => {
            setPage(0);
            setStatus(event.target.value as ProjectStatus | "");
          }}
          className="border-input bg-background h-10 rounded-xl border px-3 text-sm"
        >
          <option value="">All statuses</option>
          {PROJECT_STATUSES.map((item) => (
            <option key={item} value={item}>
              {PROJECT_STATUS_LABELS[item]}
            </option>
          ))}
        </select>
        <select
          value={eventType}
          onChange={(event) => {
            setPage(0);
            setEventType(event.target.value as EventType | "");
          }}
          className="border-input bg-background h-10 rounded-xl border px-3 text-sm"
        >
          <option value="">All event types</option>
          {EVENT_TYPES.map((item) => (
            <option key={item} value={item}>
              {EVENT_TYPE_LABELS[item]}
            </option>
          ))}
        </select>
      </div>

      {loading ? (
        <SkeletonLoader
          variant="card"
          count={6}
          className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3"
        />
      ) : error ? (
        <EmptyState
          title="Could not load projects"
          description={error}
          actionLabel="Retry"
          onAction={() => void load()}
        />
      ) : projects.length === 0 ? (
        <EmptyState
          title="No projects yet"
          description="Create a project to start shortlisting rentals and vendors."
          icon={<FolderKanban className="size-5" />}
          actionLabel="Create project"
          onAction={() => {
            window.location.href = ROUTES.planner.projectNew;
          }}
        />
      ) : (
        <>
          <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
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
          <AppPagination
            page={page + 1}
            totalPages={totalPages}
            onPageChange={(next) => setPage(next - 1)}
          />
        </>
      )}
    </div>
  );
}
