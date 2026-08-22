import Link from "next/link";
import { CalendarDays, MapPin, Package, Users } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import {
  EVENT_TYPE_LABELS,
  PROJECT_STATUS_LABELS,
  PROJECT_STATUS_STYLES,
  formatEventDate,
} from "@/lib/projects/labels";
import type { Project } from "@/types";
import { cn } from "@/lib/utils";

interface ProjectCardProps {
  project: Project;
  href?: string;
  className?: string;
}

export function ProjectCard({ project, href, className }: ProjectCardProps) {
  const content = (
    <Card
      className={cn(
        "group h-full transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md",
        className
      )}
    >
      <div className="from-muted to-muted/40 relative aspect-[16/9] overflow-hidden bg-gradient-to-br">
        {project.coverImageUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={project.coverImageUrl}
            alt={project.name}
            className="size-full object-cover transition-transform duration-500 group-hover:scale-[1.03]"
          />
        ) : (
          <div className="text-muted-foreground flex size-full items-center justify-center">
            <CalendarDays className="size-8 opacity-40" />
          </div>
        )}
        <Badge
          className={cn(
            "absolute top-3 left-3 border-0 shadow-sm",
            PROJECT_STATUS_STYLES[project.status]
          )}
        >
          {PROJECT_STATUS_LABELS[project.status]}
        </Badge>
      </div>

      <CardHeader>
        <p className="text-muted-foreground text-xs font-medium tracking-wide uppercase">
          {EVENT_TYPE_LABELS[project.eventType]}
        </p>
        <CardTitle className="line-clamp-1 text-base">{project.name}</CardTitle>
        {project.description ? (
          <p className="text-muted-foreground line-clamp-2 text-sm">{project.description}</p>
        ) : null}
      </CardHeader>

      <CardContent className="text-muted-foreground space-y-2 text-sm">
        {project.eventDate ? (
          <div className="flex items-center gap-2">
            <CalendarDays className="size-3.5" />
            <span>{formatEventDate(project.eventDate)}</span>
          </div>
        ) : null}
        {project.location ? (
          <div className="flex items-center gap-2">
            <MapPin className="size-3.5" />
            <span className="line-clamp-1">{project.location}</span>
          </div>
        ) : null}
        {typeof project.guestCount === "number" ? (
          <div className="flex items-center gap-2">
            <Users className="size-3.5" />
            <span>{project.guestCount} guests</span>
          </div>
        ) : null}
        <Progress value={project.progressPercent ?? 0} className="mt-2 h-1.5" />
      </CardContent>

      <CardFooter className="justify-between text-sm">
        <div className="text-muted-foreground flex items-center gap-1.5">
          <Package className="size-3.5" />
          <span>{project.productCount ?? project.itemCount ?? 0} products</span>
        </div>
        <span className="text-muted-foreground tabular-nums">{project.progressPercent ?? 0}%</span>
      </CardFooter>
    </Card>
  );

  if (href) {
    return (
      <Link href={href} className="block focus-visible:outline-none">
        {content}
      </Link>
    );
  }

  return content;
}
