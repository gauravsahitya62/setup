import { BookingRequestWizard } from "@/components/planner/booking-request-wizard";
import { APP_NAME } from "@/constants";

interface PageProps {
  params: Promise<{ id: string }>;
}

export async function generateMetadata() {
  return { title: `Request booking · ${APP_NAME}` };
}

export default async function ProjectBookingPage({ params }: PageProps) {
  const { id } = await params;
  return <BookingRequestWizard projectId={id} />;
}
