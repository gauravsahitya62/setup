"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

import { ScrollArea, ScrollBar } from "@/components/ui/scroll-area";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Modal } from "@/components/shared/modal";
import { EmptyState } from "@/components/shared/empty-state";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { ProjectBookingsSection } from "@/components/planner/project-bookings-section";
import { ProjectOrdersSection } from "@/components/planner/project-orders-section";
import { ProjectFilesSection } from "@/components/planner/project-files-section";
import { ProjectMoodBoardSection } from "@/components/planner/project-moodboard-section";
import { ProjectNotesSection } from "@/components/planner/project-notes-section";
import { ProjectOverviewSection } from "@/components/planner/project-overview-section";
import { ProjectProductsSection } from "@/components/planner/project-products-section";
import { ProjectVendorsSection } from "@/components/planner/project-vendors-section";
import { ProjectActivitySection } from "@/components/planner/project-activity-section";
import { ProjectWorkspaceHeader } from "@/components/planner/project-workspace-header";
import { PROJECT_WORKSPACE_TABS, ROUTES, type ProjectWorkspaceTabId } from "@/constants";
import {
  EVENT_TYPE_LABELS,
  EVENT_TYPES,
  PROJECT_STATUS_LABELS,
  PROJECT_STATUSES,
} from "@/lib/projects/labels";
import { projectErrorMessage } from "@/lib/projects/activity";
import {
  ApiClientError,
  bookingApi,
  mapBookingSummary,
  mapProject,
  orderApi,
  projectApi,
  type OrderSummaryDto,
  type ProjectActivityDto,
  type ProjectNoteDto,
} from "@/services";
import type { BookingRequest, Project } from "@/types";
import { cn } from "@/lib/utils";

interface ProjectWorkspaceViewProps {
  projectId: string;
  defaultTab?: ProjectWorkspaceTabId;
}

