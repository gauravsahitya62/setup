import type { ActivityItem, BookingRequest, UpcomingEvent, User, Vendor } from "@/types";

const img = (id: string, w = 1200) =>
  `https://images.unsplash.com/${id}?auto=format&fit=crop&w=${w}&q=80`;

export const PLANNER_USER: User = {
  id: "usr-planner-1",
  email: "maya@northline.events",
  firstName: "Maya",
  lastName: "Chen",
  role: "planner",
  companyName: "Northline Events",
  avatarUrl: img("photo-1494790108377-be9c29b29330", 200),
  createdAt: "2025-06-01T00:00:00.000Z",
  updatedAt: "2026-07-01T00:00:00.000Z",
};

export const PLANNER_BOOKING_REQUESTS: BookingRequest[] = [];

export const PLANNER_UPCOMING_EVENTS: UpcomingEvent[] = [
  {
    id: "evt-1",
    projectId: "prj-2",
    name: "Rooftop Summer Soirée",
    date: "Aug 14, 2026",
    time: "6:00 PM",
    location: "Embarcadero Rooftop",
    status: "PLANNING",
    guestCount: 140,
  },
  {
    id: "evt-2",
    projectId: "prj-1",
    name: "Harbor Gala 2026",
    date: "Sep 28, 2026",
    time: "5:30 PM",
    location: "Pier 45, San Francisco",
    status: "ACTIVE",
    guestCount: 320,
  },
  {
    id: "evt-3",
    projectId: "prj-3",
    name: "Product Launch — Meridian",
    date: "Oct 3, 2026",
    time: "4:00 PM",
    location: "Fort Mason Center",
    status: "DRAFT",
    guestCount: 220,
  },
];

export const PLANNER_RECOMMENDED_VENDORS: Vendor[] = [
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

export const PLANNER_ACTIVITY: ActivityItem[] = [
  {
    id: "act-1",
    type: "quote",
    title: "New quote from Lumen Collective",
    description: "Lighting package for Rooftop Summer Soirée — $4,200",
    timestamp: "12 minutes ago",
    href: "/planner/booking-requests",
  },
  {
    id: "act-2",
    type: "booking",
    title: "Booking confirmed",
    description: "Harbor & Grove confirmed lounge inventory for Harbor Gala.",
    timestamp: "2 hours ago",
    href: "/planner/orders",
  },
  {
    id: "act-3",
    type: "message",
    title: "Message from Atlas Event Rentals",
    description: "Asked about load-in window for Meridian launch.",
    timestamp: "Yesterday",
    href: "/planner/booking-requests",
  },
  {
    id: "act-4",
    type: "project",
    title: "Project updated",
    description: "You revised the guest count on Private Estate Wedding.",
    timestamp: "2 days ago",
    href: "/planner/projects",
  },
  {
    id: "act-5",
    type: "order",
    title: "Order invoice ready",
    description: "Deposit invoice for Harbor Gala seating is ready to review.",
    timestamp: "3 days ago",
    href: "/planner/orders",
  },
];

export const PLANNER_STATS = [
  { label: "Active projects", value: "4" },
  { label: "Open requests", value: "3" },
  { label: "Upcoming events", value: "3" },
  { label: "Vendors engaged", value: "7" },
] as const;
