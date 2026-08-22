"use client";

import * as React from "react";
import Link from "next/link";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Modal } from "@/components/shared/modal";
import { ROUTES } from "@/constants";
import { useAuth } from "@/providers";
import { ApiClientError, mapProjectSummary, projectApi } from "@/services";

interface SaveVendorDialogProps {
  vendorId: string;
  vendorName: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  projectId?: string;
}

export function SaveVendorDialog({
  vendorId,
  vendorName,
  open,
  onOpenChange,
  projectId,
}: SaveVendorDialogProps) {
  const { isAuthenticated, role } = useAuth();
  const [projects, setProjects] = React.useState<{ id: string; name: string }[]>([]);
  const [selectedId, setSelectedId] = React.useState(projectId ?? "");
  const [loading, setLoading] = React.useState(false);
  const [submitting, setSubmitting] = React.useState(false);

  React.useEffect(() => {
    if (!open || role !== "planner" || !isAuthenticated) return;
    let cancelled = false;
    async function load() {
      setLoading(true);
      try {
        const page = await projectApi.list({ size: 50, sort: "updatedAt", direction: "desc" });
        if (cancelled) return;
        const mapped = page.content.map(mapProjectSummary);
        setProjects(mapped.map((project) => ({ id: project.id, name: project.name })));
        setSelectedId(projectId || mapped[0]?.id || "");
      } catch {
        if (!cancelled) setProjects([]);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    void load();
    return () => {
      cancelled = true;
    };
  }, [open, role, isAuthenticated, projectId]);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!selectedId) {
      toast.error("Create a project first.");
      return;
    }
    setSubmitting(true);
    try {
      await projectApi.vendors.save(selectedId, { vendorId });
      const project = projects.find((item) => item.id === selectedId);
      toast.success(`Saved ${vendorName}`, { description: project?.name });
      onOpenChange(false);
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not save vendor");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      title="Save vendor to project"
      description={`Add ${vendorName} to a project shortlist. This does not create a booking.`}
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
          <Button
            type="submit"
            form="save-vendor"
            className="rounded-xl"
            disabled={submitting || loading}
          >
            {submitting ? <Loader2 className="size-4 animate-spin" /> : null}
            Save vendor
          </Button>
        </>
      }
    >
      <form id="save-vendor" className="space-y-4" onSubmit={handleSubmit}>
        {loading ? (
          <p className="text-muted-foreground text-sm">Loading projects…</p>
        ) : projects.length === 0 ? (
          <div className="space-y-3 text-sm">
            <p className="font-medium">Create your first project</p>
            <Button className="rounded-xl" asChild>
              <Link href={ROUTES.planner.projectNew}>Create project</Link>
            </Button>
          </div>
        ) : (
          <div className="space-y-2">
            <Label htmlFor="save-vendor-project">Project</Label>
            <select
              id="save-vendor-project"
              value={selectedId}
              onChange={(event) => setSelectedId(event.target.value)}
              className="border-input bg-background h-11 w-full rounded-xl border px-3 text-sm"
            >
              {projects.map((project) => (
                <option key={project.id} value={project.id}>
                  {project.name}
                </option>
              ))}
            </select>
          </div>
        )}
      </form>
    </Modal>
  );
}
