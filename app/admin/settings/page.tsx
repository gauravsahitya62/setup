import { AdminSettingsPage } from "@/components/admin/admin-settings-page";
import { createPageMetadata } from "@/lib/metadata";

export const metadata = createPageMetadata({
  title: "Settings",
  description: "Platform configuration and notification preferences.",
  path: "/admin/settings",
});

export default function AdminSettingsRoute() {
  return <AdminSettingsPage />;
}
