"use client";

import * as React from "react";
import { toast } from "sonner";

import { AdminConfirmDialog } from "@/components/admin/admin-confirm-dialog";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { EmptyState } from "@/components/shared/empty-state";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { ReviewCard } from "@/components/reviews/review-card";
import { REVIEW_STATUS_LABELS, reviewErrorMessage } from "@/lib/reviews/labels";
import {
  ApiClientError,
  reviewApi,
  type ReviewDto,
  type ReviewModerationAction,
  type ReviewStatus,
} from "@/services";

const TABS: { id: string; label: string; status?: ReviewStatus; reported?: boolean }[] = [
  { id: "all", label: "All" },
  { id: "reported", label: "Reported", reported: true },
  { id: "published", label: "Published", status: "PUBLISHED" },
  { id: "hidden", label: "Hidden", status: "HIDDEN" },
  { id: "removed", label: "Removed", status: "REMOVED" },
];

export function AdminReviewsPage() {
  const [tab, setTab] = React.useState("all");
  const [rows, setRows] = React.useState<ReviewDto[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [note, setNote] = React.useState("");
  const [historyId, setHistoryId] = React.useState<string | null>(null);
  const [history, setHistory] = React.useState<string>("");
  const [pendingId, setPendingId] = React.useState<string | null>(null);
  const [pendingAction, setPendingAction] = React.useState<ReviewModerationAction | null>(null);

  const current = TABS.find((item) => item.id === tab) ?? TABS[0]!;

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const page = current.reported
        ? await reviewApi.admin.reported({ size: 50 })
        : await reviewApi.admin.list({ status: current.status, size: 50 });
      setRows(page.content);
    } catch (err) {
      setError(reviewErrorMessage(err));
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [current.reported, current.status]);

  React.useEffect(() => {
    void load();
  }, [load]);

  const moderate = async (id: string, action: ReviewModerationAction, reason?: string) => {
    try {
      await reviewApi.admin.moderate(id, { action, note: (reason ?? note).trim() || undefined });
      toast.success(`Review ${action.toLowerCase()}`);
      setNote("");
      await load();
    } catch (err) {
      toast.error(err instanceof ApiClientError ? err.message : reviewErrorMessage(err));
    }
  };

  const loadHistory = async (id: string) => {
    try {
      const rows = await reviewApi.admin.history(id);
      setHistoryId(id);
      setHistory(
        rows
          .map(
            (row) =>
              `${new Date(row.createdAt).toLocaleString()} · ${row.action}${row.note ? ` — ${row.note}` : ""}`
          )
          .join("\n") || "No moderation history."
      );
    } catch (err) {
      toast.error(reviewErrorMessage(err));
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Reviews</h1>
        <p className="text-muted-foreground mt-1 text-sm">
          Moderate reported reviews. Hide, restore, or remove — vendors cannot change planner
          feedback.
        </p>
      </div>
      <Tabs value={tab} onValueChange={setTab}>
        <TabsList variant="line">
          {TABS.map((item) => (
            <TabsTrigger key={item.id} value={item.id}>
              {item.label}
            </TabsTrigger>
          ))}
        </TabsList>
        {TABS.map((item) => (
          <TabsContent key={item.id} value={item.id} className="mt-6 space-y-4">
            {loading ? (
              <SkeletonLoader variant="text" count={6} />
            ) : error ? (
              <EmptyState
                title="Could not load reviews"
                description={error}
                actionLabel="Retry"
                onAction={() => void load()}
              />
            ) : rows.length === 0 ? (
              <EmptyState title="No reviews" description="Nothing in this queue." />
            ) : (
              rows.map((review) => (
                <div key={review.id} className="space-y-3 rounded-2xl border p-4">
                  <div className="text-muted-foreground flex flex-wrap items-center justify-between gap-2 text-xs">
                    <span>
                      {REVIEW_STATUS_LABELS[review.status] ?? review.status} · {review.vendorName} ·
                      order {review.orderId.slice(0, 8)}
                    </span>
                  </div>
                  <ReviewCard review={review} />
                  <Textarea
                    placeholder="Moderation note"
                    value={note}
                    onChange={(event) => setNote(event.target.value)}
                    rows={2}
                  />
                  <div className="flex flex-wrap gap-2">
                    <Button
                      size="sm"
                      className="rounded-xl"
                      onClick={() => {
                        setPendingId(review.id);
                        setPendingAction("HIDE");
                      }}
                    >
                      Hide
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      className="rounded-xl"
                      onClick={() => {
                        setPendingId(review.id);
                        setPendingAction("RESTORE");
                      }}
                    >
                      Restore
                    </Button>
                    <Button
                      size="sm"
                      variant="destructive"
                      className="rounded-xl"
                      onClick={() => {
                        setPendingId(review.id);
                        setPendingAction("REMOVE");
                      }}
                    >
                      Remove
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      className="rounded-xl"
                      onClick={() => void loadHistory(review.id)}
                    >
                      History
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      className="rounded-xl"
                      onClick={() => void moderate(review.id, "NOTE")}
                    >
                      Add note
                    </Button>
                  </div>
                  {historyId === review.id && history ? (
                    <pre className="bg-muted/40 rounded-xl p-3 text-xs whitespace-pre-wrap">
                      {history}
                    </pre>
                  ) : null}
                </div>
              ))
            )}
          </TabsContent>
        ))}
      </Tabs>
      <AdminConfirmDialog
        open={Boolean(pendingId && pendingAction)}
        title={
          pendingAction === "REMOVE"
            ? "Remove review"
            : pendingAction === "HIDE"
              ? "Hide review"
              : "Restore review"
        }
        description="This is recorded in the audit log. Removing a review requires a reason."
        confirmLabel="Confirm"
        requireReason={pendingAction === "REMOVE" || pendingAction === "HIDE"}
        onOpenChange={(open) => {
          if (!open) {
            setPendingId(null);
            setPendingAction(null);
          }
        }}
        onConfirm={async (reason) => {
          if (!pendingId || !pendingAction) return;
          await moderate(pendingId, pendingAction, reason);
          setPendingId(null);
          setPendingAction(null);
        }}
      />
    </div>
  );
}
