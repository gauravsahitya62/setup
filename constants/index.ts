export const APP_NAME = "FlowSet";
export const APP_TAGLINE = "Powering Every Event.";

export { LANDING_CATEGORIES, LANDING_VENDORS, LANDING_RENTALS } from "./landing";
export {
  PLANNER_USER,
  PLANNER_PROJECTS,
  PLANNER_BOOKING_REQUESTS,
  PLANNER_UPCOMING_EVENTS,
  PLANNER_RECOMMENDED_VENDORS,
  PLANNER_ACTIVITY,
  PLANNER_STATS,
  PLANNER_NOTIFICATIONS,
} from "./planner";
export {
  getProjectWorkspace,
  getAllProjectWorkspaceIds,
  PROJECT_WORKSPACE_TABS,
} from "./project-workspace";
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
export {
  ADMIN_OPERATOR,
  ADMIN_VENDOR_QUEUE,
  ADMIN_CATEGORIES,
  ADMIN_PRODUCT_QUEUE,
  ADMIN_USERS,
  ADMIN_REPORTS,
  ADMIN_AUDIT_LOGS,
  ADMIN_TICKETS,
  ADMIN_KPI,
  ADMIN_GMV_SERIES,
  ADMIN_ROLE_MIX,
  adminPendingVerifications,
  adminOpenTickets,
  formatAdminMoney,
} from "./admin";

export const ROUTES = {
  home: "/",
  marketplace: "/marketplace",
  vendors: "/vendors",
  categories: "/categories",
  planner: {
    root: "/planner",
    projects: "/planner/projects",
    project: (id: string) => `/planner/projects/${id}`,
    marketplace: "/marketplace",
    bookingRequests: "/planner/booking-requests",
    orders: "/planner/orders",
    notifications: "/planner/notifications",
    settings: "/planner/settings",
  },
  vendor: {
    root: "/vendor",
    catalog: "/vendor/catalog",
    requests: "/vendor/requests",
    bookings: "/vendor/bookings",
    analytics: "/vendor/analytics",
    reviews: "/vendor/reviews",
    availability: "/vendor/availability",
    settings: "/vendor/settings",
  },
  admin: {
    root: "/admin",
    vendors: "/admin/vendors",
    categories: "/admin/categories",
    products: "/admin/products",
    users: "/admin/users",
    reports: "/admin/reports",
    audit: "/admin/audit",
    analytics: "/admin/analytics",
    tickets: "/admin/tickets",
    settings: "/admin/settings",
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
  user: "user",
} as const;

export const DEFAULT_PAGE_SIZE = 12;

export const SIDEBAR_WIDTH = 280;
export const SIDEBAR_COLLAPSED_WIDTH = 72;
