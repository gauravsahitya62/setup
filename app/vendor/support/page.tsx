import { SupportInbox } from "@/components/support/support-inbox";
import { ROUTES } from "@/constants";

export default function VendorSupportPage() {
  return <SupportInbox title="Support" ticketBasePath={ROUTES.vendor.support} />;
}
