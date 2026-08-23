"use client";

import * as React from "react";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Modal } from "@/components/shared/modal";
import { ProjectPickerFields } from "@/components/planner/project-picker-fields";
import { usePlannerProjectOptions } from "@/hooks/use-planner-project-options";
import { ApiClientError, projectApi } from "@/services";

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
  const { projects, loading, error, canUse, isAuthenticated, reload, addProject } =
    usePlannerProjectOptions(open);
  const [selectedId, setSelectedId] = React.useState(projectId ?? "");
  const [submitting, setSubmitting] = React.useState(false);

  React.useEffect(() => {
    if (projectId) {
      setSelectedId(projectId);
      return;
    }
    if (selectedId && projects.some((project) => project.id === selectedId)) return;
    setSelectedId(projects[0]?.id ?? "");
  }, [projectId, projects, selectedId]);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!selectedId) {
      toast.error("Create or choose a project first.");
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
            disabled={submitting || loading || !selectedId}
          >
            {submitting ? <Loader2 className="size-4 animate-spin" /> : null}
            Save vendor
          </Button>
        </>
      }
    >
      <form id="save-vendor" className="space-y-4" onSubmit={(event) => void handleSubmit(event)}>
        <ProjectPickerFields
          projects={projects}
          loading={loading}
          error={error}
          canUse={canUse}
          isAuthenticated={isAuthenticated}
          lockedProjectId={projectId}
          selectedId={selectedId}
          onSelectedIdChange={setSelectedId}
          onCreated={addProject}
          onRetry={() => void reload()}
          selectId="save-vendor-project"
        />
      </form>
    </Modal>
  );
}
