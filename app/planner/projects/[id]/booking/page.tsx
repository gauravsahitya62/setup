import { BookingRequestWizard } from "@/components/planner/booking-request-wizard";
import { APP_NAME } from "@/constants";

interface PageProps {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ vendor?: string }>;
}

export async function generateMetadata() {
  return { title: `Request booking · ${APP_NAME}` };
}

export default async function ProjectBookingPage({ params, searchParams }: PageProps) {
  const { id } = await params;
  const { vendor } = await searchParams;
  return <BookingRequestWizard projectId={id} vendorId={vendor} />;
}
