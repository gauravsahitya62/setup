export const APP_NAME = "FlowSet";
export const APP_TAGLINE = "Powering Every Event.";

export { LANDING_CATEGORIES, LANDING_VENDORS, LANDING_RENTALS } from "./landing";
export {
  PLANNER_USER,
  PLANNER_BOOKING_REQUESTS,
  PLANNER_UPCOMING_EVENTS,
  PLANNER_RECOMMENDED_VENDORS,
  PLANNER_ACTIVITY,
  PLANNER_STATS,
} from "./planner";
export { PROJECT_WORKSPACE_TABS } from "./project-workspace";
export type { ProjectWorkspaceTabId } from "./project-workspace";
export {
  MARKETPLACE_LISTINGS,
  MARKETPLACE_CATEGORIES,
  MARKETPLACE_CITIES,
  DEFAULT_MARKETPLACE_FILTERS,
  filterMarketplaceListings,
} from "./marketplace";
export {
  VENDOR_PROFILES,
  getVendorProfile,
  getVendorReviews,
  getVendorListings,
  getAllVendorSlugs,
} from "./vendors";
export {
  VENDOR_DASHBOARD_SLUG,
  VENDOR_OPERATOR,
  VENDOR_INVENTORY,
  VENDOR_PENDING_REQUESTS,
  VENDOR_BOOKINGS,
  VENDOR_REVENUE_SERIES,
  VENDOR_CATEGORY_SHARE,
  VENDOR_KPI,
  VENDOR_AVAILABILITY,
  getVendorDashboardProfile,
  getVendorPopularProducts,
  getVendorDashboardReviews,
  inventorySummary,
} from "./vendor-dashboard";
export const ROUTES = {
  home: "/",
  notifications: "/notifications",
  marketplace: "/marketplace",
  product: (slug: string) => `/marketplace/${slug}`,
  vendors: "/vendors",
  categories: "/categories",
  category: (slug: string) => `/categories/${slug}`,
  planner: {
    root: "/planner",
    projects: "/planner/projects",
    projectNew: "/planner/projects/new",
    project: (id: string) => `/planner/projects/${id}`,
    projectMarketplace: (id: string) => `/planner/projects/${id}/marketplace`,
    marketplace: "/marketplace",
    favorites: "/planner/favorites",
    bookingRequests: "/planner/booking-requests",
    bookingRequest: (id: string) => `/planner/booking-requests/${id}`,
    projectBookingNew: (id: string) => `/planner/projects/${id}/booking`,
    orders: "/planner/orders",
    order: (id: string) => `/planner/orders/${id}`,
    reviews: "/planner/reviews",
    support: "/planner/support",
    ticket: (id: string) => `/planner/support/${id}`,
    notifications: "/planner/notifications",
    settings: "/planner/settings",
  },
  vendor: {
    root: "/vendor",
    catalog: "/vendor/catalog",
    inventory: "/vendor/inventory",
    inventoryDetail: (variantId: string) => `/vendor/inventory/${variantId}`,
    productWizard: "/vendor/catalog/new",
    requests: "/vendor/requests",
    request: (id: string) => `/vendor/requests/${id}`,
    bookings: "/vendor/bookings",
    order: (id: string) => `/vendor/bookings/${id}`,
    analytics: "/vendor/analytics",
    reviews: "/vendor/reviews",
    support: "/vendor/support",
    ticket: (id: string) => `/vendor/support/${id}`,
    availability: "/vendor/availability",
    profile: "/vendor/profile",
    profileWizard: "/vendor/profile/wizard",
    serviceAreas: "/vendor/service-areas",
    portfolio: "/vendor/portfolio",
    documents: "/vendor/documents",
    social: "/vendor/social",
    notifications: "/vendor/notifications",
    settings: "/vendor/settings",
  },
  admin: {
    root: "/admin",
    users: "/admin/users",
    vendors: "/admin/vendors",
    vendor: (id: string) => `/admin/vendors/${id}`,
    products: "/admin/products",
    categories: "/admin/categories",
    reviews: "/admin/reviews",
    orders: "/admin/orders",
    reports: "/admin/reports",
    tickets: "/admin/tickets",
    ticket: (id: string) => `/admin/tickets/${id}`,
    audit: "/admin/audit-logs",
    settings: "/admin/settings",
    notifications: "/admin/notifications",
    analytics: "/admin",
  },
  auth: {
    signIn: "/sign-in",
    signUp: "/sign-up",
    forgotPassword: "/forgot-password",
    resetPassword: "/reset-password",
  },
} as const;

export const BREAKPOINTS = {
  sm: 640,
  md: 768,
  lg: 1024,
  xl: 1280,
  "2xl": 1536,
} as const;

export const QUERY_KEYS = {
  vendors: "vendors",
  products: "products",
  categories: "categories",
  projects: "projects",
  bookings: "bookings",
  orders: "orders",
  notifications: "notifications",
  reviews: "reviews",
  admin: "admin",
  user: "user",
} as const;

export const DEFAULT_PAGE_SIZE = 12;

export const SIDEBAR_WIDTH = 280;
export const SIDEBAR_COLLAPSED_WIDTH = 72;
