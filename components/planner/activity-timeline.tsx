"use client";

import Link from "next/link";
import { ClipboardList, FolderKanban, MessageSquare, Package, Receipt } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ROUTES } from "@/constants";
import type { ActivityItem, ActivityType } from "@/types";
import { cn } from "@/lib/utils";

const iconMap: Record<ActivityType, React.ComponentType<{ className?: string }>> = {
  booking: ClipboardList,
  quote: Receipt,
  message: MessageSquare,
  project: FolderKanban,
  order: Package,
};

interface ActivityTimelineProps {
  items: ActivityItem[];
  className?: string;
}

export function ActivityTimeline({ items, className }: ActivityTimelineProps) {
  return (
    <section className={cn(className)}>
      <div className="mb-5">
        <h2 className="text-xl font-semibold tracking-tight">Activity timeline</h2>
        <p className="text-muted-foreground mt-1 text-sm">
          A live feed of quotes, confirmations, and project updates.
        </p>
      </div>

      <Card className="overflow-hidden">
        <CardHeader className="pb-2">
          <CardTitle className="text-base">Recent activity</CardTitle>
        </CardHeader>
        <CardContent className="pt-2">
          {items.length === 0 ? (
            <p className="text-muted-foreground pb-4 text-sm">
              No activity yet. Booking requests, orders, and project updates will appear here.
            </p>
          ) : (
            <ol className="relative space-y-0">
              {items.map((item, index) => {
                const Icon = iconMap[item.type];
                const content = (
                  <div className="flex gap-4">
                    <div className="relative flex flex-col items-center">
                      <span className="border-border/70 bg-muted/50 shadow-soft flex size-9 items-center justify-center rounded-xl border">
                        <Icon className="text-foreground size-4" />
                      </span>
                      {index < items.length - 1 ? (
                        <span className="bg-border/80 mt-2 w-px flex-1" />
                      ) : null}
                    </div>
                    <div
                      className={cn("min-w-0 flex-1 pb-6", index === items.length - 1 && "pb-1")}
                    >
                      <div className="flex flex-wrap items-baseline justify-between gap-2">
                        <p className="text-sm font-medium tracking-tight">{item.title}</p>
                        <time className="text-muted-foreground text-xs">{item.timestamp}</time>
                      </div>
                      <p className="text-muted-foreground mt-1 text-sm leading-relaxed">
                        {item.description}
                      </p>
                    </div>
                  </div>
                );

                return (
                  <li
                    key={item.id}
                    className="animate-in fade-in slide-in-from-left-2 fill-mode-both duration-400"
                    style={{ animationDelay: `${index * 50}ms` }}
                  >
                    {item.href ? (
                      <Link
                        href={item.href}
                        className="hover:bg-muted/40 -mx-2 block rounded-xl px-2 transition-colors"
                      >
                        {content}
                      </Link>
                    ) : (
                      content
                    )}
                  </li>
                );
              })}
            </ol>
          )}

          <Button variant="outline" className="mt-2 w-full rounded-xl" asChild>
            <Link href={ROUTES.planner.notifications}>View all notifications</Link>
          </Button>
        </CardContent>
      </Card>
    </section>
  );
}
