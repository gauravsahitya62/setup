"use client";

import * as React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useMyVendor } from "@/hooks/use-my-vendor";
import { ApiClientError, vendorApi, type VendorSocialLinksDto } from "@/services";
import { ROUTES } from "@/constants";

const FIELDS = [
  ["instagram", "Instagram"],
  ["facebook", "Facebook"],
  ["linkedin", "LinkedIn"],
  ["youtube", "YouTube"],
  ["pinterest", "Pinterest"],
  ["website", "Website"],
] as const;

export function VendorSocialLinksManagePage() {
  const router = useRouter();
  const { vendor, loading, missing, reload } = useMyVendor();
  const [social, setSocial] = React.useState<VendorSocialLinksDto>({});
  const [saving, setSaving] = React.useState(false);

  React.useEffect(() => {
    if (vendor?.socialLinks) setSocial(vendor.socialLinks);
  }, [vendor]);

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

  async function save() {
    setSaving(true);
    try {
      await vendorApi.upsertSocialLinks(vendor!.id, social);
      await reload();
      toast.success("Social links saved");
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Save failed");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">Social links</h1>
          <p className="text-muted-foreground text-sm">
            Help planners find you on the channels you already use.
          </p>
        </div>
        <Button variant="outline" className="rounded-xl" asChild>
          <Link href={ROUTES.vendor.profile}>Back to profile</Link>
        </Button>
      </div>

      <Card className="rounded-2xl shadow-sm">
        <CardHeader>
          <CardTitle className="text-base">Channels</CardTitle>
          <CardDescription>
            Instagram, Facebook, LinkedIn, YouTube, Pinterest, and website.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            {FIELDS.map(([key, label]) => (
              <div key={key} className="space-y-1.5">
                <Label>{label}</Label>
                <Input
                  value={social[key] ?? ""}
                  onChange={(e) => setSocial((s) => ({ ...s, [key]: e.target.value }))}
                  placeholder="https://"
                />
              </div>
            ))}
          </div>
          <Button className="rounded-xl" disabled={saving} onClick={() => void save()}>
            {saving ? <Loader2 className="size-4 animate-spin" /> : null}
            Save links
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
