import { VendorDeliveryCalendar } from "@/components/orders/vendor-delivery-calendar";
import { VendorOrderList } from "@/components/orders/vendor-order-list";
import { APP_NAME } from "@/constants";

export const metadata = { title: `Orders · ${APP_NAME}` };

export default function VendorBookingsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Orders</h1>
        <p className="text-muted-foreground mt-1 text-sm">
          Fulfill confirmed rentals: schedule delivery, mark status, and pick up after the event.
        </p>
      </div>
      <VendorDeliveryCalendar />
      <VendorOrderList />
    </div>
  );
}
