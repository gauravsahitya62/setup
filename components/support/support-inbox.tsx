"use client";

import * as React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { EmptyState } from "@/components/shared/empty-state";
import { SkeletonLoader } from "@/components/shared/skeleton-loader";
import { TICKET_PRIORITY_LABEL, TICKET_STATUS_LABEL, adminErrorMessage } from "@/lib/admin/labels";
import {
  ApiClientError,
  supportApi,
  type SupportCategoryDto,
  type SupportTicketDto,
  type SupportTicketPriority,
  type SupportTicketSummaryDto,
} from "@/services";

export function SupportInbox({ title, ticketBasePath }: { title: string; ticketBasePath: string }) {
  const [categories, setCategories] = React.useState<SupportCategoryDto[]>([]);
  const [tickets, setTickets] = React.useState<SupportTicketSummaryDto[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [subject, setSubject] = React.useState("");
  const [description, setDescription] = React.useState("");
  const [categoryId, setCategoryId] = React.useState("");
  const [priority, setPriority] = React.useState<SupportTicketPriority>("MEDIUM");

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [cats, page] = await Promise.all([supportApi.categories(), supportApi.tickets(0)]);
      setCategories(cats);
      setTickets(page.content);
      setCategoryId((current) => current || cats[0]?.id || "");
    } catch (err) {
      setError(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  React.useEffect(() => {
    void load();
  }, [load]);

  async function create(event: React.FormEvent) {
    event.preventDefault();
    try {
      await supportApi.create({ subject, description, categoryId, priority });
      toast.success("Ticket created");
      setSubject("");
      setDescription("");
      await load();
    } catch (err) {
      toast.error(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
        <p className="text-muted-foreground mt-1 text-sm">
          Create a ticket and follow replies from FlowSet support.
        </p>
      </div>

      <form
        onSubmit={(event) => void create(event)}
        className="grid max-w-xl gap-3 rounded-2xl border p-5"
      >
        <div className="space-y-1">
          <Label htmlFor="ticket-subject">Subject</Label>
          <Input
            id="ticket-subject"
            value={subject}
            onChange={(e) => setSubject(e.target.value)}
            className="rounded-xl"
            required
          />
        </div>
        <div className="space-y-1">
          <Label htmlFor="ticket-category">Category</Label>
          <select
            id="ticket-category"
            className="bg-background w-full rounded-xl border px-3 py-2 text-sm"
            value={categoryId}
            onChange={(e) => setCategoryId(e.target.value)}
          >
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </div>
        <div className="space-y-1">
          <Label htmlFor="ticket-priority">Priority</Label>
          <select
            id="ticket-priority"
            className="bg-background w-full rounded-xl border px-3 py-2 text-sm"
            value={priority}
            onChange={(e) => setPriority(e.target.value as SupportTicketPriority)}
          >
            {Object.entries(TICKET_PRIORITY_LABEL).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </div>
        <div className="space-y-1">
          <Label htmlFor="ticket-description">Description</Label>
          <Textarea
            id="ticket-description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="rounded-xl"
            required
          />
        </div>
        <Button className="w-fit rounded-xl" type="submit">
          Create ticket
        </Button>
      </form>

      {loading ? <SkeletonLoader className="h-32 rounded-2xl" /> : null}
      {error ? <EmptyState title="Could not load tickets" description={error} /> : null}
      {!loading && !error && tickets.length === 0 ? <EmptyState title="No tickets yet" /> : null}

      <div className="space-y-3">
        {tickets.map((ticket) => (
          <Link
            key={ticket.id}
            href={`${ticketBasePath}/${ticket.id}`}
            className="hover:bg-muted/30 block rounded-2xl border p-4"
          >
            <p className="font-medium">{ticket.subject}</p>
            <p className="text-muted-foreground text-sm">
              {TICKET_STATUS_LABEL[ticket.status]} · {TICKET_PRIORITY_LABEL[ticket.priority]} ·{" "}
              {new Date(ticket.updatedAt).toLocaleString()}
            </p>
          </Link>
        ))}
      </div>
    </div>
  );
}

export function SupportTicketDetail({
  ticketId,
  backHref,
}: {
  ticketId: string;
  backHref: string;
}) {
  const router = useRouter();
  const [ticket, setTicket] = React.useState<SupportTicketDto | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [body, setBody] = React.useState("");

  const load = React.useCallback(async () => {
    setLoading(true);
    try {
      setTicket(await supportApi.ticket(ticketId));
    } catch (err) {
      setError(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [ticketId]);

  React.useEffect(() => {
    void load();
  }, [load]);

  async function send() {
    if (!ticket || !body.trim()) return;
    try {
      setTicket(await supportApi.message(ticket.id, body.trim()));
      setBody("");
    } catch (err) {
      toast.error(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    }
  }

  async function close() {
    if (!ticket) return;
    try {
      setTicket(await supportApi.close(ticket.id));
      toast.success("Ticket closed");
    } catch (err) {
      toast.error(err instanceof ApiClientError ? err.message : adminErrorMessage(err));
    }
  }

  if (loading) return <SkeletonLoader className="h-48 rounded-2xl" />;
  if (error || !ticket)
    return (
      <EmptyState
        title="Ticket not found"
        description={error ?? undefined}
        actionLabel="Back"
        onAction={() => router.push(backHref)}
      />
    );

  return (
    <div className="space-y-4">
      <Button variant="outline" className="rounded-xl" onClick={() => router.push(backHref)}>
        Back
      </Button>
      <h1 className="text-2xl font-semibold">{ticket.subject}</h1>
      <p className="text-muted-foreground text-sm">{ticket.description}</p>
      <p className="text-sm">
        {TICKET_STATUS_LABEL[ticket.status]} · {TICKET_PRIORITY_LABEL[ticket.priority]}
      </p>
      <div className="space-y-3">
        {ticket.messages.map((message) => (
          <div key={message.id} className="rounded-xl border p-3 text-sm">
            <p className="text-muted-foreground text-xs">
              {message.authorName} · {new Date(message.createdAt).toLocaleString()}
            </p>
            <p className="mt-1">{message.body}</p>
          </div>
        ))}
      </div>
      {ticket.status !== "CLOSED" && ticket.status !== "RESOLVED" ? (
        <>
          <Textarea
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder="Add a message"
            className="rounded-xl"
          />
          <div className="flex gap-2">
            <Button className="rounded-xl" onClick={() => void send()}>
              Send
            </Button>
            <Button variant="outline" className="rounded-xl" onClick={() => void close()}>
              Close ticket
            </Button>
          </div>
        </>
      ) : null}
    </div>
  );
}
