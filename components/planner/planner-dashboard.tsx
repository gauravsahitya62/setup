"use client";

import * as React from "react";

import { ActivityTimeline } from "@/components/planner/activity-timeline";
import { ProjectCardsSection } from "@/components/planner/project-cards-section";
import { QuickAddProjectDialog } from "@/components/planner/quick-add-project-dialog";
import { RecentBookingRequests } from "@/components/planner/booking-request-card";
import { RecommendedVendors } from "@/components/planner/recommended-vendors";
import { UpcomingEvents } from "@/components/planner/upcoming-events";
import { WelcomeSection } from "@/components/planner/welcome-section";
import { EmptyState } from "@/components/shared/empty-state";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { PLANNER_ACTIVITY, PLANNER_RECOMMENDED_VENDORS } from "@/constants/planner";
import { ROUTES } from "@/constants";
import { formatEventDate, formatProjectLocation } from "@/lib/projects/labels";
import { bookingApi, mapBookingSummary, mapProjectSummary, projectApi } from "@/services";
import type { BookingRequest, Project, UpcomingEvent } from "@/types";

export function PlannerDashboard() {
  const [addOpen, setAddOpen] = React.useState(false);
  const [projects, setProjects] = React.useState<Project[]>([]);
  const [requests, setRequests] = React.useState<BookingRequest[]>([]);
  const [loading, setLoading] = React.useState(true);

  const reload = React.useCallback(async () => {
    setLoading(true);
    try {
      const [page, bookingPage] = await Promise.all([
        projectApi.list({ size: 8, sort: "updatedAt", direction: "desc" }),
        bookingApi.list({ size: 6 }),
      ]);
      setProjects(page.content.map(mapProjectSummary));
      setRequests(bookingPage.content.map(mapBookingSummary));
    } catch {
      setProjects([]);
      setRequests([]);
    } finally {
      setLoading(false);
    }
  }, []);

  React.useEffect(() => {
    void reload();
  }, [reload]);

  const upcoming: UpcomingEvent[] = projects
    .filter((project) => project.eventDate)
    .slice(0, 3)
    .map((project) => ({
      id: project.id,
      projectId: project.id,
      name: project.name,
      date: formatEventDate(project.eventDate) ?? project.eventDate ?? "",
      location: formatProjectLocation(project) ?? "Venue TBD",
      status: project.status,
      guestCount: project.guestCount,
    }));

  return (
    <div className="space-y-10 lg:space-y-12">
      <WelcomeSection onAddProject={() => setAddOpen(true)} projectCount={projects.length} />
      {loading ? (
        <SkeletonLoader
          variant="card"
          count={4}
          className="grid gap-5 sm:grid-cols-2 xl:grid-cols-4"
        />
      ) : projects.length === 0 ? (
        <EmptyState
          title="Create your first project"
          description="Projects are event workspaces — add products from the marketplace without reserving inventory."
          actionLabel="New project"
          onAction={() => {
            window.location.href = ROUTES.planner.projectNew;
          }}
        />
      ) : (
        <ProjectCardsSection projects={projects} />
      )}
      <RecentBookingRequests requests={requests} />

      <div className="grid gap-10 xl:grid-cols-[1.4fr_0.9fr] xl:gap-8">
        <UpcomingEvents events={upcoming} />
        <ActivityTimeline items={PLANNER_ACTIVITY} />
      </div>

      <RecommendedVendors vendors={PLANNER_RECOMMENDED_VENDORS} />

      <QuickAddProjectDialog
        open={addOpen}
        onOpenChange={setAddOpen}
        onCreated={() => void reload()}
      />
    </div>
  );
}
