export type UserRole = "planner" | "vendor" | "admin" | "guest";

export type BookingStatus =
  "draft" | "requested" | "quoted" | "confirmed" | "in_progress" | "completed" | "cancelled";

export type VendorVerificationStatus = "pending" | "verified" | "rejected" | "suspended";

export interface BaseEntity {
  id: string;
  createdAt: string;
  updatedAt: string;
}

export interface User extends BaseEntity {
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  avatarUrl?: string;
  companyName?: string;
  phone?: string;
}

/** Authenticated session user returned by /auth/me */
export interface AuthUser extends User {
  emailVerified: boolean;
  status: "ACTIVE" | "PENDING_VERIFICATION" | "SUSPENDED";
}

export interface Vendor extends BaseEntity {
  name: string;
  slug: string;
  description: string;
  logoUrl?: string;
  coverImageUrl?: string;
  location: string;
  rating: number;
  reviewCount: number;
  verificationStatus: VendorVerificationStatus;
  categories: string[];
  responseTimeHours?: number;
}

export interface VendorSocialLinks {
  website?: string;
  instagram?: string;
  linkedin?: string;
  facebook?: string;
}

export interface VendorReview {
  id: string;
  authorName: string;
  authorRole?: string;
  authorAvatarUrl?: string;
  rating: number;
  title: string;
  body: string;
  createdAt: string;
  eventType?: string;
}

export interface VendorProfile extends Vendor {
  tagline?: string;
  about: string;
  completedEvents: number;
  yearsInBusiness: number;
  serviceCities: string[];
  portfolio: { id: string; imageUrl: string; caption?: string }[];
  social: VendorSocialLinks;
  highlights?: string[];
}

export interface Product extends BaseEntity {
  vendorId: string;
  name: string;
  slug: string;
  description: string;
  imageUrl?: string;
  categoryId: string;
  dailyRate: number;
  currency: string;
  available: boolean;
  minQuantity?: number;
}

/** Enriched listing used by the marketplace search experience. */
export interface MarketplaceListing extends Product {
  vendorName: string;
  vendorSlug: string;
  vendorVerified: boolean;
  city: string;
  rating: number;
  reviewCount: number;
  categorySlug: string;
  categoryName: string;
}

export interface MarketplaceFilters {
  query: string;
  categorySlug: string | "all";
  city: string | "all";
  priceMin: number;
  priceMax: number;
  availableOnly: boolean;
  minRating: number;
  verifiedOnly: boolean;
}

export interface Category extends BaseEntity {
  name: string;
  slug: string;
  description?: string;
  icon?: string;
  imageUrl?: string;
  productCount: number;
}

export interface Project extends BaseEntity {
  plannerId: string;
  name: string;
  description?: string;
  eventDate?: string;
  location?: string;
  status: BookingStatus;
  budget?: number;
  currency?: string;
  coverImageUrl?: string;
  vendorCount?: number;
  itemCount?: number;
}

export type MilestoneStatus = "completed" | "current" | "upcoming";

export interface ProjectMilestone {
  id: string;
  title: string;
  description?: string;
  date: string;
  status: MilestoneStatus;
}

export interface MoodBoardItem {
  id: string;
  imageUrl: string;
  caption?: string;
  source?: string;
}

export interface ProjectProductItem {
  id: string;
  listingId: string;
  name: string;
  imageUrl?: string;
  vendorName: string;
  categoryId: string;
  categoryName: string;
  quantity: number;
  dailyRate: number;
  currency: string;
  notes?: string;
}

export interface ProjectProductColumn {
  id: string;
  name: string;
  categorySlug: string;
  productIds: string[];
}

export interface BudgetLineItem {
  id: string;
  category: string;
  budgeted: number;
  spent: number;
  currency: string;
}

export interface ProjectFileItem {
  id: string;
  name: string;
  type: "pdf" | "image" | "spreadsheet" | "document" | "other";
  sizeLabel: string;
  uploadedAt: string;
  uploadedBy: string;
}

export interface ProjectNote {
  id: string;
  title: string;
  body: string;
  updatedAt: string;
  authorName: string;
  pinned?: boolean;
}

export interface ProjectWorkspaceStats {
  progressPercent: number;
  guestCount: number;
  daysUntilEvent: number;
  confirmedVendors: number;
  openRequests: number;
  totalItems: number;
  budgetUsedPercent: number;
}

export interface ProjectBudget {
  total: number;
  spent: number;
  currency: string;
  lines: BudgetLineItem[];
}

export interface ProjectWorkspace {
  project: Project;
  stats: ProjectWorkspaceStats;
  milestones: ProjectMilestone[];
  moodBoard: MoodBoardItem[];
  products: ProjectProductItem[];
  productColumns: ProjectProductColumn[];
  bookingRequests: BookingRequest[];
  budget: ProjectBudget;
  files: ProjectFileItem[];
  notes: ProjectNote[];
}

export interface BookingRequest extends BaseEntity {
  projectId: string;
  projectName: string;
  vendorId: string;
  vendorName: string;
  vendorLogoUrl?: string;
  itemsSummary: string;
  status: BookingStatus;
  amount?: number;
  currency?: string;
  requestedAt: string;
}

export interface UpcomingEvent {
  id: string;
  projectId: string;
  name: string;
  date: string;
  time?: string;
  location: string;
  status: BookingStatus;
  guestCount?: number;
}

export type ActivityType = "booking" | "quote" | "message" | "project" | "order";

export interface ActivityItem {
  id: string;
  type: ActivityType;
  title: string;
  description: string;
  timestamp: string;
  href?: string;
}

export interface PaginatedResponse<T> {
  data: T[];
  page: number;
  pageSize: number;
  total: number;
  totalPages: number;
}

export interface ApiError {
  message: string;
  code?: string;
  status?: number;
  details?: Record<string, string[]>;
  fieldErrors?: Record<string, string>;
  error?: string;
  path?: string;
  timestamp?: string;
}
