import type { BookingStatus, MarketplaceListing, VendorProfile, VendorReview } from "@/types";
import { getVendorReviews } from "@/constants/vendors";
import { formatCurrency } from "@/lib/utils";

const img = (id: string, w = 900) =>
  `https://images.unsplash.com/${id}?auto=format&fit=crop&w=${w}&q=80`;

const now = "2026-01-01T00:00:00.000Z";

/**
 * Lightweight ops-dashboard placeholders used only for the overview widgets
 * (revenue chart, popular products, KPIs) until those views move to
 * aggregate analytics endpoints. The real product catalog powering
 * `/vendor/catalog` is fetched live via `marketplaceApi.vendorProducts.list`
 * in `VendorInventoryPage` — this list is intentionally not shared with the
 * public marketplace mock data (which has been removed).
 */
const DASHBOARD_PLACEHOLDER_LISTINGS: MarketplaceListing[] = [
  {
    id: "ph-1",
    vendorId: "vnd-self",
    name: "Cross-Back Dining Chair",
    slug: "cross-back-dining-chair",
    description: "Natural wood finish with woven seat.",
    imageUrl: img("photo-1506439773649-6e0eb8cfb237"),
    categoryId: "cat-seating",
    categoryName: "Seating",
    categorySlug: "seating",
    dailyRate: 8,
    currency: "USD",
    available: true,
    vendorName: "Your company",
    vendorSlug: "draft",
    vendorVerified: false,
    city: "",
    rating: 4.9,
    reviewCount: 214,
    createdAt: now,
    updatedAt: now,
  },
  {
    id: "ph-2",
    vendorId: "vnd-self",
    name: "Farm Table — 8ft",
    slug: "farm-table-8ft",
    description: "Reclaimed oak banquet table.",
    imageUrl: img("photo-1414235077428-338989a2e8c0"),
    categoryId: "cat-tables",
    categoryName: "Tables",
    categorySlug: "tables",
    dailyRate: 95,
    currency: "USD",
    available: true,
    vendorName: "Your company",
    vendorSlug: "draft",
    vendorVerified: false,
    city: "",
    rating: 4.9,
    reviewCount: 178,
    createdAt: now,
    updatedAt: now,
  },
  {
    id: "ph-3",
    vendorId: "vnd-self",
    name: "Warm String Light Canopy",
    slug: "warm-string-light-canopy",
    description: "Soft amber canopy lighting.",
    imageUrl: img("photo-1464366400600-7168b8af9bc3"),
    categoryId: "cat-lighting",
    categoryName: "Lighting",
    categorySlug: "lighting",
    dailyRate: 220,
    currency: "USD",
    available: true,
    vendorName: "Your company",
    vendorSlug: "draft",
    vendorVerified: false,
    city: "",
    rating: 4.8,
    reviewCount: 164,
    createdAt: now,
    updatedAt: now,
  },
  {
    id: "ph-4",
    vendorId: "vnd-self",
    name: "Linen Set — Ivory Soft Wash",
    slug: "linen-set-ivory-soft-wash",
    description: "Tablecloth and napkin set.",
    imageUrl: img("photo-1519225421980-715cb0215aed"),
    categoryId: "cat-linens",
    categoryName: "Linens",
    categorySlug: "linens",
    dailyRate: 32,
    currency: "USD",
    available: true,
    vendorName: "Your company",
    vendorSlug: "draft",
    vendorVerified: false,
    city: "",
    rating: 4.8,
    reviewCount: 201,
    createdAt: now,
    updatedAt: now,
  },
  {
    id: "ph-5",
    vendorId: "vnd-self",
    name: "Line Array Speaker Pair",
    slug: "line-array-speaker-pair",
    description: "Compact line-array pair.",
    imageUrl: img("photo-1492684223066-81342ee5ff30"),
    categoryId: "cat-av",
    categoryName: "AV & Sound",
    categorySlug: "av-sound",
    dailyRate: 260,
    currency: "USD",
    available: true,
    vendorName: "Your company",
    vendorSlug: "draft",
    vendorVerified: false,
    city: "",
    rating: 4.8,
    reviewCount: 93,
    createdAt: now,
    updatedAt: now,
  },
  {
    id: "ph-6",
    vendorId: "vnd-self",
    name: "Sailcloth Tent — 40ft",
    slug: "sailcloth-tent-40ft",
    description: "Elegant sailcloth tent.",
    imageUrl: img("photo-1530103862676-de8c9debad1d"),
    categoryId: "cat-structures",
    categoryName: "Structures",
    categorySlug: "structures",
    dailyRate: 420,
    currency: "USD",
    available: true,
    vendorName: "Your company",
    vendorSlug: "draft",
    vendorVerified: false,
    city: "",
    rating: 4.9,
    reviewCount: 119,
    createdAt: now,
    updatedAt: now,
  },
  {
    id: "ph-7",
    vendorId: "vnd-self",
    name: "Ghost Acrylic Chair",
    slug: "ghost-acrylic-chair",
    description: "Transparent acrylic seating.",
    imageUrl: img("photo-1506439773649-6e0eb8cfb237"),
    categoryId: "cat-seating",
    categoryName: "Seating",
    categorySlug: "seating",
    dailyRate: 12,
    currency: "USD",
    available: true,
    vendorName: "Your company",
    vendorSlug: "draft",
    vendorVerified: false,
    city: "",
    rating: 4.8,
    reviewCount: 96,
    createdAt: now,
    updatedAt: now,
  },
  {
    id: "ph-8",
    vendorId: "vnd-self",
    name: 'Round Banquet Table 60"',
    slug: "round-banquet-table-60",
    description: "Classic 60-inch round.",
    imageUrl: img("photo-1414235077428-338989a2e8c0"),
    categoryId: "cat-tables",
    categoryName: "Tables",
    categorySlug: "tables",
    dailyRate: 28,
    currency: "USD",
    available: true,
    vendorName: "Your company",
    vendorSlug: "draft",
    vendorVerified: false,
    city: "",
    rating: 4.7,
    reviewCount: 220,
    createdAt: now,
    updatedAt: now,
  },
];

