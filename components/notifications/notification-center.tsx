"use client";

import * as React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  Bell,
  CheckCheck,
  ClipboardList,
  Package,
  Settings2,
  Truck,
  Warehouse,
} from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import {
  formatNotificationTime,
  formatUnreadBadge,
  notificationActionHref,
  notificationErrorMessage,
  notificationIconKind,
  notificationsInboxHref,
} from "@/lib/notifications/labels";
import { useAuth } from "@/providers";
import { notificationApi, type NotificationDto } from "@/services/notification-api";
import { cn } from "@/lib/utils";

function TypeIcon({ type }: { type: NotificationDto["type"] }) {
  const kind = notificationIconKind(type);
  const className = "size-3.5 shrink-0";
  if (kind === "booking") return <ClipboardList className={className} />;
  if (kind === "order") return <Package className={className} />;
  if (kind === "delivery") return <Truck className={className} />;
  if (kind === "inventory") return <Warehouse className={className} />;
  return <Settings2 className={className} />;
}

export function NotificationCenter() {
  const router = useRouter();
  const { role, isAuthenticated } = useAuth();
  const [open, setOpen] = React.useState(false);
  const [unreadCount, setUnreadCount] = React.useState(0);
  const [items, setItems] = React.useState<NotificationDto[]>([]);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const inboxHref = notificationsInboxHref(role);
  const badge = formatUnreadBadge(unreadCount);

  const refreshCount = React.useCallback(async () => {
    if (!isAuthenticated) {
      setUnreadCount(0);
      return;
    }
    try {
      const result = await notificationApi.unreadCount();
      setUnreadCount(result.unreadCount);
    } catch {
      // Keep the last known count; the panel surfaces errors on open.
    }
  }, [isAuthenticated]);

  React.useEffect(() => {
    void refreshCount();
    const timer = window.setInterval(() => {
      void refreshCount();
    }, 60_000);
    return () => window.clearInterval(timer);
  }, [refreshCount]);

  React.useEffect(() => {
    if (!open || !isAuthenticated) return;
    let cancelled = false;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const page = await notificationApi.list({ size: 8 });
        if (!cancelled) setItems(page.content);
      } catch (err) {
        if (!cancelled) {
          setError(notificationErrorMessage(err));
          setItems([]);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [open, isAuthenticated]);

  async function handleOpen(notification: NotificationDto) {
    const href = notificationActionHref(notification) ?? inboxHref;
    if (!notification.isRead) {
      try {
        await notificationApi.markRead(notification.id);
        setItems((current) =>
          current.map((row) => (row.id === notification.id ? { ...row, isRead: true } : row))
        );
        setUnreadCount((count) => Math.max(0, count - 1));
      } catch {
        // Navigation still proceeds.
      }
    }
    setOpen(false);
    router.push(href);
  }

  async function handleMarkAll() {
    try {
      await notificationApi.markAllRead();
      setItems((current) => current.map((row) => ({ ...row, isRead: true })));
      setUnreadCount(0);
    } catch (err) {
      setError(notificationErrorMessage(err, "Could not mark notifications as read"));
    }
  }

  return (
    <DropdownMenu open={open} onOpenChange={setOpen}>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          className="relative rounded-xl"
          aria-label="Notifications"
        >
          <Bell className="size-4" />
          {badge ? (
            <Badge className="absolute -top-1 -right-1 h-5 min-w-5 justify-center rounded-md px-1 text-[10px]">
              {badge}
            </Badge>
          ) : null}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-80">
        <DropdownMenuLabel className="flex items-center justify-between gap-2">
          <span>Notifications</span>
          {unreadCount > 0 ? (
            <Button
              variant="ghost"
              size="sm"
              className="h-7 rounded-lg px-2 text-xs"
              onClick={() => void handleMarkAll()}
            >
              <CheckCheck className="size-3.5" />
              Mark all as read
            </Button>
          ) : null}
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        {loading ? (
          <div className="px-2 py-3">
            <SkeletonLoader variant="list" count={3} />
          </div>
        ) : error ? (
          <p className="text-muted-foreground px-3 py-6 text-center text-sm">{error}</p>
        ) : items.length === 0 ? (
          <p className="text-muted-foreground px-3 py-6 text-center text-sm">
            You are all caught up.
          </p>
        ) : (
          items.map((notification) => (
            <DropdownMenuItem
              key={notification.id}
              className={cn(
                "flex cursor-pointer flex-col items-start gap-1 py-3",
                !notification.isRead && "bg-muted/50"
              )}
              onSelect={(event) => {
                event.preventDefault();
                void handleOpen(notification);
              }}
            >
              <div className="flex w-full items-center justify-between gap-2">
                <span className="flex min-w-0 items-center gap-2 font-medium">
                  <TypeIcon type={notification.type} />
                  <span className="truncate">{notification.title}</span>
                </span>
                {!notification.isRead ? (
                  <span className="bg-foreground size-1.5 shrink-0 rounded-full" />
                ) : null}
              </div>
              <span className="text-muted-foreground line-clamp-2 text-xs">
                {notification.message}
              </span>
              <span className="text-muted-foreground text-[11px]">
                {formatNotificationTime(notification.createdAt)}
              </span>
            </DropdownMenuItem>
          ))
        )}
        <DropdownMenuSeparator />
        <DropdownMenuItem asChild>
          <Link href={inboxHref}>View all</Link>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
