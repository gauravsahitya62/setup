import { VendorReviewsDashboard } from "@/components/reviews/vendor-reviews-dashboard";
import { APP_NAME } from "@/constants";

export const metadata = { title: `Reviews · ${APP_NAME}` };

export default function VendorReviewsPage() {
  return <VendorReviewsDashboard />;
}
