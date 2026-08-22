import { redirect } from "next/navigation";

import { ROUTES } from "@/constants";

export default function AdminAnalyticsRedirect() {
  redirect(ROUTES.admin.root);
}
