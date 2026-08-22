"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import {
  Bell,
  CheckCheck,
  ClipboardList,
  Package,
  Settings2,
  Trash2,
  Truck,
  Warehouse,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/shared/empty-state";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import {
  NOTIFICATION_FILTER_LABELS,
  formatNotificationTime,
  notificationActionHref,
  notificationErrorMessage,
  notificationIconKind,
} from "@/lib/notifications/labels";
import {
  notificationApi,
  type NotificationDto,
  type NotificationFilter,
} from "@/services/notification-api";
import { cn } from "@/lib/utils";

const FILTERS: NotificationFilter[] = [
  "ALL",
  "UNREAD",
  "BOOKINGS",
  "ORDERS",
  "DELIVERY",
  "INVENTORY",
  "SYSTEM",
];

function TypeIcon({ type }: { type: NotificationDto["type"] }) {
  const kind = notificationIconKind(type);
  const className = "size-4";
  if (kind === "booking") return <ClipboardList className={className} />;
  if (kind === "order") return <Package className={className} />;
  if (kind === "delivery") return <Truck className={className} />;
  if (kind === "inventory") return <Warehouse className={className} />;
  return <Settings2 className={className} />;
}

export function NotificationInbox() {
  const router = useRouter();
  const [filter, setFilter] = React.useState<NotificationFilter>("ALL");
  const [page, setPage] = React.useState(0);
  const [rows, setRows] = React.useState<NotificationDto[]>([]);
  const [totalPages, setTotalPages] = React.useState(0);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await notificationApi.list({ filter, page, size: 20 });
      setRows(result.content);
      setTotalPages(result.totalPages);
    } catch (err) {
      setError(notificationErrorMessage(err));
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [filter, page]);

  React.useEffect(() => {
    void load();
  }, [load]);

  async function markRead(notification: NotificationDto) {
    if (notification.isRead) return;
    await notificationApi.markRead(notification.id);
    setRows((current) =>
      current.map((row) => (row.id === notification.id ? { ...row, isRead: true } : row))
    );
  }

  async function handleOpen(notification: NotificationDto) {
    try {
      await markRead(notification);
    } catch {
      // Still navigate.
    }
    const href = notificationActionHref(notification);
    if (href) router.push(href);
  }

  async function handleDelete(id: string) {
    await notificationApi.delete(id);
    setRows((current) => current.filter((row) => row.id !== id));
  }

  async function handleMarkAll() {
    await notificationApi.markAllRead();
    setRows((current) => current.map((row) => ({ ...row, isRead: true })));
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Notifications</h1>
          <p className="text-muted-foreground mt-1 text-sm">
            Personal alerts for bookings, orders, delivery, and inventory. Project history stays on
            the project activity tab.
          </p>
        </div>
        <Button variant="outline" className="rounded-xl" onClick={() => void handleMarkAll()}>
          <CheckCheck className="size-4" />
          Mark all as read
        </Button>
      </div>

      <div className="flex flex-wrap gap-2">
        {FILTERS.map((item) => (
          <Button
            key={item}
            size="sm"
            variant={filter === item ? "default" : "outline"}
            className="rounded-xl"
            onClick={() => {
              setFilter(item);
              setPage(0);
            }}
          >
            {NOTIFICATION_FILTER_LABELS[item]}
          </Button>
        ))}
      </div>

      {loading ? (
        <SkeletonLoader variant="list" count={6} />
      ) : error ? (
        <EmptyState
          title="Could not load notifications"
          description={error}
          icon={<Bell className="size-5" />}
        />
      ) : rows.length === 0 ? (
        <EmptyState
          title="No notifications"
          description="When vendors respond to requests or orders move, they will show up here."
          icon={<Bell className="size-5" />}
        />
      ) : (
        <ul className="divide-border/70 divide-y rounded-2xl border">
          {rows.map((notification) => (
            <li
              key={notification.id}
              className={cn(
                "flex items-start gap-3 px-4 py-4",
                !notification.isRead && "bg-muted/40"
              )}
            >
              <div className="bg-muted text-muted-foreground mt-0.5 flex size-9 shrink-0 items-center justify-center rounded-xl">
                <TypeIcon type={notification.type} />
              </div>
              <button
                type="button"
                className="min-w-0 flex-1 text-left"
                onClick={() => void handleOpen(notification)}
              >
                <div className="flex items-center gap-2">
                  <p className="font-medium">{notification.title}</p>
                  {!notification.isRead ? (
                    <span className="bg-foreground size-1.5 rounded-full" />
                  ) : null}
                </div>
                <p className="text-muted-foreground mt-1 text-sm">{notification.message}</p>
                <p className="text-muted-foreground mt-1 text-xs">
                  {formatNotificationTime(notification.createdAt)}
                </p>
              </button>
              <div className="flex shrink-0 gap-1">
                {!notification.isRead ? (
                  <Button
                    variant="ghost"
                    size="icon"
                    className="rounded-xl"
                    aria-label="Mark as read"
                    onClick={() => void markRead(notification)}
                  >
                    <CheckCheck className="size-4" />
                  </Button>
                ) : null}
                <Button
                  variant="ghost"
                  size="icon"
                  className="rounded-xl"
                  aria-label="Delete notification"
                  onClick={() => void handleDelete(notification.id)}
                >
                  <Trash2 className="size-4" />
                </Button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {totalPages > 1 ? (
        <div className="flex items-center justify-end gap-2">
          <Button
            variant="outline"
            size="sm"
            className="rounded-xl"
            disabled={page === 0}
            onClick={() => setPage((value) => Math.max(0, value - 1))}
          >
            Previous
          </Button>
          <span className="text-muted-foreground text-sm">
            Page {page + 1} of {totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            className="rounded-xl"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((value) => value + 1)}
          >
            Next
          </Button>
        </div>
      ) : null}
    </div>
  );
}
