"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  CalendarDays,
  ChevronRight,
  MapPin,
  MoreHorizontal,
  Pencil,
  Send,
  Store,
  Trash2,
  Users,
} from "lucide-react";
import { toast } from "sonner";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { ROUTES } from "@/constants";
import {
  EVENT_TYPE_LABELS,
  PROJECT_STATUS_LABELS,
  PROJECT_STATUS_STYLES,
  daysUntil,
  formatEventDate,
  formatProjectLocation,
} from "@/lib/projects/labels";
import { ApiClientError, projectApi } from "@/services";
import type { Project } from "@/types";
import { cn } from "@/lib/utils";

interface ProjectWorkspaceHeaderProps {
  project: Project;
  onEdit: () => void;
}

export function ProjectWorkspaceHeader({ project, onEdit }: ProjectWorkspaceHeaderProps) {
  const router = useRouter();
  const until = daysUntil(project.eventDate);
  const location = formatProjectLocation(project) ?? project.location;
  const progress = project.progressPercent ?? project.progress?.percent ?? 0;

  const handleDelete = async () => {
    if (
      !window.confirm("Delete this project? This is a soft delete and can be recovered by support.")
    ) {
      return;
    }
    try {
      await projectApi.remove(project.id);
      toast.success("Project deleted");
      router.push(ROUTES.planner.projects);
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not delete project");
    }
  };

  return (
    <header className="space-y-6">
      <Breadcrumb>
        <BreadcrumbList>
          <BreadcrumbItem>
            <BreadcrumbLink asChild>
              <Link href={ROUTES.planner.root}>Planner</Link>
            </BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbSeparator>
            <ChevronRight className="size-3.5" />
          </BreadcrumbSeparator>
          <BreadcrumbItem>
            <BreadcrumbLink asChild>
              <Link href={ROUTES.planner.projects}>Projects</Link>
            </BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbSeparator>
            <ChevronRight className="size-3.5" />
          </BreadcrumbSeparator>
          <BreadcrumbItem>
            <BreadcrumbPage className="max-w-[200px] truncate sm:max-w-none">
              {project.name}
            </BreadcrumbPage>
          </BreadcrumbItem>
        </BreadcrumbList>
      </Breadcrumb>

      <div className="border-border/70 bg-card shadow-soft overflow-hidden rounded-2xl border">
        <div className="relative h-36 overflow-hidden sm:h-44 lg:h-52">
          {project.coverImageUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={project.coverImageUrl} alt="" className="size-full object-cover" />
          ) : (
            <div className="from-muted via-muted/80 to-secondary size-full bg-gradient-to-br" />
          )}
          <div className="from-background via-background/40 absolute inset-0 bg-gradient-to-t to-transparent" />
        </div>

        <div className="relative -mt-16 space-y-5 px-5 pb-6 sm:px-7 lg:px-8">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div className="min-w-0 space-y-3">
              <div className="flex flex-wrap items-center gap-2">
                <Badge
                  className={cn(
                    "rounded-lg border-0 shadow-sm",
                    PROJECT_STATUS_STYLES[project.status]
                  )}
                >
                  {PROJECT_STATUS_LABELS[project.status]}
                </Badge>
                <Badge variant="secondary" className="rounded-lg">
                  {EVENT_TYPE_LABELS[project.eventType]}
                </Badge>
                {until !== null ? (
                  <span className="text-muted-foreground text-xs">
                    {until >= 0 ? `${until} days until event` : `${Math.abs(until)} days ago`}
                  </span>
                ) : null}
              </div>
              <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">{project.name}</h1>
              {project.description ? (
                <p className="text-muted-foreground max-w-2xl text-sm leading-relaxed sm:text-[15px]">
                  {project.description}
                </p>
              ) : null}
              <div className="text-muted-foreground flex flex-wrap gap-x-5 gap-y-2 text-sm">
                {project.eventDate ? (
                  <span className="inline-flex items-center gap-1.5">
                    <CalendarDays className="size-3.5" />
                    {formatEventDate(project.eventDate)}
                  </span>
                ) : null}
                {location ? (
                  <span className="inline-flex items-center gap-1.5">
                    <MapPin className="size-3.5" />
                    {location}
                  </span>
                ) : null}
                {typeof project.guestCount === "number" ? (
                  <span className="inline-flex items-center gap-1.5">
                    <Users className="size-3.5" />
                    {project.guestCount} guests
                  </span>
                ) : null}
              </div>
            </div>

            <div className="flex shrink-0 flex-wrap items-center gap-2">
              <Button variant="outline" size="sm" className="rounded-xl" onClick={onEdit}>
                <Pencil className="size-3.5" />
                Edit
              </Button>
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="outline" size="icon-sm" className="rounded-xl">
                    <MoreHorizontal className="size-4" />
                    <span className="sr-only">More actions</span>
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuItem onClick={onEdit}>Edit details</DropdownMenuItem>
                  <DropdownMenuItem asChild>
                    <Link href={ROUTES.planner.projectMarketplace(project.id)}>
                      Browse marketplace
                    </Link>
                  </DropdownMenuItem>
                  <DropdownMenuItem
                    className="text-destructive"
                    onClick={() => void handleDelete()}
                  >
                    <Trash2 className="size-3.5" />
                    Delete project
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
              {(project.productCount ?? 0) > 0 ? (
                <>
                  <Button variant="outline" size="sm" className="rounded-xl" asChild>
                    <Link href={ROUTES.planner.projectMarketplace(project.id)}>
                      <Store className="size-3.5" />
                      Add products
                    </Link>
                  </Button>
                  <Button size="sm" className="rounded-xl" asChild>
                    <Link href={ROUTES.planner.projectBookingNew(project.id)}>
                      <Send className="size-3.5" />
                      Request booking
                    </Link>
                  </Button>
                </>
              ) : (
                <Button size="sm" className="rounded-xl" asChild>
                  <Link href={ROUTES.planner.projectMarketplace(project.id)}>
                    <Store className="size-3.5" />
                    Add products
                  </Link>
                </Button>
              )}
            </div>
          </div>

          <div className="border-border/60 bg-background/80 rounded-xl border p-4 backdrop-blur-sm">
            <div className="mb-2 flex items-center justify-between gap-3 text-sm">
              <span className="font-medium">Planning progress</span>
              <span className="text-muted-foreground tabular-nums">{progress}%</span>
            </div>
            <Progress value={progress} className="h-2" />
            <p className="text-muted-foreground mt-2 font-mono text-[11px] tracking-widest">
              {"█".repeat(Math.round(progress / 10))}
              {"░".repeat(10 - Math.round(progress / 10))} {progress}%
            </p>
          </div>
        </div>
      </div>
    </header>
  );
}
