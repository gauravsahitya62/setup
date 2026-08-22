import { PlannerReviewsPage } from "@/components/reviews/planner-reviews-page";
import { APP_NAME } from "@/constants";

export const metadata = { title: `Reviews · ${APP_NAME}` };

export default function PlannerReviewsRoute() {
  return <PlannerReviewsPage />;
}
