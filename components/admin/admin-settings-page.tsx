"use client";

import * as React from "react";
import { toast } from "sonner";

import { AdminPageHeader } from "@/components/admin/admin-page-header";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { NotificationPreferencesForm } from "@/components/notifications/notification-preferences-form";
import { EmptyState } from "@/components/shared/empty-state";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { adminErrorMessage } from "@/lib/admin/labels";
import { ApiClientError, adminApi, type PlatformSettingsDto } from "@/services";

export function AdminSettingsPage() {
  const [settings, setSettings] = React.useState<PlatformSettingsDto | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [saving, setSaving] = React.useState(false);

  React.useEffect(() => {
    void (async () => {
      try {
        setSettings(await adminApi.settings());
      } catch (err) {
        setError(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  async function save() {
    if (!settings) return;
    setSaving(true);
    try {
      setSettings(
        await adminApi.updateSettings({
          platformName: settings.platformName,
          supportEmail: settings.supportEmail,
          defaultInventoryThreshold: settings.defaultInventoryThreshold,
          reviewEditWindowDays: settings.reviewEditWindowDays,
          vendorVerificationRequired: settings.vendorVerificationRequired,
          maxImageSizeBytes: settings.maxImageSizeBytes,
          allowedFileTypes: settings.allowedFileTypes,
        })
      );
      toast.success("Settings saved");
    } catch (err) {
      toast.error(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="space-y-8">
      <AdminPageHeader
        title="Settings"
        description="Strongly typed platform configuration. Changes are audited."
      />

      {loading ? <SkeletonLoader className="h-48 rounded-2xl" /> : null}
      {error ? <EmptyState title="Could not load settings" description={error} /> : null}

      {settings ? (
        <div className="grid max-w-2xl gap-4">
          <Field label="Platform name">
            <Input
              value={settings.platformName}
              onChange={(e) => setSettings({ ...settings, platformName: e.target.value })}
              className="rounded-xl"
            />
          </Field>
          <Field label="Support email">
            <Input
              type="email"
              value={settings.supportEmail}
              onChange={(e) => setSettings({ ...settings, supportEmail: e.target.value })}
              className="rounded-xl"
            />
          </Field>
          <Field label="Default inventory threshold">
            <Input
              type="number"
              min={0}
              value={settings.defaultInventoryThreshold}
              onChange={(e) =>
                setSettings({ ...settings, defaultInventoryThreshold: Number(e.target.value) })
              }
              className="rounded-xl"
            />
          </Field>
          <Field label="Review edit window (days)">
            <Input
              type="number"
              min={1}
              value={settings.reviewEditWindowDays}
              onChange={(e) =>
                setSettings({ ...settings, reviewEditWindowDays: Number(e.target.value) })
              }
              className="rounded-xl"
            />
          </Field>
          <div className="flex items-center justify-between rounded-xl border px-3 py-3">
            <Label htmlFor="vendor-verification-required">Vendor verification required</Label>
            <Switch
              id="vendor-verification-required"
              checked={settings.vendorVerificationRequired}
              onCheckedChange={(checked) =>
                setSettings({ ...settings, vendorVerificationRequired: checked })
              }
            />
          </div>
          <Field label="Maximum image size (bytes)">
            <Input
              type="number"
              min={1}
              value={settings.maxImageSizeBytes}
              onChange={(e) =>
                setSettings({ ...settings, maxImageSizeBytes: Number(e.target.value) })
              }
              className="rounded-xl"
            />
          </Field>
          <Field label="Allowed file types">
            <Input
              value={settings.allowedFileTypes}
              onChange={(e) => setSettings({ ...settings, allowedFileTypes: e.target.value })}
              className="rounded-xl"
            />
          </Field>
          <Button className="w-fit rounded-xl" onClick={() => void save()} disabled={saving}>
            Save settings
          </Button>
        </div>
      ) : null}

      <NotificationPreferencesForm />
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="space-y-1.5">
      <Label>{label}</Label>
      {children}
    </div>
  );
}
