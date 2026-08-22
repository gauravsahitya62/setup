import { SupportTicketDetail } from "@/components/support/support-inbox";
import { ROUTES } from "@/constants";

interface PageProps {
  params: Promise<{ id: string }>;
}

export default async function VendorSupportTicketPage({ params }: PageProps) {
  const { id } = await params;
  return <SupportTicketDetail ticketId={id} backHref={ROUTES.vendor.support} />;
}
