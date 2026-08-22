"use client";

import * as React from "react";
import Link from "next/link";
import { BadgeCheck, Trash2 } from "lucide-react";
import { toast } from "sonner";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";
import { Rating } from "@/components/shared/rating";
import { ROUTES } from "@/constants";
import { ApiClientError, projectApi, type ProjectVendorDto } from "@/services";

interface ProjectVendorsSectionProps {
  projectId: string;
}

export function ProjectVendorsSection({ projectId }: ProjectVendorsSectionProps) {
  const [vendors, setVendors] = React.useState<ProjectVendorDto[]>([]);
  const [loading, setLoading] = React.useState(true);

  const reload = React.useCallback(async () => {
    setLoading(true);
    try {
      setVendors(await projectApi.vendors.list(projectId));
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not load vendors");
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  React.useEffect(() => {
    void reload();
  }, [reload]);

  const remove = async (vendor: ProjectVendorDto) => {
    try {
      await projectApi.vendors.remove(projectId, vendor.vendorId);
      setVendors((prev) => prev.filter((item) => item.id !== vendor.id));
      toast.success(`Removed ${vendor.companyName}`);
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not remove vendor");
    }
  };

  if (loading) return <p className="text-muted-foreground text-sm">Loading vendors…</p>;

  if (vendors.length === 0) {
    return (
      <EmptyState
        title="No saved vendors"
        description="Save vendors from their profile. This does not create or cancel bookings."
        actionLabel="Browse vendors"
        onAction={() => {
          window.location.href = ROUTES.vendors;
        }}
      />
    );
  }

  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-lg font-semibold tracking-tight">Vendors</h2>
        <p className="text-muted-foreground mt-1 text-sm">Saved partners for this event.</p>
      </div>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {vendors.map((vendor) => (
          <Card key={vendor.id} className="border-border/70 shadow-soft">
            <CardContent className="flex gap-3 p-4">
              <div className="bg-muted size-14 overflow-hidden rounded-xl">
                {vendor.logoUrl ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img src={vendor.logoUrl} alt="" className="size-full object-cover" />
                ) : null}
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex items-start justify-between gap-2">
                  <p className="font-medium">{vendor.companyName}</p>
                  {vendor.verified ? (
                    <Badge className="rounded-md border-0 bg-sky-500/15 text-sky-700">
                      <BadgeCheck className="size-3" />
                      Verified
                    </Badge>
                  ) : null}
                </div>
                <Rating value={Number(vendor.rating)} reviewCount={vendor.reviewCount} size="sm" />
                <p className="text-muted-foreground mt-1 text-xs">
                  {vendor.city ?? "—"} · {vendor.productsInProject} products in project
                </p>
                {vendor.unavailable ? (
                  <p className="text-destructive mt-1 text-xs">Vendor unavailable</p>
                ) : null}
                <div className="mt-3 flex gap-2">
                  <Button size="sm" variant="outline" className="rounded-lg" asChild>
                    <Link href={`/vendors/${vendor.slug}`}>View profile</Link>
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    className="text-destructive rounded-lg"
                    onClick={() => void remove(vendor)}
                  >
                    <Trash2 className="size-3.5" />
                    Remove
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
