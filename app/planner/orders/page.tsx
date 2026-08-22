import { PlannerOrderList } from "@/components/orders/planner-order-list";
import { APP_NAME } from "@/constants";

export const metadata = { title: `Orders · ${APP_NAME}` };

export default function OrdersPage() {
  return <PlannerOrderList />;
}
