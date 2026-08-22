import type { Category, Product, Vendor } from "@/types";

const img = (id: string, w = 1200) =>
  `https://images.unsplash.com/${id}?auto=format&fit=crop&w=${w}&q=80`;

export const LANDING_HERO = {
  brand: "FlowSet",
  tagline: "Powering Every Event.",
  headline: "Find Everything Your Event Needs",
  description:
    "Discover verified rental vendors, compare catalogs, and request bookings — all in one place built for professional planners.",
  image: img("photo-1511578314322-379afb476865", 2400),
  imageAlt: "Guests gathered at an elegant evening event reception",
} as const;

export const LANDING_CATEGORIES: Category[] = [
  {
    id: "cat-seating",
    name: "Seating",
    slug: "seating",
    description: "Chairs, lounges, and benches for every guest count.",
    imageUrl: img("photo-1506439773649-6e0eb8cfb237", 900),
    productCount: 1240,
    createdAt: "2026-01-01T00:00:00.000Z",
    updatedAt: "2026-01-01T00:00:00.000Z",
  },
  {
    id: "cat-tables",
    name: "Tables",
    slug: "tables",
    description: "Rounds, farm tables, and cocktail high-tops.",
    imageUrl: img("photo-1414235077428-338989a2e8c0", 900),
    productCount: 860,
    createdAt: "2026-01-01T00:00:00.000Z",
    updatedAt: "2026-01-01T00:00:00.000Z",
  },
  {
    id: "cat-lighting",
    name: "Lighting",
    slug: "lighting",
    description: "String lights, uplights, and statement fixtures.",
    imageUrl: img("photo-1478146896981-b80fe463b330", 900),
    productCount: 640,
    createdAt: "2026-01-01T00:00:00.000Z",
    updatedAt: "2026-01-01T00:00:00.000Z",
  },
  {
    id: "cat-linens",
    name: "Linens",
    slug: "linens",
    description: "Tablecloths, napkins, and textured overlays.",
    imageUrl: img("photo-1519225421980-715cb0215aed", 900),
    productCount: 520,
    createdAt: "2026-01-01T00:00:00.000Z",
    updatedAt: "2026-01-01T00:00:00.000Z",
  },
  {
    id: "cat-av",
    name: "AV & Sound",
    slug: "av-sound",
    description: "Speakers, screens, and production essentials.",
    imageUrl: img("photo-1492684223066-81342ee5ff30", 900),
    productCount: 410,
    createdAt: "2026-01-01T00:00:00.000Z",
    updatedAt: "2026-01-01T00:00:00.000Z",
  },
  {
    id: "cat-structures",
    name: "Structures",
    slug: "structures",
    description: "Tents, stages, and outdoor frameworks.",
    imageUrl: img("photo-1530103862676-de8c9debad1d", 900),
    productCount: 290,
    createdAt: "2026-01-01T00:00:00.000Z",
    updatedAt: "2026-01-01T00:00:00.000Z",
  },
];

export const LANDING_VENDORS: Vendor[] = [
  {
    id: "vnd-1",
    name: "Atlas Event Rentals",
    slug: "atlas-event-rentals",
    description: "Full-service seating and tableware for weddings and corporate galas.",
    logoUrl: img("photo-1560179707-f14e90ef3623", 200),
    coverImageUrl: img("photo-1511795409834-ef04bbd61622", 1200),
    location: "Los Angeles, CA",
    rating: 4.9,
    reviewCount: 214,
    verificationStatus: "verified",
    categories: ["Seating", "Tables", "Linens"],
    responseTimeHours: 2,
    createdAt: "2026-01-01T00:00:00.000Z",
    updatedAt: "2026-01-01T00:00:00.000Z",
  },
  {
    id: "vnd-2",
    name: "Lumen Collective",
    slug: "lumen-collective",
    description: "Architectural lighting and atmospheric design for elevated events.",
    logoUrl: img("photo-1556761175-b413da4baf72", 200),
    coverImageUrl: img("photo-1514525253161-7a46d19cd819", 1200),
    location: "Austin, TX",
    rating: 4.8,
    reviewCount: 168,
    verificationStatus: "verified",
    categories: ["Lighting", "AV & Sound"],
    responseTimeHours: 4,
    createdAt: "2026-01-01T00:00:00.000Z",
    updatedAt: "2026-01-01T00:00:00.000Z",
  },
  {
    id: "vnd-3",
    name: "Harbor & Grove",
    slug: "harbor-and-grove",
    description: "Outdoor structures, lounge vignettes, and destination-ready inventory.",
    logoUrl: img("photo-1542744173-8e7e53415bb0", 200),
    coverImageUrl: img("photo-1505236858219-8359eb29e329", 1200),
    location: "Miami, FL",
    rating: 4.9,
    reviewCount: 192,
    verificationStatus: "verified",
    categories: ["Structures", "Seating"],
    responseTimeHours: 3,
    createdAt: "2026-01-01T00:00:00.000Z",
    updatedAt: "2026-01-01T00:00:00.000Z",
  },
];

