export const PROJECT_WORKSPACE_TABS = [
  { id: "overview", label: "Overview" },
  { id: "products", label: "Products" },
  { id: "vendors", label: "Vendors" },
  { id: "moodboard", label: "Mood Board" },
  { id: "notes", label: "Notes" },
  { id: "files", label: "Files" },
  { id: "activity", label: "Activity" },
  { id: "bookings", label: "Booking Requests" },
  { id: "orders", label: "Orders" },
] as const;

export type ProjectWorkspaceTabId = (typeof PROJECT_WORKSPACE_TABS)[number]["id"];
