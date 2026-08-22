"use client";

import * as React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Loader2, Trash2, Upload } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useMyVendor } from "@/hooks/use-my-vendor";
import { ApiClientError, vendorApi } from "@/services";
import { ROUTES } from "@/constants";

export function VendorPortfolioManagePage() {
  const router = useRouter();
  const { vendor, loading, missing, reload } = useMyVendor();
  const [title, setTitle] = React.useState("");
  const [saving, setSaving] = React.useState(false);

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
        actionLabel="Start wizard"
        onAction={() => router.push(ROUTES.vendor.profileWizard)}
      />
    );
  }

  async function upload(file: File | null) {
    if (!file) return;
    setSaving(true);
    try {
      await vendorApi.addPortfolio(vendor!.id, file, title.trim() || undefined);
      setTitle("");
      await reload();
      toast.success("Portfolio item added");
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Upload failed");
    } finally {
      setSaving(false);
    }
  }

  async function remove(id: string) {
    setSaving(true);
    try {
      await vendorApi.deletePortfolio(vendor!.id, id);
      await reload();
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Delete failed");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">Portfolio</h1>
          <p className="text-muted-foreground text-sm">
            Showcase past events so planners can evaluate your work.
          </p>
        </div>
        <Button variant="outline" className="rounded-xl" asChild>
          <Link href={ROUTES.vendor.profile}>Back to profile</Link>
        </Button>
      </div>

      <Card className="rounded-2xl shadow-sm">
        <CardHeader>
          <CardTitle className="text-base">Upload</CardTitle>
          <CardDescription>
            Images are stored outside PostgreSQL via StorageService.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-1.5">
            <Label>Title (optional)</Label>
            <Input value={title} onChange={(e) => setTitle(e.target.value)} />
          </div>
          <Label className="inline-flex cursor-pointer items-center gap-2 rounded-xl border border-dashed px-4 py-3 text-sm">
            <Upload className="size-4" />
            Choose image
            <input
              type="file"
              accept="image/*"
              className="hidden"
              disabled={saving}
              onChange={(e) => void upload(e.target.files?.[0] ?? null)}
            />
          </Label>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {vendor.portfolio.map((item) => (
              <div key={item.id} className="overflow-hidden rounded-xl border">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={item.imageUrl}
                  alt={item.title ?? "Portfolio"}
                  className="h-40 w-full object-cover"
                />
                <div className="flex items-center justify-between gap-2 p-2 text-sm">
                  <span className="truncate">{item.title || "Untitled"}</span>
                  <Button size="icon-sm" variant="ghost" onClick={() => void remove(item.id)}>
                    <Trash2 className="size-4" />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
