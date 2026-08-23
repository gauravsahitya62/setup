"use client";

import * as React from "react";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Modal } from "@/components/shared/modal";
import { ProjectPickerFields } from "@/components/planner/project-picker-fields";
import { usePlannerProjectOptions } from "@/hooks/use-planner-project-options";
import { track } from "@/lib/analytics";
import { useAuth } from "@/providers";
import { ApiClientError, marketplaceApi, projectApi } from "@/services";
import type { MarketplaceListing } from "@/types";
import { formatCurrency } from "@/lib/utils";

interface AddToProjectDialogProps {
  listing: MarketplaceListing | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  variantId?: string;
  quantity?: number;
  projectId?: string;
}

export function AddToProjectDialog({
  listing,
  open,
  onOpenChange,
  variantId,
  quantity = 1,
  projectId,
}: AddToProjectDialogProps) {
  const { isAuthenticated, role } = useAuth();
  const {
    projects,
    loading: projectsLoading,
    error: projectsError,
    canUse,
    isAuthenticated: signedIn,
    reload,
    addProject,
  } = usePlannerProjectOptions(open);
  const [selectedId, setSelectedId] = React.useState(projectId ?? "");
  const [selectedVariant, setSelectedVariant] = React.useState(variantId ?? "");
  const [variants, setVariants] = React.useState<{ id: string; variantName: string }[]>([]);
  const [variantsLoading, setVariantsLoading] = React.useState(false);
  const [submitting, setSubmitting] = React.useState(false);

  React.useEffect(() => {
    if (projectId) {
      setSelectedId(projectId);
      return;
    }
    if (selectedId && projects.some((project) => project.id === selectedId)) return;
    setSelectedId(projects[0]?.id ?? "");
  }, [projectId, projects, selectedId]);

  React.useEffect(() => {
    if (!open || !listing) return;
    if (variantId) {
      setSelectedVariant(variantId);
      setVariants([]);
      return;
    }

    const productId = listing.id;
    const slug = listing.slug;
    let cancelled = false;

    async function loadVariants() {
      setVariantsLoading(true);
      try {
        const product = slug
          ? await marketplaceApi.products
              .getBySlug(slug)
              .catch(() => marketplaceApi.products.getById(productId))
          : await marketplaceApi.products.getById(productId);
        if (cancelled) return;
        const active = (product.variants ?? []).filter((item) => item.status !== "INACTIVE");
        const options = active.length > 0 ? active : await marketplaceApi.variants.list(productId);
        if (cancelled) return;
        setVariants(options.map((item) => ({ id: item.id, variantName: item.variantName })));
        setSelectedVariant(options[0]?.id ?? "");
      } catch {
        if (!cancelled) {
          setVariants([]);
          setSelectedVariant("");
        }
      } finally {
        if (!cancelled) setVariantsLoading(false);
      }
    }

    void loadVariants();
    return () => {
      cancelled = true;
    };
  }, [open, listing, variantId]);

  if (!listing) return null;

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!isAuthenticated || role !== "planner") {
      toast.error("Sign in as a planner to add products to a project.");
      return;
    }
    if (!selectedId) {
      toast.error("Create or choose a project first.");
      return;
    }
    if (!selectedVariant) {
      toast.error("Choose a product variant.");
      return;
    }
    setSubmitting(true);
    try {
      await projectApi.products.add(selectedId, { variantId: selectedVariant, quantity });
      track("product_added_to_project", { quantity });
      const project = projects.find((item) => item.id === selectedId);
      toast.success(`Added to ${project?.name ?? "project"}`, {
        description: `${listing.name} · ${formatCurrency(listing.dailyRate, listing.currency)}/day — inventory is not reserved.`,
      });
      onOpenChange(false);
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not add product");
    } finally {
      setSubmitting(false);
    }
  };

  const loading = projectsLoading || variantsLoading;

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      title="Add to project"
      description={`Choose a project for “${listing.name}”. This does not send a booking request.`}
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
            form="add-to-project"
            className="rounded-xl"
            disabled={submitting || loading || !selectedId}
          >
            {submitting ? <Loader2 className="size-4 animate-spin" /> : null}
            Add to project
          </Button>
        </>
      }
    >
      <form id="add-to-project" className="space-y-4" onSubmit={handleSubmit}>
        <div className="border-border/70 bg-muted/30 rounded-xl border p-3 text-sm">
          <p className="font-medium">{listing.name}</p>
          <p className="text-muted-foreground mt-1">
            {listing.vendorName} · {formatCurrency(listing.dailyRate, listing.currency)}/day
          </p>
        </div>

        <ProjectPickerFields
          projects={projects}
          loading={projectsLoading}
          error={projectsError}
          canUse={canUse}
          isAuthenticated={signedIn}
          lockedProjectId={projectId}
          selectedId={selectedId}
          onSelectedIdChange={setSelectedId}
          onCreated={addProject}
          onRetry={() => void reload()}
        />

        {!variantId && variants.length > 1 ? (
          <div className="space-y-2">
            <Label htmlFor="variant-select">Variant</Label>
            <select
              id="variant-select"
              value={selectedVariant}
              onChange={(event) => setSelectedVariant(event.target.value)}
              className="border-input bg-background h-11 w-full rounded-xl border px-3 text-sm outline-none"
            >
              {variants.map((variant) => (
                <option key={variant.id} value={variant.id}>
                  {variant.variantName}
                </option>
              ))}
            </select>
          </div>
        ) : null}
      </form>
    </Modal>
  );
}
