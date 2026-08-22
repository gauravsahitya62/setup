import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import type { ProjectBudget } from "@/types";
import { cn, formatCurrency } from "@/lib/utils";

interface ProjectBudgetSectionProps {
  budget: ProjectBudget;
}

export function ProjectBudgetSection({ budget }: ProjectBudgetSectionProps) {
  const remaining = Math.max(0, budget.total - budget.spent);
  const usedPercent =
    budget.total > 0 ? Math.min(100, Math.round((budget.spent / budget.total) * 100)) : 0;

  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-lg font-semibold tracking-tight">Budget</h2>
        <p className="text-muted-foreground mt-1 text-sm">
          Track spend by category against the production budget.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        {[
          { label: "Total budget", value: formatCurrency(budget.total, budget.currency) },
          { label: "Spent", value: formatCurrency(budget.spent, budget.currency) },
          { label: "Remaining", value: formatCurrency(remaining, budget.currency) },
        ].map((stat, index) => (
          <Card
            key={stat.label}
            className="animate-in fade-in slide-in-from-bottom-2 border-border/70 shadow-soft fill-mode-both duration-500"
            style={{ animationDelay: `${index * 50}ms` }}
          >
            <CardHeader className="pb-2">
              <CardTitle className="text-muted-foreground text-sm font-medium">
                {stat.label}
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-semibold tracking-tight tabular-nums">{stat.value}</p>
            </CardContent>
          </Card>
        ))}
      </div>

      <Card className="border-border/70 shadow-soft">
        <CardHeader>
          <div className="flex items-center justify-between gap-3">
            <CardTitle className="text-base">Overall utilization</CardTitle>
            <span className="text-muted-foreground text-sm tabular-nums">{usedPercent}%</span>
          </div>
          <Progress value={usedPercent} className="mt-3 h-2.5" />
        </CardHeader>
        <CardContent className="space-y-3">
          {budget.lines.map((line, index) => {
            const pct =
              line.budgeted > 0 ? Math.min(100, Math.round((line.spent / line.budgeted) * 100)) : 0;
            const over = line.spent > line.budgeted;

            return (
              <div
                key={line.id}
                className={cn(
                  "border-border/60 bg-muted/15 animate-in fade-in slide-in-from-bottom-2 fill-mode-both rounded-xl border px-4 py-3.5 duration-500"
                )}
                style={{ animationDelay: `${index * 40}ms` }}
              >
                <div className="mb-2.5 flex flex-wrap items-center justify-between gap-2">
                  <p className="font-medium">{line.category}</p>
                  <p className="text-muted-foreground text-sm tabular-nums">
                    <span
                      className={cn("font-medium", over ? "text-destructive" : "text-foreground")}
                    >
                      {formatCurrency(line.spent, line.currency)}
                    </span>
                    {" / "}
                    {formatCurrency(line.budgeted, line.currency)}
                  </p>
                </div>
                <Progress
                  value={pct}
                  className={cn(
                    "h-1.5",
                    over && "[&_[data-slot=progress-indicator]]:bg-destructive"
                  )}
                />
              </div>
            );
          })}
        </CardContent>
      </Card>
    </div>
  );
}
