"use client";

import * as React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Loader2, Plus, Trash2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useMyVendor } from "@/hooks/use-my-vendor";
import { ApiClientError, vendorApi } from "@/services";
import { ROUTES } from "@/constants";

export function VendorServiceAreasPage() {
  const router = useRouter();
  const { vendor, loading, missing, reload } = useMyVendor();
  const [saving, setSaving] = React.useState(false);
  const [draft, setDraft] = React.useState({ country: "India", state: "", city: "" });

  if (loading) {
    return (
      <div className="text-muted-foreground flex justify-center py-24">
        <Loader2 className="size-5 animate-spin" />
      </div>
    );
  }

  if (missing || !vendor) {
    return (
      <EmptyState
        title="Create a company profile first"
        description="Service areas belong to your vendor storefront."
        actionLabel="Start wizard"
        onAction={() => router.push(ROUTES.vendor.profileWizard)}
      />
    );
  }

  async function add() {
    if (!draft.state.trim() || !draft.city.trim()) {
      toast.error("State and city are required");
      return;
    }
    setSaving(true);
    try {
      await vendorApi.addServiceArea(vendor!.id, {
        country: draft.country.trim() || "India",
        state: draft.state.trim(),
        city: draft.city.trim(),
      });
      setDraft((d) => ({ ...d, state: "", city: "" }));
      await reload();
      toast.success("Service area added");
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to add area");
    } finally {
      setSaving(false);
    }
  }

  async function remove(id: string) {
    setSaving(true);
    try {
      await vendorApi.deleteServiceArea(vendor!.id, id);
      await reload();
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Failed to remove area");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="space-y-6">
      <Header
        title="Service areas"
        description="Cities and regions where planners can book your inventory."
      />
      <Card className="rounded-2xl shadow-sm">
        <CardHeader>
          <CardTitle className="text-base">Add location</CardTitle>
          <CardDescription>Each vendor can serve multiple markets.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-3">
            <div className="space-y-1.5">
              <Label>Country</Label>
              <Input
                value={draft.country}
                onChange={(e) => setDraft((d) => ({ ...d, country: e.target.value }))}
              />
            </div>
            <div className="space-y-1.5">
              <Label>State</Label>
              <Input
                value={draft.state}
                onChange={(e) => setDraft((d) => ({ ...d, state: e.target.value }))}
              />
            </div>
            <div className="space-y-1.5">
              <Label>City</Label>
              <Input
                value={draft.city}
                onChange={(e) => setDraft((d) => ({ ...d, city: e.target.value }))}
              />
            </div>
          </div>
          <Button className="rounded-xl" disabled={saving} onClick={() => void add()}>
            <Plus className="size-4" />
            Add area
          </Button>
          <ul className="space-y-2">
            {vendor.serviceAreas.map((area) => (
              <li
                key={area.id}
                className="flex items-center justify-between rounded-xl border px-3 py-2 text-sm"
              >
                <span>
                  {area.city}, {area.state} · {area.country}
                </span>
                <Button size="icon-sm" variant="ghost" onClick={() => void remove(area.id)}>
                  <Trash2 className="size-4" />
                </Button>
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>
    </div>
  );
}

function Header({ title, description }: { title: string; description: string }) {
  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
      <div className="space-y-1">
        <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">{title}</h1>
        <p className="text-muted-foreground text-sm">{description}</p>
      </div>
      <Button variant="outline" className="rounded-xl" asChild>
        <Link href={ROUTES.vendor.profile}>Back to profile</Link>
      </Button>
    </div>
  );
}
