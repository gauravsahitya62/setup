"use client";

import * as React from "react";
import { toast } from "sonner";

import { AdminPageHeader } from "@/components/admin/admin-page-header";
import { AdminStatusBadge, type AdminStatusTone } from "@/components/admin/admin-status-badge";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { EmptyState } from "@/components/shared/empty-state";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { TICKET_PRIORITY_LABEL, TICKET_STATUS_LABEL, adminErrorMessage } from "@/lib/admin/labels";
import { useAuth } from "@/providers";
import {
  ApiClientError,
  adminApi,
  type SupportTicketDto,
  type SupportTicketPriority,
  type SupportTicketStatus,
  type SupportTicketSummaryDto,
} from "@/services";

const STATUS_TONE: Record<string, AdminStatusTone> = {
  OPEN: "info",
  IN_PROGRESS: "warning",
  WAITING_FOR_USER: "neutral",
  RESOLVED: "success",
  CLOSED: "neutral",
};

const PRIORITY_TONE: Record<string, AdminStatusTone> = {
  LOW: "neutral",
  MEDIUM: "info",
  HIGH: "warning",
  URGENT: "danger",
};

export function AdminTicketsPage() {
  const { user } = useAuth();
  const [status, setStatus] = React.useState("");
  const [priority, setPriority] = React.useState("");
  const [page, setPage] = React.useState(0);
  const [rows, setRows] = React.useState<SupportTicketSummaryDto[]>([]);
  const [totalPages, setTotalPages] = React.useState(0);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [selected, setSelected] = React.useState<SupportTicketDto | null>(null);
  const [reply, setReply] = React.useState("");

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await adminApi.supportTickets({
        status: status || undefined,
        priority: priority || undefined,
        page,
        size: 20,
      });
      setRows(result.content);
      setTotalPages(result.totalPages);
    } catch (err) {
      setError(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [status, priority, page]);

  React.useEffect(() => {
    void load();
  }, [load]);

  async function openTicket(id: string) {
    try {
      setSelected(await adminApi.supportTicket(id));
    } catch (err) {
      toast.error(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    }
  }

  async function patch(
    id: string,
    payload: {
      status?: SupportTicketStatus;
      priority?: SupportTicketPriority;
      assignedAdminId?: string;
    }
  ) {
    try {
      const updated = await adminApi.updateSupportTicket(id, payload);
      setSelected(updated);
      await load();
      toast.success("Ticket updated");
    } catch (err) {
      toast.error(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    }
  }

  async function sendReply() {
    if (!selected || !reply.trim()) return;
    try {
      const updated = await adminApi.replySupportTicket(selected.id, reply.trim());
      setSelected(updated);
      setReply("");
      toast.success("Reply sent");
    } catch (err) {
      toast.error(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    }
  }

  return (
    <div className="space-y-6">
      <AdminPageHeader
        title="Support tickets"
        description="Assign, prioritize, reply, resolve, and close planner and vendor tickets."
      />

      <div className="flex flex-wrap gap-3">
        <select
          className="bg-background rounded-xl border px-3 py-2 text-sm"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value);
            setPage(0);
          }}
        >
          <option value="">All statuses</option>
          {Object.entries(TICKET_STATUS_LABEL).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
        <select
          className="bg-background rounded-xl border px-3 py-2 text-sm"
          value={priority}
          onChange={(e) => {
            setPriority(e.target.value);
            setPage(0);
          }}
        >
          <option value="">All priorities</option>
          {Object.entries(TICKET_PRIORITY_LABEL).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </div>

      {loading ? <SkeletonLoader className="h-48 rounded-2xl" /> : null}
      {error ? <EmptyState title="Could not load tickets" description={error} /> : null}

      <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
        <div className="space-y-3">
          {!loading && !error && rows.length === 0 ? (
            <EmptyState title="No tickets in this view" />
          ) : null}
          {rows.map((ticket) => (
            <button
              key={ticket.id}
              type="button"
              onClick={() => void openTicket(ticket.id)}
              className="bg-card hover:bg-muted/30 w-full rounded-2xl border p-4 text-left shadow-sm"
            >
              <div className="flex flex-wrap items-center gap-2">
                <AdminStatusBadge
                  label={TICKET_STATUS_LABEL[ticket.status] ?? ticket.status}
                  tone={STATUS_TONE[ticket.status]}
                />
                <AdminStatusBadge
                  label={TICKET_PRIORITY_LABEL[ticket.priority] ?? ticket.priority}
                  tone={PRIORITY_TONE[ticket.priority]}
                />
                <span className="text-muted-foreground text-xs">{ticket.categoryName}</span>
              </div>
              <p className="mt-2 font-medium">{ticket.subject}</p>
              <p className="text-muted-foreground text-sm">
                {ticket.createdByName} · {new Date(ticket.updatedAt).toLocaleString()}
              </p>
            </button>
          ))}
          <div className="flex justify-end gap-2">
            <Button
              variant="outline"
              className="rounded-xl"
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
            >
              Previous
            </Button>
            <Button
              variant="outline"
              className="rounded-xl"
              disabled={page + 1 >= totalPages}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </Button>
          </div>
        </div>

        <div className="bg-card rounded-2xl border p-5">
          {selected ? (
            <div className="space-y-4">
              <div>
                <h2 className="text-lg font-semibold">{selected.subject}</h2>
                <p className="text-muted-foreground text-sm">{selected.description}</p>
              </div>
              <div className="flex flex-wrap gap-2">
                <select
                  className="bg-background rounded-xl border px-3 py-2 text-sm"
                  value={selected.status}
                  onChange={(e) =>
                    void patch(selected.id, { status: e.target.value as SupportTicketStatus })
                  }
                >
                  {Object.keys(TICKET_STATUS_LABEL).map((value) => (
                    <option key={value} value={value}>
                      {TICKET_STATUS_LABEL[value]}
                    </option>
                  ))}
                </select>
                <select
                  className="bg-background rounded-xl border px-3 py-2 text-sm"
                  value={selected.priority}
                  onChange={(e) =>
                    void patch(selected.id, { priority: e.target.value as SupportTicketPriority })
                  }
                >
                  {Object.keys(TICKET_PRIORITY_LABEL).map((value) => (
                    <option key={value} value={value}>
                      {TICKET_PRIORITY_LABEL[value]}
                    </option>
                  ))}
                </select>
                <Button
                  size="sm"
                  className="rounded-lg"
                  onClick={() =>
                    void patch(selected.id, { assignedAdminId: user?.id, status: "IN_PROGRESS" })
                  }
                >
                  Assign to me
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  className="rounded-lg"
                  onClick={() => void patch(selected.id, { status: "RESOLVED" })}
                >
                  Resolve
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  className="rounded-lg"
                  onClick={() => void patch(selected.id, { status: "CLOSED" })}
                >
                  Close
                </Button>
              </div>
              <div className="space-y-3">
                {selected.messages.map((message) => (
                  <div key={message.id} className="bg-muted/40 rounded-xl p-3 text-sm">
                    <p className="text-muted-foreground text-xs">
                      {message.authorName} · {new Date(message.createdAt).toLocaleString()}
                    </p>
                    <p className="mt-1">{message.body}</p>
                  </div>
                ))}
              </div>
              <Textarea
                value={reply}
                onChange={(e) => setReply(e.target.value)}
                placeholder="Reply to the requester"
                className="rounded-xl"
              />
              <Button
                className="rounded-xl"
                onClick={() => void sendReply()}
                disabled={!reply.trim()}
              >
                Send reply
              </Button>
            </div>
          ) : (
            <p className="text-muted-foreground text-sm">
              Select a ticket to assign, reply, or resolve.
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
