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
import { ApiClientError, marketplaceApi, projectApi, mapProjectSummary } from "@/services";
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
  const [projects, setProjects] = React.useState<{ id: string; name: string }[]>([]);
  const [selectedId, setSelectedId] = React.useState(projectId ?? "");
  const [selectedVariant, setSelectedVariant] = React.useState(variantId ?? "");
  const [variants, setVariants] = React.useState<{ id: string; variantName: string }[]>([]);
  const [loading, setLoading] = React.useState(false);
  const [submitting, setSubmitting] = React.useState(false);

  React.useEffect(() => {
    if (!open || !listing) return;
    const current = listing;
    let cancelled = false;
    async function load() {
      setLoading(true);
      try {
        if (role === "planner" && isAuthenticated) {
          const page = await projectApi.list({ size: 50, sort: "updatedAt", direction: "desc" });
          if (!cancelled) {
            const mapped = page.content.map(mapProjectSummary);
            setProjects(mapped.map((project) => ({ id: project.id, name: project.name })));
            setSelectedId(projectId || mapped[0]?.id || "");
          }
        }
        if (!variantId) {
          const product = await marketplaceApi.products.getBySlug(current.slug);
          if (!cancelled) {
            const active = product.variants.filter((item) => item.status === "ACTIVE");
            setVariants(active.map((item) => ({ id: item.id, variantName: item.variantName })));
            setSelectedVariant(active[0]?.id ?? "");
          }
        } else {
          setSelectedVariant(variantId);
        }
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
  }, [open, listing, role, isAuthenticated, projectId, variantId]);

  if (!listing) return null;

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!isAuthenticated || role !== "planner") {
      toast.error("Sign in as a planner to add products to a project.");
      return;
    }
    if (!selectedId) {
      toast.error("Create a project first.");
      return;
    }
    if (!selectedVariant) {
      toast.error("Choose a product variant.");
      return;
    }
    setSubmitting(true);
    try {
      await projectApi.products.add(selectedId, { variantId: selectedVariant, quantity });
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
            disabled={submitting || loading}
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

        {loading ? (
          <p className="text-muted-foreground text-sm">Loading your projects…</p>
        ) : projects.length === 0 ? (
          <div className="border-border/80 space-y-3 rounded-xl border border-dashed p-4 text-sm">
            <p className="font-medium">Create your first project</p>
            <p className="text-muted-foreground">
              Products are shortlisted onto a project. Create one to start planning this event.
            </p>
            <Button className="rounded-xl" asChild>
              <Link href={ROUTES.planner.projectNew}>Create project</Link>
            </Button>
          </div>
        ) : (
          <>
            {!projectId ? (
              <div className="space-y-2">
                <Label htmlFor="project-select">Add to</Label>
                <select
                  id="project-select"
                  value={selectedId}
                  onChange={(event) => setSelectedId(event.target.value)}
                  className="border-input bg-background focus-visible:border-ring focus-visible:ring-ring/50 h-11 w-full rounded-xl border px-3 text-sm outline-none focus-visible:ring-3"
                >
                  {projects.map((project) => (
                    <option key={project.id} value={project.id}>
                      {project.name}
                    </option>
                  ))}
                </select>
                <p className="text-muted-foreground text-xs">
                  <Link
                    href={ROUTES.planner.projectNew}
                    className="underline-offset-4 hover:underline"
                  >
                    Create new project
                  </Link>
                </p>
              </div>
            ) : (
              <p className="text-muted-foreground text-sm">
                Adding to{" "}
                <span className="text-foreground font-medium">
                  {projects.find((item) => item.id === selectedId)?.name ?? "this project"}
                </span>
                .
              </p>
            )}

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
          </>
        )}
      </form>
    </Modal>
  );
}
