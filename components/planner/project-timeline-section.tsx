import { Check, Circle, CircleDot } from "lucide-react";

import { Card, CardContent } from "@/components/ui/card";
import type { ProjectMilestone } from "@/types";
import { cn } from "@/lib/utils";

interface ProjectTimelineSectionProps {
  milestones: ProjectMilestone[];
}

export function ProjectTimelineSection({ milestones }: ProjectTimelineSectionProps) {
  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-lg font-semibold tracking-tight">Timeline</h2>
        <p className="text-muted-foreground mt-1 text-sm">
          Milestones from concept lock through event day.
        </p>
      </div>

      <Card className="border-border/70 shadow-soft">
        <CardContent className="p-5 sm:p-7">
          <ol className="relative space-y-0">
            {milestones.map((milestone, index) => {
              const isLast = index === milestones.length - 1;
              const Icon =
                milestone.status === "completed"
                  ? Check
                  : milestone.status === "current"
                    ? CircleDot
                    : Circle;

              return (
                <li
                  key={milestone.id}
                  className="animate-in fade-in slide-in-from-left-2 fill-mode-both relative flex gap-4 pb-8 duration-500 last:pb-0"
                  style={{ animationDelay: `${index * 60}ms` }}
                >
                  {!isLast ? (
                    <span
                      className={cn(
                        "absolute top-9 left-[15px] h-[calc(100%-28px)] w-px",
                        milestone.status === "completed" ? "bg-foreground/20" : "bg-border"
                      )}
                      aria-hidden
                    />
                  ) : null}

                  <div
                    className={cn(
                      "relative z-10 flex size-8 shrink-0 items-center justify-center rounded-full border",
                      milestone.status === "completed" &&
                        "border-foreground/20 bg-foreground text-background",
                      milestone.status === "current" &&
                        "border-emerald-500/40 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300",
                      milestone.status === "upcoming" &&
                        "border-border bg-background text-muted-foreground"
                    )}
                  >
                    <Icon className="size-3.5" strokeWidth={2.5} />
                  </div>

                  <div
                    className={cn(
                      "min-w-0 flex-1 rounded-xl border px-4 py-3.5 transition-colors",
                      milestone.status === "current"
                        ? "border-emerald-500/25 bg-emerald-500/[0.04]"
                        : "border-border/60 bg-muted/15"
                    )}
                  >
                    <div className="flex flex-wrap items-baseline justify-between gap-2">
                      <h3 className="font-medium tracking-tight">{milestone.title}</h3>
                      <time className="text-muted-foreground text-xs">{milestone.date}</time>
                    </div>
                    {milestone.description ? (
                      <p className="text-muted-foreground mt-1.5 text-sm leading-relaxed">
                        {milestone.description}
                      </p>
                    ) : null}
                    <p className="text-muted-foreground mt-2 text-[11px] font-medium tracking-wide uppercase">
                      {milestone.status === "completed"
                        ? "Completed"
                        : milestone.status === "current"
                          ? "In progress"
                          : "Upcoming"}
                    </p>
                  </div>
                </li>
              );
            })}
          </ol>
        </CardContent>
      </Card>
    </div>
  );
}
