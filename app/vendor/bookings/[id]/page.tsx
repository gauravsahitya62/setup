import { OrderDetail } from "@/components/orders/order-detail";
import { APP_NAME } from "@/constants";

interface PageProps {
  params: Promise<{ id: string }>;
}

export async function generateMetadata() {
  return { title: `Order · ${APP_NAME}` };
}

export default async function VendorOrderPage({ params }: PageProps) {
  const { id } = await params;
  return <OrderDetail orderId={id} role="vendor" />;
}
