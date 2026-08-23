"use client";

import Link from "next/link";
import { CalendarDays, ClipboardList, Package, Pin, Store, Users } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { ROUTES } from "@/constants";
import { daysUntil, formatEventDate, formatProjectLocation } from "@/lib/projects/labels";
import type { ProjectActivityDto, ProjectNoteDto } from "@/services";
import type { Project } from "@/types";
import { cn } from "@/lib/utils";

interface ProjectOverviewSectionProps {
  project: Project;
  notes: ProjectNoteDto[];
  activity: ProjectActivityDto[];
  onAddProducts: () => void;
}

export function ProjectOverviewSection({
  project,
  notes,
  activity,
  onAddProducts,
}: ProjectOverviewSectionProps) {
  const until = daysUntil(project.eventDate);
  const location = formatProjectLocation(project);
  const pinned = notes.find((note) => note.pinned) ?? notes[0];
  const pending = (project.progress?.tasks ?? []).filter((task) => !task.done);
  const cards = [
    {
      label: "Guests",
      value: project.guestCount != null ? String(project.guestCount) : "—",
      hint: until !== null ? `${until} days out` : "Set an event date",
      icon: Users,
    },
    {
      label: "Products",
      value: String(project.productCount ?? 0),
      hint: "Shortlisted variants",
      icon: Package,
    },
    {
      label: "Vendors",
      value: String(project.vendorCount ?? 0),
      hint: "Saved partners",
      icon: Store,
    },
    {
      label: "Open setup",
      value: String(pending.length),
      hint: "Planning tasks remaining",
      icon: ClipboardList,
    },
  ];

  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {cards.map((card, index) => (
          <Card
            key={card.label}
            className="animate-in fade-in slide-in-from-bottom-2 border-border/70 shadow-soft fill-mode-both duration-500"
            style={{ animationDelay: `${index * 50}ms` }}
          >
            <CardHeader className="flex-row items-start justify-between space-y-0 pb-2">
              <CardTitle className="text-muted-foreground text-sm font-medium">
                {card.label}
              </CardTitle>
              <div className="bg-muted/80 text-muted-foreground flex size-8 items-center justify-center rounded-lg">
                <card.icon className="size-4" />
              </div>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-semibold tracking-tight tabular-nums">{card.value}</p>
              <p className="text-muted-foreground mt-1 text-xs">{card.hint}</p>
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="grid gap-6 lg:grid-cols-5">
        <Card className="border-border/70 shadow-soft lg:col-span-3">
          <CardHeader>
            <CardTitle className="text-base">Event information</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4 text-sm">
            <p className="text-muted-foreground flex items-center gap-2">
              <CalendarDays className="size-3.5" />
              {formatEventDate(project.eventDate) ?? "Event date not set"}
            </p>
            <p>{location ?? "Venue not set"}</p>
            {project.venueAddress ? (
              <p className="text-muted-foreground">{project.venueAddress}</p>
            ) : null}
            <div>
              <div className="mb-2 flex items-center justify-between">
                <span className="font-medium">Planning progress</span>
                <span className="text-muted-foreground tabular-nums">
                  {project.progress?.percent ?? 0}%
                </span>
              </div>
              <Progress value={project.progress?.percent ?? 0} className="h-2" />
              <ul className="mt-4 grid gap-2 sm:grid-cols-2">
                {(project.progress?.tasks ?? []).map((task) => (
                  <li
                    key={task.id}
                    className={cn(
                      "rounded-lg border px-3 py-2 text-xs",
                      task.done
                        ? "border-emerald-500/20 text-emerald-700 dark:text-emerald-300"
                        : "border-border/60 text-muted-foreground"
                    )}
                  >
                    {task.done ? "✓" : "○"} {task.label}
                  </li>
                ))}
              </ul>
            </div>
            <div className="flex flex-wrap gap-2 pt-2">
              {(project.productCount ?? 0) > 0 ? (
                <>
                  <Button size="sm" className="rounded-xl" asChild>
                    <Link href={ROUTES.planner.projectBookingNew(project.id)}>Request booking</Link>
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    className="rounded-xl"
                    onClick={onAddProducts}
                  >
                    Add products
                  </Button>
                </>
              ) : (
                <Button size="sm" className="rounded-xl" onClick={onAddProducts}>
                  Add products
                </Button>
              )}
              <Button size="sm" variant="outline" className="rounded-xl" asChild>
                <Link href={ROUTES.vendors}>Save vendors</Link>
              </Button>
            </div>
          </CardContent>
        </Card>

        <div className="space-y-6 lg:col-span-2">
          {pinned ? (
            <Card className="border-border/70 shadow-soft">
              <CardHeader>
                <div className="flex items-center gap-2">
                  <Pin className="text-muted-foreground size-4" />
                  <CardTitle className="text-base">Pinned note</CardTitle>
                </div>
              </CardHeader>
              <CardContent>
                <p className="font-medium">{pinned.title}</p>
                <p className="text-muted-foreground mt-2 text-sm leading-relaxed">
                  {pinned.content}
                </p>
              </CardContent>
            </Card>
          ) : null}
          <Card className="border-border/70 shadow-soft">
            <CardHeader>
              <CardTitle className="text-base">Recent activity</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {activity.slice(0, 5).map((item) => (
                <p key={item.id} className="text-muted-foreground text-sm">
                  {item.description}
                </p>
              ))}
              {activity.length === 0 ? (
                <p className="text-muted-foreground text-sm">No activity yet.</p>
              ) : null}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
