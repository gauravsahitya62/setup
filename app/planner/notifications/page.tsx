import { NotificationInbox } from "@/components/notifications/notification-inbox";
import { APP_NAME } from "@/constants";

export const metadata = { title: `Notifications · ${APP_NAME}` };

export default function PlannerNotificationsPage() {
  return <NotificationInbox />;
}
