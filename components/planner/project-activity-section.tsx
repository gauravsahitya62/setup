"use client";

import * as React from "react";

import { Card, CardContent } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";
import { formatActivityTime, groupActivityByDay } from "@/lib/projects/activity";
import { projectApi, type ProjectActivityDto } from "@/services";

interface ProjectActivitySectionProps {
  projectId: string;
}

export function ProjectActivitySection({ projectId }: ProjectActivitySectionProps) {
  const [items, setItems] = React.useState<ProjectActivityDto[]>([]);

  React.useEffect(() => {
    void projectApi.activity.list(projectId, 0, 50).then((page) => setItems(page.content));
  }, [projectId]);

  const groups = groupActivityByDay(items);

  if (items.length === 0) {
    return (
      <EmptyState
        title="No activity yet"
        description="Project actions will appear here as an immutable timeline."
      />
    );
  }

  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-lg font-semibold tracking-tight">Activity</h2>
        <p className="text-muted-foreground mt-1 text-sm">Chronological history of this project.</p>
      </div>
      <Card className="border-border/70 shadow-soft">
        <CardContent className="space-y-6 p-5">
          {groups.map((group) => (
            <section key={group.label}>
              <h3 className="text-muted-foreground mb-3 text-xs font-semibold tracking-wide uppercase">
                {group.label}
              </h3>
              <ol className="space-y-3">
                {group.items.map((item) => (
                  <li key={item.id} className="flex gap-4 text-sm">
                    <span className="text-muted-foreground w-20 shrink-0 tabular-nums">
                      {formatActivityTime(item.createdAt)}
                    </span>
                    <span>{item.description}</span>
                  </li>
                ))}
              </ol>
            </section>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
