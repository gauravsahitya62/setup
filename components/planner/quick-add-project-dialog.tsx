"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Modal } from "@/components/shared/modal";
import { ROUTES } from "@/constants";
import { track } from "@/lib/analytics";
import { ApiClientError, projectApi } from "@/services";

interface QuickAddProjectDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated?: () => void;
}

export function QuickAddProjectDialog({
  open,
  onOpenChange,
  onCreated,
}: QuickAddProjectDialogProps) {
  const router = useRouter();
  const [name, setName] = React.useState("");
  const [city, setCity] = React.useState("");
  const [date, setDate] = React.useState("");
  const [notes, setNotes] = React.useState("");
  const [saving, setSaving] = React.useState(false);

  const reset = () => {
    setName("");
    setCity("");
    setDate("");
    setNotes("");
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!name.trim()) {
      toast.error("Give your project a name to continue.");
      return;
    }
    setSaving(true);
    try {
      const created = await projectApi.create({
        name: name.trim(),
        eventType: "OTHER",
        eventDate: date || undefined,
        city: city || undefined,
        description: notes || undefined,
        status: "DRAFT",
      });
      track("project_created", { status: "DRAFT" });
      toast.success(`“${created.name}” created`);
      reset();
      onOpenChange(false);
      onCreated?.();
      router.push(ROUTES.planner.project(created.id));
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not create project");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      title="Quick add project"
      description="Create a draft workspace so you can start shortlisting vendors right away."
      footer={
        <>
          <Button
            type="button"
            variant="outline"
            className="rounded-xl"
            onClick={() => onOpenChange(false)}
          >
            Cancel
          </Button>
          <Button type="submit" form="quick-add-project" className="rounded-xl" disabled={saving}>
            Create project
          </Button>
        </>
      }
    >
      <form
        id="quick-add-project"
        className="space-y-4"
        onSubmit={(event) => void handleSubmit(event)}
      >
        <div className="space-y-2">
          <Label htmlFor="project-name">Project name</Label>
          <Input
            id="project-name"
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder="e.g. Sharma Wedding"
            className="rounded-xl"
            autoFocus
          />
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="project-date">Event date</Label>
            <Input
              id="project-date"
              type="date"
              value={date}
              onChange={(event) => setDate(event.target.value)}
              className="rounded-xl"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="project-location">City</Label>
            <Input
              id="project-location"
              value={city}
              onChange={(event) => setCity(event.target.value)}
              placeholder="Jaipur"
              className="rounded-xl"
            />
          </div>
        </div>
        <div className="space-y-2">
          <Label htmlFor="project-notes">Notes</Label>
          <Textarea
            id="project-notes"
            value={notes}
            onChange={(event) => setNotes(event.target.value)}
            placeholder="Guest count, vibe, must-have rentals…"
            className="min-h-24 rounded-xl"
          />
        </div>
      </form>
    </Modal>
  );
}
