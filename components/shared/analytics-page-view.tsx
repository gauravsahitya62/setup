"use client";

import * as React from "react";

import { track, type AnalyticsEvent, type AnalyticsProperties } from "@/lib/analytics";

export function AnalyticsPageView({
  event,
  properties,
}: {
  event: AnalyticsEvent;
  properties?: AnalyticsProperties;
}) {
  const key = JSON.stringify(properties ?? {});
  React.useEffect(() => {
    track(event, properties);
    // Intentionally fire once per event+properties pair for this mount.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [event, key]);
  return null;
}
