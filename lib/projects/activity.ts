export interface ActivityLike {
  id: string;
  description: string;
  createdAt: string;
}

export interface ActivityDayGroup {
  label: string;
  items: ActivityLike[];
}

function startOfDay(date: Date): number {
  const copy = new Date(date);
  copy.setHours(0, 0, 0, 0);
  return copy.getTime();
}

export function formatActivityTime(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleTimeString("en-IN", { hour: "numeric", minute: "2-digit" });
}

export function groupActivityByDay(items: ActivityLike[], now = new Date()): ActivityDayGroup[] {
  const today = startOfDay(now);
  const yesterday = today - 86_400_000;
  const groups = new Map<string, ActivityLike[]>();
  const order: string[] = [];

  for (const item of items) {
    const date = new Date(item.createdAt);
    const day = startOfDay(date);
    let label: string;
    if (day === today) label = "Today";
    else if (day === yesterday) label = "Yesterday";
    else {
      label = date.toLocaleDateString("en-IN", {
        day: "numeric",
        month: "short",
        year: "numeric",
      });
    }
    if (!groups.has(label)) {
      groups.set(label, []);
      order.push(label);
    }
    groups.get(label)!.push(item);
  }

  return order.map((label) => ({ label, items: groups.get(label) ?? [] }));
}

export function projectErrorMessage(error: unknown, fallback = "Something went wrong"): string {
  if (error && typeof error === "object" && "status" in error) {
    const status = (error as { status?: number }).status;
    const message = "message" in error ? String((error as { message?: string }).message) : "";
    if (status === 404) return "Project not found";
    if (status === 401) return "Please sign in to continue";
    if (status === 403) return "You do not have access to this project";
    if (message) return message;
  }
  if (error instanceof Error && error.message) return error.message;
  return fallback;
}