export const VENDOR_DASHBOARD_SLUG = "draft";

export const VENDOR_OPERATOR = {
  id: "usr-vendor-1",
  firstName: "Alex",
  lastName: "Rivera",
  email: "alex@atlas.example.com",
  role: "Owner",
} as const;

/** Placeholder only — live company name/slug come from `useMyVendor` / auth. */
export function getVendorDashboardProfile(): VendorProfile {
  const now = new Date().toISOString();
  return {
    id: "pending",
    name: "Your company",
    slug: VENDOR_DASHBOARD_SLUG,
    description: "",
    about: "",
    location: "",
    rating: 0,
    reviewCount: 0,
    verificationStatus: "pending",
    categories: [],
    completedEvents: 0,
    yearsInBusiness: 0,
    serviceCities: [],
    portfolio: [],
    social: {},
    createdAt: now,
    updatedAt: now,
  };
}

export type InventoryStatus = "active" | "draft" | "archived";

export interface VendorInventoryItem extends MarketplaceListing {
  sku: string;
  stock: number;
  reserved: number;
  status: InventoryStatus;
  updatedLabel: string;
}

export interface VendorPendingRequest {
  id: string;
  plannerName: string;
  companyName: string;
  projectName: string;
  itemsSummary: string;
  amount: number;
  currency: string;
  status: BookingStatus;
  requestedAt: string;
  eventDate: string;
}

export interface VendorBooking {
  id: string;
  projectName: string;
  plannerName: string;
  eventDate: string;
  amount: number;
  currency: string;
  status: "confirmed" | "in_progress" | "completed";
  itemsCount: number;
}

export interface AvailabilityDay {
  date: string; // ISO date
  status: "open" | "limited" | "blocked" | "booked";
  label?: string;
}

export interface RevenuePoint {
  month: string;
  revenue: number;
  bookings: number;
}

export interface CategorySharePoint {
  name: string;
  value: number;
}

