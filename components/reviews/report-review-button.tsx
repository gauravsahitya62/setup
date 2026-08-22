"use client";

import * as React from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Modal } from "@/components/shared/modal";
import { REVIEW_REPORT_REASON_LABELS, reviewErrorMessage } from "@/lib/reviews/labels";
import { ApiClientError, reviewApi, type ReviewReportReason } from "@/services";
import { useAuth } from "@/providers";

const REASONS = Object.keys(REVIEW_REPORT_REASON_LABELS) as ReviewReportReason[];

export function ReportReviewButton({ reviewId }: { reviewId: string }) {
  const { user } = useAuth();
  const [open, setOpen] = React.useState(false);
  const [reason, setReason] = React.useState<ReviewReportReason>("SPAM");
  const [description, setDescription] = React.useState("");
  const [busy, setBusy] = React.useState(false);

  if (!user) return null;

  const submit = async () => {
    setBusy(true);
    try {
      await reviewApi.report(reviewId, { reason, description: description.trim() || undefined });
      toast.success("Report submitted");
      setOpen(false);
    } catch (err) {
      toast.error(
        err instanceof ApiClientError
          ? err.message
          : reviewErrorMessage(err, "Could not report review")
      );
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <Button
        type="button"
        size="sm"
        variant="ghost"
        className="rounded-xl"
        onClick={() => setOpen(true)}
      >
        Report
      </Button>
      <Modal
        open={open}
        onOpenChange={setOpen}
        title="Report review"
        description="Tell us why this review should be reviewed by FlowSet."
      >
        <div className="space-y-3">
          <div className="space-y-1">
            <Label htmlFor="report-reason">Reason</Label>
            <select
              id="report-reason"
              className="bg-background h-9 w-full rounded-xl border px-3 text-sm"
              value={reason}
              onChange={(event) => setReason(event.target.value as ReviewReportReason)}
            >
              {REASONS.map((item) => (
                <option key={item} value={item}>
                  {REVIEW_REPORT_REASON_LABELS[item]}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-1">
            <Label htmlFor="report-description">Details (optional)</Label>
            <Textarea
              id="report-description"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              rows={3}
            />
          </div>
          <Button className="rounded-xl" disabled={busy} onClick={() => void submit()}>
            Submit report
          </Button>
        </div>
      </Modal>
    </>
  );
}
