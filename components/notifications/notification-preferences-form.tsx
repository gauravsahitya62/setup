"use client";

import * as React from "react";

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { notificationErrorMessage } from "@/lib/notifications/labels";
import { notificationApi, type NotificationPreferencesDto } from "@/services/notification-api";

const PREFERENCE_FIELDS: {
  key: keyof NotificationPreferencesDto;
  label: string;
  description: string;
}[] = [
  {
    key: "bookingUpdates",
    label: "Booking updates",
    description: "Requests sent, accepted, rejected, or cancelled.",
  },
  { key: "orderUpdates", label: "Order updates", description: "Confirmed and completed orders." },
  {
    key: "deliveryUpdates",
    label: "Delivery updates",
    description: "Scheduled delivery, out for delivery, and pickup.",
  },
  {
    key: "inventoryAlerts",
    label: "Inventory alerts",
    description: "Low stock and unavailable inventory.",
  },
  {
    key: "systemAnnouncements",
    label: "System announcements",
    description: "Verification, reports, and platform notices.",
  },
];

export function NotificationPreferencesForm() {
  const [prefs, setPrefs] = React.useState<NotificationPreferencesDto | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [saving, setSaving] = React.useState(false);

  React.useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const result = await notificationApi.getPreferences();
        if (!cancelled) setPrefs(result);
      } catch (err) {
        if (!cancelled) setError(notificationErrorMessage(err, "Could not load preferences"));
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  async function toggle(key: keyof NotificationPreferencesDto, enabled: boolean) {
    if (!prefs) return;
    const next = { ...prefs, [key]: enabled };
    setPrefs(next);
    setSaving(true);
    try {
      setPrefs(await notificationApi.updatePreferences(next));
      setError(null);
    } catch (err) {
      setError(notificationErrorMessage(err, "Could not save preferences"));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Card className="rounded-2xl shadow-sm">
      <CardHeader>
        <CardTitle className="text-base">Notification preferences</CardTitle>
        <CardDescription>
          In-app alerts only. Email, SMS, WhatsApp, and push can be added later.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {error ? <p className="text-destructive text-sm">{error}</p> : null}
        {prefs
          ? PREFERENCE_FIELDS.map((field) => (
              <div key={field.key} className="flex items-center justify-between gap-4">
                <div>
                  <Label htmlFor={field.key}>{field.label}</Label>
                  <p className="text-muted-foreground text-sm">{field.description}</p>
                </div>
                <Switch
                  id={field.key}
                  checked={prefs[field.key]}
                  disabled={saving}
                  onCheckedChange={(checked) => void toggle(field.key, checked)}
                />
              </div>
            ))
          : null}
      </CardContent>
    </Card>
  );
}
