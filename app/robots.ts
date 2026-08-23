import type { MetadataRoute } from "next";

import { env } from "@/lib/env";

export default function robots(): MetadataRoute.Robots {
  const allowIndex = env.NEXT_PUBLIC_APP_ENV === "production";
  return {
    rules: allowIndex
      ? {
          userAgent: "*",
          allow: "/",
          disallow: ["/planner", "/vendor", "/admin", "/sign-in", "/sign-up", "/notifications"],
        }
      : { userAgent: "*", disallow: "/" },
    sitemap: `${env.NEXT_PUBLIC_APP_URL}/sitemap.xml`,
  };
}
