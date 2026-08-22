import { BookingRequestDetail } from "@/components/booking/booking-request-detail";
import { APP_NAME } from "@/constants";

interface PageProps {
  params: Promise<{ id: string }>;
}

export async function generateMetadata() {
  return { title: `Booking request · ${APP_NAME}` };
}

export default async function PlannerBookingRequestPage({ params }: PageProps) {
  const { id } = await params;
  return <BookingRequestDetail requestId={id} role="planner" />;
}
