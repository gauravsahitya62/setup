"use client";

import * as React from "react";

import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Modal } from "@/components/shared/modal";

interface AdminConfirmDialogProps {
  open: boolean;
  title: string;
  description: string;
  confirmLabel: string;
  requireReason?: boolean;
  pending?: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: (reason: string) => void | Promise<void>;
}

export function AdminConfirmDialog({
  open,
  title,
  description,
  confirmLabel,
  requireReason,
  pending,
  onOpenChange,
  onConfirm,
}: AdminConfirmDialogProps) {
  const [reason, setReason] = React.useState("");
  const [busy, setBusy] = React.useState(false);

  React.useEffect(() => {
    if (!open) {
      setReason("");
      setBusy(false);
    }
  }, [open]);

  const disabled = pending || busy || (requireReason && reason.trim().length < 3);

  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      title={title}
      description={description}
      footer={
        <>
          <Button variant="outline" className="rounded-xl" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button
            variant="destructive"
            className="rounded-xl"
            disabled={disabled}
            onClick={() => {
              void (async () => {
                setBusy(true);
                try {
                  await onConfirm(reason.trim());
                } finally {
                  setBusy(false);
                }
              })();
            }}
          >
            {confirmLabel}
          </Button>
        </>
      }
    >
      {requireReason ? (
        <Textarea
          value={reason}
          onChange={(event) => setReason(event.target.value)}
          placeholder="Record a reason for the audit log"
          className="min-h-24 rounded-xl"
        />
      ) : (
        <p className="text-muted-foreground text-sm">This action is recorded in the audit log.</p>
      )}
    </Modal>
  );
}
