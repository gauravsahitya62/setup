"use client";

import * as React from "react";
import { useRouter } from "next/navigation";

import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { ROUTES } from "@/constants";
import { notificationsInboxHref } from "@/lib/notifications/labels";
import { useAuth } from "@/providers";

export default function NotificationsRedirectPage() {
  const router = useRouter();
  const { role, isAuthenticated, isLoading } = useAuth();

  React.useEffect(() => {
    if (isLoading) return;
    if (!isAuthenticated) {
      router.replace(`${ROUTES.auth.signIn}?next=${ROUTES.notifications}`);
      return;
    }
    router.replace(notificationsInboxHref(role));
  }, [isAuthenticated, isLoading, role, router]);

  return <SkeletonLoader variant="list" count={4} />;
}
