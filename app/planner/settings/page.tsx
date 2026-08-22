import Link from "next/link";

import { Button } from "@/components/ui/button";
import { NotificationPreferencesForm } from "@/components/notifications/notification-preferences-form";
import { ROUTES } from "@/constants";

export default function SettingsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Settings</h1>
        <p className="text-muted-foreground mt-1 text-sm">
          Account preferences for your planner workspace.
        </p>
      </div>
      <NotificationPreferencesForm />
      <Button variant="outline" className="rounded-xl" asChild>
        <Link href={ROUTES.planner.root}>Back to dashboard</Link>
      </Button>
    </div>
  );
}
