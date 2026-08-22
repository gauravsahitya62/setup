"use client";

import Link from "next/link";
import { CalendarDays, MapPin, Users } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { UpcomingEvent } from "@/types";
import { ROUTES } from "@/constants";
import { cn } from "@/lib/utils";

const statusLabel: Record<UpcomingEvent["status"], string> = {
  DRAFT: "Draft",
  PLANNING: "Planning",
  ACTIVE: "Active",
  COMPLETED: "Completed",
  CANCELLED: "Cancelled",
};

interface UpcomingEventCardProps {
  event: UpcomingEvent;
  className?: string;
}

export function UpcomingEventCard({ event, className }: UpcomingEventCardProps) {
  return (
    <Card
      className={cn(
        "group transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md",
        className
      )}
    >
      <CardHeader className="flex-row items-start justify-between gap-3 space-y-0">
        <div>
          <CardTitle className="text-base">{event.name}</CardTitle>
          <p className="text-muted-foreground mt-1 text-sm">
            {event.date}
            {event.time ? ` · ${event.time}` : ""}
          </p>
        </div>
        <Badge variant="secondary" className="rounded-lg capitalize">
          {statusLabel[event.status]}
        </Badge>
      </CardHeader>
      <CardContent className="text-muted-foreground space-y-3 text-sm">
        <div className="flex items-center gap-2">
          <MapPin className="size-3.5 shrink-0" />
          <span className="line-clamp-1">{event.location}</span>
        </div>
        {typeof event.guestCount === "number" ? (
          <div className="flex items-center gap-2">
            <Users className="size-3.5 shrink-0" />
            <span>{event.guestCount} guests</span>
          </div>
        ) : null}
        <div className="flex items-center gap-2 pt-1">
          <CalendarDays className="size-3.5 shrink-0" />
          <Link
            href={`${ROUTES.planner.projects}/${event.projectId}`}
            className="text-foreground font-medium transition-colors hover:underline"
          >
            Open project
          </Link>
        </div>
      </CardContent>
    </Card>
  );
}

interface UpcomingEventsProps {
  events: UpcomingEvent[];
}

export function UpcomingEvents({ events }: UpcomingEventsProps) {
  return (
    <section>
      <div className="mb-5 flex items-end justify-between gap-4">
        <div>
          <h2 className="text-xl font-semibold tracking-tight">Upcoming events</h2>
          <p className="text-muted-foreground mt-1 text-sm">
            Dates on your calendar that still need rental coordination.
          </p>
        </div>
        <Button variant="outline" size="sm" className="rounded-xl" asChild>
          <Link href={ROUTES.planner.projects}>All projects</Link>
        </Button>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {events.map((event, index) => (
          <div
            key={event.id}
            className="animate-in fade-in slide-in-from-bottom-3 fill-mode-both duration-500"
            style={{ animationDelay: `${index * 60}ms` }}
          >
            <UpcomingEventCard event={event} />
          </div>
        ))}
      </div>
    </section>
  );
}