export const LANDING_RENTALS: Product[] = [
  {
    id: "prd-1",
    vendorId: "vnd-1",
    name: "Cross-Back Dining Chair",
    slug: "cross-back-dining-chair",
    description: "Natural wood finish with woven seat — ideal for reception dinners.",
    imageUrl: img("photo-1506439773649-6e0eb8cfb237", 900),
    categoryId: "cat-seating",
    dailyRate: 8,
    currency: "USD",
    available: true,
    createdAt: "2026-01-01T00:00:00.000Z",
    updatedAt: "2026-01-01T00:00:00.000Z",
  },
  {
    id: "prd-2",
    vendorId: "vnd-1",
    name: "Farm Table — 8ft",
    slug: "farm-table-8ft",
    description: "Reclaimed oak banquet table for family-style seating.",
    imageUrl: img("photo-1414235077428-338989a2e8c0", 900),
    categoryId: "cat-tables",
    dailyRate: 95,
    currency: "USD",
    available: true,
    createdAt: "2026-01-01T00:00:00.000Z",
    updatedAt: "2026-01-01T00:00:00.000Z",
  },
  {
    id: "prd-3",
    vendorId: "vnd-2",
    name: "Warm String Light Canopy",
    slug: "warm-string-light-canopy",
    description: "Soft amber canopy lighting for courtyards and gardens.",
    imageUrl: img("photo-1464366400600-7168b8af9bc3", 900),
    categoryId: "cat-lighting",
    dailyRate: 220,
    currency: "USD",
    available: true,
    createdAt: "2026-01-01T00:00:00.000Z",
    updatedAt: "2026-01-01T00:00:00.000Z",
  },
  {
    id: "prd-4",
    vendorId: "vnd-3",
    name: "Lounge Sofa Set",
    slug: "lounge-sofa-set",
    description: "Modular lounge vignette with coffee table and side chairs.",
    imageUrl: img("photo-1555041469-a586c61ea9bc", 900),
    categoryId: "cat-seating",
    dailyRate: 185,
    currency: "USD",
    available: true,
    createdAt: "2026-01-01T00:00:00.000Z",
    updatedAt: "2026-01-01T00:00:00.000Z",
  },
];

export const HOW_IT_WORKS = [
  {
    step: "01",
    title: "Discover",
    description:
      "Browse verified vendors and curated rental catalogs by category, location, and availability.",
  },
  {
    step: "02",
    title: "Compare",
    description:
      "Review pricing, ratings, and inventory side by side so every shortlist decision is clear.",
  },
  {
    step: "03",
    title: "Request",
    description:
      "Send booking requests from one workspace and keep vendor conversations organized.",
  },
] as const;

export const WHY_FLOWSET = [
  {
    title: "Verified vendors only",
    description:
      "Every partner is reviewed for reliability, inventory quality, and response standards.",
  },
  {
    title: "Built for planners",
    description:
      "Organize projects, compare quotes, and manage requests without spreadsheet chaos.",
  },
  {
    title: "One marketplace",
    description: "Seating, lighting, structures, and more — sourced from trusted local operators.",
  },
  {
    title: "Clear collaboration",
    description: "Keep stakeholders aligned with shared project context and booking status.",
  },
] as const;

export const TESTIMONIALS = [
  {
    quote:
      "FlowSet cut our vendor sourcing time in half. We shortlist faster and book with more confidence.",
    name: "Maya Chen",
    role: "Lead Planner, Northline Events",
  },
  {
    quote:
      "The marketplace feels curated, not crowded. Our clients notice the difference in quality immediately.",
    name: "Jordan Blake",
    role: "Creative Director, Studio Ember",
  },
  {
    quote:
      "Finally a platform that understands how planners actually work — clear catalogs, fast replies, no noise.",
    name: "Priya Nair",
    role: "Founder, Gather & Co.",
  },
] as const;

export const FAQS = [
  {
    question: "Who is FlowSet for?",
    answer:
      "FlowSet is built for professional event planners and verified rental vendors who need a reliable way to discover, compare, and book inventory.",
  },
  {
    question: "Are vendors verified?",
    answer:
      "Yes. Vendors go through a verification process covering business legitimacy, inventory standards, and response expectations before appearing in the marketplace.",
  },
  {
    question: "Can I request bookings from multiple vendors?",
    answer:
      "Absolutely. Create a project, shortlist products across vendors, and send booking requests from a single workspace.",
  },
  {
    question: "Is FlowSet available in my city?",
    answer:
      "We’re expanding market by market. Search by location on the marketplace to see active vendors near your event.",
  },
  {
    question: "Do I need an account to browse?",
    answer:
      "You can explore categories and catalogs freely. Creating an account unlocks projects, saved vendors, and booking requests.",
  },
] as const;