export function ProjectWorkspaceView({
  projectId,
  defaultTab = "overview",
}: ProjectWorkspaceViewProps) {
  const router = useRouter();
  const [project, setProject] = React.useState<Project | null>(null);
  const [notes, setNotes] = React.useState<ProjectNoteDto[]>([]);
  const [activity, setActivity] = React.useState<ProjectActivityDto[]>([]);
  const [bookingRequests, setBookingRequests] = React.useState<BookingRequest[]>([]);
  const [orders, setOrders] = React.useState<OrderSummaryDto[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [editOpen, setEditOpen] = React.useState(false);
  const [tab, setTab] = React.useState<ProjectWorkspaceTabId>(defaultTab);

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const dto = await projectApi.get(projectId);
      setProject(mapProject(dto));
      setActivity(dto.recentActivity ?? []);
      setNotes(await projectApi.notes.list(projectId));
      try {
        const bookingPage = await bookingApi.list({ projectId, size: 50 });
        setBookingRequests(bookingPage.content.map(mapBookingSummary));
      } catch {
        setBookingRequests([]);
      }
      try {
        const orderPage = await orderApi.list({ projectId, size: 50 });
        setOrders(orderPage.content);
      } catch {
        setOrders([]);
      }
    } catch (err) {
      const message = projectErrorMessage(err instanceof ApiClientError ? err : undefined);
      setError(message);
      if (err instanceof ApiClientError && err.status === 404) {
        setProject(null);
      }
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  React.useEffect(() => {
    void load();
  }, [load]);

  if (loading) {
    return <SkeletonLoader variant="text" count={6} />;
  }

  if (error || !project) {
    return (
      <EmptyState
        title="Project not found"
        description={error ?? "This project is unavailable."}
        actionLabel="Back to projects"
        onAction={() => router.push(ROUTES.planner.projects)}
      />
    );
  }

  return (
    <div className="space-y-6 pb-20 lg:space-y-8 lg:pb-0">
      <div className="animate-in fade-in slide-in-from-bottom-2 duration-500">
        <ProjectWorkspaceHeader project={project} onEdit={() => setEditOpen(true)} />
      </div>

      <Tabs
        value={tab}
        onValueChange={(value) => setTab(value as ProjectWorkspaceTabId)}
        className="gap-6"
      >
        <div className="border-border/60 bg-background/90 sticky top-0 z-20 -mx-4 hidden border-b px-4 backdrop-blur-md sm:-mx-6 sm:px-6 md:block lg:-mx-8 lg:px-8">
          <ScrollArea className="w-full whitespace-nowrap">
            <TabsList
              variant="line"
              className="h-11 w-max min-w-full justify-start gap-0 rounded-none bg-transparent p-0"
            >
              {PROJECT_WORKSPACE_TABS.map((item) => (
                <TabsTrigger
                  key={item.id}
                  value={item.id}
                  className="h-11 flex-none rounded-none px-3.5 text-sm data-active:bg-transparent"
                >
                  {item.label}
                </TabsTrigger>
              ))}
            </TabsList>
            <ScrollBar orientation="horizontal" />
          </ScrollArea>
        </div>

        <TabsContent value="overview" className="mt-0 outline-none">
          <ProjectOverviewSection
            project={project}
            notes={notes}
            activity={activity}
            onAddProducts={() => router.push(ROUTES.planner.projectMarketplace(project.id))}
          />
        </TabsContent>
        <TabsContent value="products" className="mt-0 outline-none">
          <ProjectProductsSection
            projectId={project.id}
            eventDate={project.eventDate}
            marketplaceHref={ROUTES.planner.projectMarketplace(project.id)}
          />
        </TabsContent>
        <TabsContent value="vendors" className="mt-0 outline-none">
          <ProjectVendorsSection projectId={project.id} />
        </TabsContent>
        <TabsContent value="moodboard" className="mt-0 outline-none">
          <ProjectMoodBoardSection projectId={project.id} />
        </TabsContent>
        <TabsContent value="notes" className="mt-0 outline-none">
          <ProjectNotesSection projectId={project.id} />
        </TabsContent>
        <TabsContent value="files" className="mt-0 outline-none">
          <ProjectFilesSection projectId={project.id} />
        </TabsContent>
        <TabsContent value="activity" className="mt-0 outline-none">
          <ProjectActivitySection projectId={project.id} />
        </TabsContent>
        <TabsContent value="bookings" className="mt-0 outline-none">
          <ProjectBookingsSection projectId={project.id} requests={bookingRequests} />
        </TabsContent>
        <TabsContent value="orders" className="mt-0 outline-none">
          <ProjectOrdersSection projectId={project.id} orders={orders} />
        </TabsContent>
      </Tabs>

      <nav className="border-border/70 bg-background/95 fixed inset-x-0 bottom-0 z-30 border-t px-2 py-2 backdrop-blur md:hidden">
        <div className="flex justify-between gap-1 overflow-x-auto">
          {PROJECT_WORKSPACE_TABS.map((item) => (
            <button
              key={item.id}
              type="button"
              onClick={() => setTab(item.id)}
              className={cn(
                "min-w-0 flex-1 rounded-lg px-1 py-2 text-[10px] font-medium",
                tab === item.id ? "bg-muted text-foreground" : "text-muted-foreground"
              )}
            >
              {item.label.split(" ")[0]}
            </button>
          ))}
        </div>
      </nav>

      <EditProjectDialog
        project={project}
        open={editOpen}
        onOpenChange={setEditOpen}
        onSaved={async () => {
          await load();
        }}
      />
    </div>
  );
}

function EditProjectDialog({
  project,
  open,
  onOpenChange,
  onSaved,
}: {
  project: Project;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSaved: () => Promise<void>;
}) {
  const [name, setName] = React.useState(project.name);
  const [eventType, setEventType] = React.useState(project.eventType);
  const [eventDate, setEventDate] = React.useState(project.eventDate ?? "");
  const [guestCount, setGuestCount] = React.useState(project.guestCount?.toString() ?? "");
  const [venueName, setVenueName] = React.useState(project.venueName ?? "");
  const [city, setCity] = React.useState(project.city ?? "");
  const [state, setState] = React.useState(project.state ?? "");
  const [description, setDescription] = React.useState(project.description ?? "");
  const [status, setStatus] = React.useState(project.status);
  const [saving, setSaving] = React.useState(false);

  React.useEffect(() => {
    if (!open) return;
    setName(project.name);
    setEventType(project.eventType);
    setEventDate(project.eventDate ?? "");
    setGuestCount(project.guestCount?.toString() ?? "");
    setVenueName(project.venueName ?? "");
    setCity(project.city ?? "");
    setState(project.state ?? "");
    setDescription(project.description ?? "");
    setStatus(project.status);
  }, [open, project]);

  const save = async () => {
    setSaving(true);
    try {
      await projectApi.update(project.id, {
        name,
        eventType,
        eventDate: eventDate || undefined,
        guestCount: guestCount ? Number(guestCount) : undefined,
        venueName,
        city,
        state,
        description,
        status,
      });
      toast.success("Project updated");
      onOpenChange(false);
      await onSaved();
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not update project");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      title="Edit project"
      footer={
        <>
          <Button variant="outline" className="rounded-xl" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button className="rounded-xl" disabled={saving} onClick={() => void save()}>
            {saving ? <Loader2 className="size-4 animate-spin" /> : null}
            Save
          </Button>
        </>
      }
    >
      <div className="grid max-h-[60vh] gap-3 overflow-y-auto pr-1">
        <Field label="Name">
          <Input
            className="rounded-xl"
            value={name}
            onChange={(event) => setName(event.target.value)}
          />
        </Field>
        <Field label="Event type">
          <select
            className="h-10 w-full rounded-xl border px-3 text-sm"
            value={eventType}
            onChange={(event) => setEventType(event.target.value as Project["eventType"])}
          >
            {EVENT_TYPES.map((type) => (
              <option key={type} value={type}>
                {EVENT_TYPE_LABELS[type]}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Status">
          <select
            className="h-10 w-full rounded-xl border px-3 text-sm"
            value={status}
            onChange={(event) => setStatus(event.target.value as Project["status"])}
          >
            {PROJECT_STATUSES.map((item) => (
              <option key={item} value={item}>
                {PROJECT_STATUS_LABELS[item]}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Event date">
          <Input
            className="rounded-xl"
            type="date"
            value={eventDate}
            onChange={(event) => setEventDate(event.target.value)}
          />
        </Field>
        <Field label="Guest count">
          <Input
            className="rounded-xl"
            type="number"
            value={guestCount}
            onChange={(event) => setGuestCount(event.target.value)}
          />
        </Field>
        <Field label="Venue">
          <Input
            className="rounded-xl"
            value={venueName}
            onChange={(event) => setVenueName(event.target.value)}
          />
        </Field>
        <Field label="City">
          <Input
            className="rounded-xl"
            value={city}
            onChange={(event) => setCity(event.target.value)}
          />
        </Field>
        <Field label="State">
          <Input
            className="rounded-xl"
            value={state}
            onChange={(event) => setState(event.target.value)}
          />
        </Field>
        <Field label="Description">
          <Textarea
            className="rounded-xl"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
          />
        </Field>
      </div>
    </Modal>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="space-y-1.5">
      <Label>{label}</Label>
      {children}
    </div>
  );
}
