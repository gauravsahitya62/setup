"use client";

import * as React from "react";
import Link from "next/link";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ROUTES } from "@/constants";
import { track } from "@/lib/analytics";
import { ApiClientError, projectApi } from "@/services";
import type { PlannerProjectOption } from "@/hooks/use-planner-project-options";

interface ProjectPickerFieldsProps {
  projects: PlannerProjectOption[];
  loading: boolean;
  error: string | null;
  canUse: boolean;
  isAuthenticated: boolean;
  lockedProjectId?: string;
  selectedId: string;
  onSelectedIdChange: (id: string) => void;
  onCreated: (project: PlannerProjectOption) => void;
  onRetry: () => void;
  selectId?: string;
}

export function ProjectPickerFields({
  projects,
  loading,
  error,
  canUse,
  isAuthenticated,
  lockedProjectId,
  selectedId,
  onSelectedIdChange,
  onCreated,
  onRetry,
  selectId = "project-select",
}: ProjectPickerFieldsProps) {
  const [showCreate, setShowCreate] = React.useState(false);
  const [name, setName] = React.useState("");
  const [saving, setSaving] = React.useState(false);

  React.useEffect(() => {
    if (projects.length > 0) setShowCreate(false);
  }, [projects.length]);

  if (loading) {
    return <p className="text-muted-foreground text-sm">Loading your projects…</p>;
  }

  if (!isAuthenticated) {
    return (
      <div className="border-border/80 space-y-3 rounded-xl border border-dashed p-4 text-sm">
        <p className="font-medium">Sign in to add this to a project</p>
        <p className="text-muted-foreground">
          Planner accounts can shortlist products onto an event workspace.
        </p>
        <Button className="rounded-xl" asChild>
          <Link href={`${ROUTES.auth.signIn}?next=${ROUTES.marketplace}`}>Sign in</Link>
        </Button>
      </div>
    );
  }

  if (!canUse) {
    return (
      <p className="text-muted-foreground text-sm">
        Sign in as a planner to add products and vendors to a project.
      </p>
    );
  }

  if (error) {
    return (
      <div className="border-border/80 space-y-3 rounded-xl border border-dashed p-4 text-sm">
        <p className="font-medium">Could not load your projects</p>
        <p className="text-muted-foreground">{error}</p>
        <Button type="button" className="rounded-xl" onClick={onRetry}>
          Try again
        </Button>
      </div>
    );
  }

  if (lockedProjectId) {
    return (
      <p className="text-muted-foreground text-sm">
        Adding to{" "}
        <span className="text-foreground font-medium">
          {projects.find((item) => item.id === selectedId)?.name ?? "this project"}
        </span>
        .
      </p>
    );
  }

  async function handleCreate() {
    if (!name.trim()) {
      toast.error("Give your project a name to continue.");
      return;
    }
    setSaving(true);
    try {
      const created = await projectApi.create({
        name: name.trim(),
        eventType: "OTHER",
        status: "DRAFT",
      });
      track("project_created", { status: "DRAFT" });
      const option = { id: created.id, name: created.name };
      onCreated(option);
      onSelectedIdChange(created.id);
      setName("");
      setShowCreate(false);
      toast.success(`“${created.name}” created`);
    } catch (err) {
      toast.error(err instanceof ApiClientError ? err.message : "Could not create project");
    } finally {
      setSaving(false);
    }
  }

  if (projects.length === 0 || showCreate) {
    return (
      <div className="border-border/80 space-y-3 rounded-xl border border-dashed p-4 text-sm">
        <p className="font-medium">
          {projects.length === 0 ? "Create your first project" : "New project"}
        </p>
        <p className="text-muted-foreground">
          Products are shortlisted onto a project. Create one to start planning this event.
        </p>
        <div className="space-y-2">
          <Label htmlFor="inline-project-name">Project name</Label>
          <Input
            id="inline-project-name"
            value={name}
            onChange={(event) => setName(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter") {
                event.preventDefault();
                void handleCreate();
              }
            }}
            placeholder="e.g. Sharma Wedding"
            className="rounded-xl"
            autoFocus
          />
        </div>
        <div className="flex flex-wrap gap-2">
          <Button
            type="button"
            className="rounded-xl"
            disabled={saving}
            onClick={() => void handleCreate()}
          >
            {saving ? <Loader2 className="size-4 animate-spin" /> : null}
            Create project
          </Button>
          {projects.length > 0 ? (
            <Button
              type="button"
              variant="ghost"
              className="rounded-xl"
              onClick={() => setShowCreate(false)}
            >
              Cancel
            </Button>
          ) : null}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-2">
      <Label htmlFor={selectId}>Add to</Label>
      <select
        id={selectId}
        value={selectedId}
        onChange={(event) => onSelectedIdChange(event.target.value)}
        className="border-input bg-background focus-visible:border-ring focus-visible:ring-ring/50 h-11 w-full rounded-xl border px-3 text-sm outline-none focus-visible:ring-3"
      >
        {projects.map((project) => (
          <option key={project.id} value={project.id}>
            {project.name}
          </option>
        ))}
      </select>
      <p className="text-muted-foreground text-xs">
        <button
          type="button"
          className="underline-offset-4 hover:underline"
          onClick={() => setShowCreate(true)}
        >
          Create new project
        </button>
      </p>
    </div>
  );
}
