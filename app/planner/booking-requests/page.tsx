import { PlannerBookingRequestList } from "@/components/planner/planner-booking-request-list";
import { APP_NAME } from "@/constants";

export const metadata = { title: `Booking requests · ${APP_NAME}` };

export default function BookingRequestsPage() {
  return <PlannerBookingRequestList />;
}
