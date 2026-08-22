import { VendorBookingRequestInbox } from "@/components/vendor/vendor-booking-request-inbox";
import { APP_NAME } from "@/constants";

export const metadata = { title: `Requests · ${APP_NAME}` };

export default function VendorRequestsPage() {
  return <VendorBookingRequestInbox />;
}
