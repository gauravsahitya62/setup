import type { Metadata } from "next";

import { env } from "@/lib/env";

const siteName = env.NEXT_PUBLIC_APP_NAME;
const siteUrl = env.NEXT_PUBLIC_APP_URL;

export const siteConfig = {
  name: siteName,
  tagline: "Powering Every Event.",
  description:
    "FlowSet helps event planners discover, compare, and request bookings from verified rental vendors.",
  url: siteUrl,
  ogImage: `${siteUrl}/og.png`,
  links: {
    twitter: "https://twitter.com/flowset",
    linkedin: "https://linkedin.com/company/flowset",
  },
} as const;

export const defaultMetadata: Metadata = {
  metadataBase: new URL(siteUrl),
  title: {
    default: `${siteName} · Powering Every Event`,
    template: `%s · ${siteName}`,
  },
  description: siteConfig.description,
  applicationName: siteName,
  keywords: [
    "event rentals",
    "event planners",
    "vendor marketplace",
    "B2B events",
    "rental booking",
    "FlowSet",
  ],
  authors: [{ name: "FlowSet" }],
  creator: "FlowSet",
  publisher: "FlowSet",
  formatDetection: {
    email: false,
    address: false,
    telephone: false,
  },
  openGraph: {
    type: "website",
    locale: "en_US",
    url: siteUrl,
    siteName,
    title: siteName,
    description: siteConfig.description,
    images: [
      {
        url: siteConfig.ogImage,
        width: 1200,
        height: 630,
        alt: siteName,
      },
    ],
  },
  twitter: {
    card: "summary_large_image",
    title: siteName,
    description: siteConfig.description,
    images: [siteConfig.ogImage],
  },
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
      "max-video-preview": -1,
      "max-image-preview": "large",
      "max-snippet": -1,
    },
  },
  icons: {
    icon: "/favicon.ico",
    shortcut: "/favicon.ico",
    apple: "/apple-touch-icon.png",
  },
  manifest: "/site.webmanifest",
};

export function createPageMetadata({
  title,
  description,
  path = "",
  image,
}: {
  title: string;
  description?: string;
  path?: string;
  image?: string;
}): Metadata {
  const url = `${siteUrl}${path}`;
  const pageDescription = description ?? siteConfig.description;
  const ogImage = image ?? siteConfig.ogImage;

  return {
    title,
    description: pageDescription,
    alternates: { canonical: url },
    openGraph: {
      title,
      description: pageDescription,
      url,
      images: [{ url: ogImage, width: 1200, height: 630, alt: title }],
    },
    twitter: {
      title,
      description: pageDescription,
      images: [ogImage],
    },
  };
}
