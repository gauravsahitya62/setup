import { SupportInbox } from "@/components/support/support-inbox";
import { ROUTES } from "@/constants";

export default function PlannerSupportPage() {
  return <SupportInbox title="Support" ticketHref={ROUTES.planner.ticket} />;
}