function buildInventory(): VendorInventoryItem[] {
  // Temporary ops placeholders for the dashboard overview widgets only.
  return DASHBOARD_PLACEHOLDER_LISTINGS.slice(0, 8).map((listing, index) => {
    const stock = [120, 80, 24, 200, 45, 60, 18, 90, 36, 150][index % 10] ?? 40;
    const reserved = Math.floor(stock * ([0.1, 0.25, 0.4, 0.05, 0.15][index % 5] ?? 0.1));
    const statuses: InventoryStatus[] = ["active", "active", "active", "draft", "archived"];
    return {
      ...listing,
      sku: `ATL-${String(1000 + index)}`,
      stock,
      reserved,
      status: statuses[index % statuses.length] ?? "active",
      updatedLabel: ["2h ago", "Yesterday", "3 days ago", "1 week ago"][index % 4] ?? "Recently",
    };
  });
}

export const VENDOR_INVENTORY: VendorInventoryItem[] = buildInventory();

export const VENDOR_PENDING_REQUESTS: VendorPendingRequest[] = [];

export const VENDOR_BOOKINGS: VendorBooking[] = [];

export const VENDOR_REVENUE_SERIES: RevenuePoint[] = [
  { month: "Jan", revenue: 42000, bookings: 18 },
  { month: "Feb", revenue: 38000, bookings: 15 },
  { month: "Mar", revenue: 51000, bookings: 22 },
  { month: "Apr", revenue: 47000, bookings: 20 },
  { month: "May", revenue: 62000, bookings: 27 },
  { month: "Jun", revenue: 71000, bookings: 31 },
  { month: "Jul", revenue: 68000, bookings: 29 },
];

export const VENDOR_CATEGORY_SHARE: CategorySharePoint[] = [
  { name: "Seating", value: 42 },
  { name: "Tables", value: 28 },
  { name: "Linens", value: 18 },
  { name: "Other", value: 12 },
];

export const VENDOR_KPI = {
  revenueMonth: 68000,
  revenueChange: 8.4,
  pendingRequests: 0,
  activeSkus: VENDOR_INVENTORY.filter((item) => item.status === "active").length,
  utilization: 74,
  avgOrderValue: 8400,
} as const;

/** Generate availability for current month-ish using fixed July 2026 dates for demo stability. */
export const VENDOR_AVAILABILITY: AvailabilityDay[] = (() => {
  const days: AvailabilityDay[] = [];
  for (let day = 1; day <= 31; day += 1) {
    const date = `2026-07-${String(day).padStart(2, "0")}`;
    let status: AvailabilityDay["status"] = "open";
    if ([3, 11, 19, 27].includes(day)) status = "booked";
    else if ([7, 8, 21, 22].includes(day)) status = "limited";
    else if ([14].includes(day)) status = "blocked";
    days.push({
      date,
      status,
      label:
        status === "booked"
          ? "Harbor Gala prep"
          : status === "limited"
            ? "Partial holds"
            : status === "blocked"
              ? "Warehouse closed"
              : undefined,
    });
  }
  return days;
})();

export function getVendorPopularProducts(): VendorInventoryItem[] {
  return [...VENDOR_INVENTORY]
    .filter((item) => item.status === "active")
    .sort((a, b) => b.reviewCount - a.reviewCount)
    .slice(0, 4);
}

export function getVendorDashboardReviews(): VendorReview[] {
  return getVendorReviews(VENDOR_DASHBOARD_SLUG).slice(0, 3);
}

export function inventorySummary() {
  const active = VENDOR_INVENTORY.filter((i) => i.status === "active").length;
  const draft = VENDOR_INVENTORY.filter((i) => i.status === "draft").length;
  const lowStock = VENDOR_INVENTORY.filter((i) => i.stock - i.reserved < 20).length;
  const reservedUnits = VENDOR_INVENTORY.reduce((sum, i) => sum + i.reserved, 0);
  const totalUnits = VENDOR_INVENTORY.reduce((sum, i) => sum + i.stock, 0);
  return { active, draft, lowStock, reservedUnits, totalUnits };
}

export function formatVendorMoney(amount: number, currency = "USD") {
  return formatCurrency(amount, currency);
}
